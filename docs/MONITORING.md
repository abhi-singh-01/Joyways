# JOYWAY — Monitoring

Status legend used throughout this document:

- **Implemented** — present in this repository and verified locally.
- **Requires Azure** — must be configured in the Azure portal or via `az`. Not verified,
  because the Azure CLI is not installed in the current environment and this workspace
  contains no Azure infrastructure definitions.

---

## 1. Application health endpoints

**Implemented.** Two probes are exposed. Both are public so infrastructure can reach them,
and both return only coarse status values — never connection strings, credentials, driver
messages, environment variables or stack traces.

| Endpoint | Purpose | Touches DB | Healthy | Unhealthy |
|---|---|---|---|---|
| `GET /api/health` | Liveness — is the process alive | No | `200` | connection refused |
| `GET /api/health/ready` | Readiness — can it serve traffic | Yes | `200` | `503` |

Liveness deliberately does **not** touch the database. If it did, a database blip would
make the orchestrator kill and restart otherwise-healthy containers, turning a recoverable
database incident into a full outage.

Liveness response:

```json
{ "success": true, "data": { "status": "UP", "service": "joyway-backend", "version": "0.0.1-SNAPSHOT" } }
```

Readiness response when the database is unreachable (HTTP 503):

```json
{ "success": false, "data": { "status": "DOWN", "service": "joyway-backend",
  "version": "0.0.1-SNAPSHOT", "database": "DOWN" }, "errorCode": "NOT_READY" }
```

The database failure reason is logged server-side as the exception *type* only
(`event=health.database.unreachable reason=SQLException`), because driver messages embed
the host and username.

Source: `backend/src/main/java/in/joyway/controller/HealthController.java`
Tests: `backend/src/test/java/in/joyway/controller/HealthControllerTest.java` — includes
assertions that the response body does not leak credentials.

### Container probes

**Implemented.** Both images declare `HEALTHCHECK`, and `docker-compose.prod.yml` declares
healthchecks with `start_period`, so a slow first boot is not reported as a failure.
The backend healthcheck targets `/api/health/ready`, so an instance that cannot reach the
database is correctly reported unhealthy rather than silently serving errors.

---

## 2. Request correlation

**Implemented.** Every request carries a correlation id.

- Header: `X-Request-ID`
- If the client supplies one it is reused, but **only** when it matches `^[A-Za-z0-9._-]{1,64}$`.
  Anything else is replaced with a generated UUID. Echoing arbitrary client input into log
  lines would let an attacker inject forged log entries (newlines, fake severities).
- Always echoed back on the response.
- Placed in the SLF4J MDC under `requestId` and rendered in every log line.
- Returned in the JSON body as `requestId` for `500` and `409` responses, so a customer can
  quote it to support.

Log pattern (`application.yml`):

```
%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%X{requestId:-no-request-id}] %logger{36} - %msg%n
```

Source: `backend/src/main/java/in/joyway/observability/RequestCorrelationFilter.java`
Tests: `RequestCorrelationFilterTest` — covers generation, pass-through, log-injection
rejection, length capping and MDC cleanup.

---

## 3. Structured log events

**Implemented.** Operationally significant paths emit `event=` keys that are greppable and
safe to index. No secrets, no full JWTs, no passwords, no card or payment credentials.

| Event | Level | Meaning |
|---|---|---|
| `booking.created` | INFO | Booking persisted, seats deducted |
| `booking.cancelled` | INFO | Booking cancelled, seats returned |
| `payment.order.created` | INFO | Razorpay order created |
| `payment.verified` | INFO | Signature verified, booking confirmed |
| `payment.webhook.received` | INFO | Webhook accepted for processing |
| `payment.webhook.confirmed` | INFO | Booking confirmed via webhook |
| `payment.webhook.duplicate` | INFO | Replayed event id ignored |
| `payment.webhook.failed` | WARN/ERROR | `reason=signature_invalid` or `reason=processing_error` |
| `payment.reconciliation.mismatch` | WARN | Booking and payment state disagree |
| `auth.login.failed` | WARN | Invalid admin credentials |
| `security.rate_limit_exceeded` | WARN | Client throttled |
| `health.database.unreachable` | ERROR | Readiness probe failed |

Customer email addresses are masked by `EmailService` before logging (e.g. `c***@example.com`).

Suggested Azure Log Analytics query:

```kusto
ContainerAppConsoleLogs_CL
| where Log_s contains "event=payment.webhook.failed"
| summarize count() by bin(TimeGenerated, 5m)
```

---

## 4. Payment monitoring and reconciliation

**Implemented.** The dangerous states are where booking status and payment status disagree.

| Situation | Meaning | Detection |
|---|---|---|
| Payment `PAID`, booking not `CONFIRMED` | Money captured, customer has no confirmed seat | `paidButNotConfirmed` |
| Booking `CONFIRMED`, payment not `PAID` | Seats held for money never captured | `confirmedButNotPaid` |

Both are surfaced in two places:

1. **Admin dashboard** — `GET /api/admin/dashboard` returns `paidButNotConfirmed` and
   `confirmedButNotPaid` counts. The UI shows both counters always, plus a warning banner
   when either is greater than zero.
2. **Scheduled check** — `PaymentReconciliationScheduler` runs every 15 minutes
   (`joyway.monitoring.reconciliation-cron`) and logs each offending booking reference at
   WARN.

The scheduler only observes. It never mutates data, because resolving a mismatch requires a
human decision (refund, manual confirmation, or reconciliation against the Razorpay
dashboard).

Related safeguard added during QA: a captured payment for a booking that is no longer active
is recorded for reconciliation but does **not** revive the booking, and raises a
"Refund review required" admin notification.

---

## 5. Email monitoring

**Implemented (pre-existing, verified).**

- Every send is persisted to `email_logs` with template, recipient, status,
  attempt count and failure reason. SMTP credentials are never stored.
  Category is derived from the template (`EmailTemplateCategoryResolver`), not stored as a column.
- `EmailRetryScheduler` retries `FAILED`/`PENDING` entries on a fixed scheduler tick
  (`joyway.mail.retry-delay-ms`, default 5 minutes).
- Retries are capped by `joyway.mail.max-retry-attempts` (default `3`), so there is no
  infinite retry loop.
- Each failed attempt waits exponentially longer before the next try
  (5 min, then 10 min, then 20 min) using `last_attempted_at`.
- Retries re-send an existing logged email rather than re-running business logic, so a retry
  cannot create a duplicate booking or duplicate payment.

There is **no `category` column** on `email_logs`. Group by `template` instead:

```sql
SELECT template, status, attempt_count, failure_reason, created_at
FROM email_logs
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 50;
```

Alias routing is resolved by `EmailSenderResolver` and covered by `EmailSenderResolverTest`:

| Category | From address |
|---|---|
| `BOOKING` | bookings@joyway.in |
| `SUPPORT` | support@joyway.in |
| `AUTOMATED` | noreply@joyway.in |
| `GENERAL` / `CUSTOMER` | hello@joyway.in |

---

## 6. Security monitoring

**Implemented.**

Rate limiting (`RateLimitFilter`, fixed one-minute window, per client IP):

| Endpoint | Default limit | Property |
|---|---|---|
| `POST /api/admin/auth/login` | 10/min | `RATE_LIMIT_LOGIN_PER_MINUTE` |
| `POST /api/bookings` | 20/min | `RATE_LIMIT_BOOKING_PER_MINUTE` |
| `POST /api/payments/orders`, `/verify` (and aliases) | 30/min | `RATE_LIMIT_PAYMENT_PER_MINUTE` |

Rejections return `429` with `errorCode: RATE_LIMITED` and log
`event=security.rate_limit_exceeded` with a **masked** client address.

The client key uses the left-most `X-Forwarded-For` entry when present, because Azure
terminates TLS upstream and the socket address would otherwise be the load balancer for
every caller.

> **The Razorpay webhook is deliberately not rate limited.** Dropping a webhook would leave a
> captured payment without a confirmed booking — exactly the failure this phase is meant to
> prevent. It is already protected by HMAC signature verification plus event-id idempotency
> backed by a unique database constraint.

Limits are per instance, so with N replicas the effective limit is N x the configured value.
That is acceptable: the goal is blunting credential stuffing and scripted abuse, not precise
quota enforcement.

Other signals: `auth.login.failed`, `security.rate_limit_exceeded`, and
`payment.webhook.failed reason=signature_invalid`.

### Audit trail

**Implemented (pre-existing).** `AuditLogService` records actor, action, entity type, entity
id, summary and timestamp for administrative operations. No passwords or secrets are stored.

---

## 7. Azure monitoring

**Requires Azure.** Not configured or verified here. The commands below are the intended
configuration; run them against your subscription.

Set your identifiers first:

```bash
RG=joyway-prod-rg
APP=joyway-backend
PG=joyway-pg
WORKSPACE=joyway-logs
```

### Container / app metrics

Metrics to watch: CPU, memory working set, replica restart count, request count,
response time, HTTP 4xx rate, HTTP 5xx rate.

```bash
# Route container logs and metrics into Log Analytics
az monitor diagnostic-settings create \
  --name joyway-diagnostics \
  --resource $(az containerapp show -g $RG -n $APP --query id -o tsv) \
  --workspace $WORKSPACE \
  --logs    '[{"category":"ContainerAppConsoleLogs","enabled":true}]' \
  --metrics '[{"category":"AllMetrics","enabled":true}]'
```

### Database metrics

CPU percent, memory percent, storage percent, active connections, failed connections,
and availability.

```bash
az monitor diagnostic-settings create \
  --name joyway-pg-diagnostics \
  --resource $(az postgres flexible-server show -g $RG -n $PG --query id -o tsv) \
  --workspace $WORKSPACE \
  --metrics '[{"category":"AllMetrics","enabled":true}]'
```

Enable `pg_stat_statements` to investigate slow queries:

```bash
az postgres flexible-server parameter set -g $RG -s $PG \
  --name pg_stat_statements.track --value all
```

---

## 8. Alerts

**Requires Azure.** Keep the set small — noisy alerts get ignored.

### Critical (page immediately)

| Alert | Condition |
|---|---|
| Backend unavailable | Availability < 100% over 5 min, or readiness failing |
| Database unavailable | PostgreSQL availability < 100% over 5 min |
| Repeated container crashes | Restart count > 3 in 15 min |
| Sustained HTTP 5xx | 5xx rate > 5% over 10 min |
| Payment webhook failures | `payment.webhook.failed` > 5 in 15 min |

### High

| Alert | Condition |
|---|---|
| CPU | > 80% for 15 min |
| Memory | > 85% for 15 min |
| Database storage | > 80% used |
| Connection exhaustion | Active connections > 80% of max |
| Repeated auth failures | `auth.login.failed` > 20 in 5 min from one address |

### Medium

| Alert | Condition |
|---|---|
| Email failures | `email_logs.status = FAILED` > 10 in 1 hour |
| API latency | p95 > 2s over 15 min |
| Payment/booking mismatch | `payment.reconciliation.mismatch` present |

Example — 5xx alert:

```bash
az monitor metrics alert create \
  --name joyway-5xx-sustained \
  --resource-group $RG \
  --scopes $(az containerapp show -g $RG -n $APP --query id -o tsv) \
  --condition "avg Requests where StatusCodeCategory == '5xx' > 5" \
  --window-size 10m --evaluation-frequency 5m --severity 1 \
  --action $(az monitor action-group show -g $RG -n joyway-oncall --query id -o tsv)
```

Example — log-based alert for webhook failures:

```kusto
ContainerAppConsoleLogs_CL
| where Log_s contains "event=payment.webhook.failed"
| summarize failures = count() by bin(TimeGenerated, 15m)
| where failures > 5
```

---

## 9. Frontend monitoring

**Partly implemented.**

- The frontend build fails fast when `NEXT_PUBLIC_API_BASE_URL` is unset in production
  (`frontend/lib/api/config.ts`), preventing a build that silently points at `localhost`.
- Frontend liveness: `GET /api/health` returns `{ "status": "UP" }` and is what the
  container `HEALTHCHECK` and Azure availability tests should hit.
- API client sends `X-Request-ID` and surfaces it on 500 errors so support can locate logs.
- API failures surface through `ErrorState` components; the backend `X-Request-ID` is
  exposed via CORS.
- **Not implemented:** browser JavaScript error capture. This needs a client-side agent
  (Azure Application Insights JS SDK or equivalent). Deferred deliberately — adding a
  third-party telemetry agent collects visitor data and is a privacy decision for the
  business, not a technical default.

Availability can be covered without any client agent using an Azure availability test
against `https://joyway.in/` and `https://api.joyway.in/api/health`.

---

## 10. Log retention

**Requires Azure** for enforcement; the local policy is implemented.

- **Container stdout/stderr** — capped locally by the `json-file` driver at 10MB x 5 files
  per service (`docker-compose.prod.yml`), so a log flood cannot fill the disk.
- **Azure Log Analytics** — set retention explicitly:

```bash
az monitor log-analytics workspace update -g $RG -n $WORKSPACE --retention-time 90
```

Recommended: 90 days for application logs, 365 days for audit logs (`audit_logs` lives in
PostgreSQL and follows the database backup retention).

Access: restrict Log Analytics reader access to the operations group via Azure RBAC.
Logs contain masked email addresses and booking references but no credentials, no full JWTs
and no payment secrets.

---

## 11. Cost monitoring

**Requires Azure.**

```bash
az consumption budget create \
  --budget-name joyway-monthly \
  --amount 200 \
  --category Cost \
  --time-grain Monthly \
  --resource-group $RG
```

Add threshold notifications at 50%, 80% and 100% of the monthly budget. Track PostgreSQL,
container apps, storage, networking and Log Analytics ingestion separately — Log Analytics
ingestion is the line item most likely to grow unexpectedly.

Record the agreed monthly figure and the notification recipients here once set.

---

## 12. Azure PostgreSQL capacity (production recommendation)

**Requires Azure to confirm current SKU.** Do not scale up without evidence.

| Setting | Staging / early production | Scale when |
|---|---|---|
| Compute | Burstable `B2s` (2 vCPU) is acceptable for low traffic | Sustained CPU > 70% for 30 min, or connection wait |
| Storage | 32 GB, auto-grow enabled | Used storage > 70% |
| Connections | Keep app pool well below `max_connections` | `pg_stat_activity` shows waiting backends |
| High availability | Off until paid traffic justifies it | RTO target cannot tolerate a zone failure |
| Backups | 14–35 day retention, geo-redundant if the region supports it | Restore test overdue (see `DISASTER_RECOVERY.md` §10) |

JOYWAY's booking volume is bursty (campaign days) rather than constantly high. Start small,
watch CPU / connections / storage, and raise SKU only after a measured need. High availability
zone-redundant PostgreSQL roughly doubles compute cost — it is a business decision, not a
default.

---

## 13. Container reliability

**Implemented in images and compose.**

- Backend image installs `wget` and health-checks `/api/health/ready`.
- Frontend image installs `wget` and health-checks `/api/health`.
- Production compose: `restart: unless-stopped`, `start_period`, `stop_grace_period`,
  CPU/memory limits, log rotation (`10m` × 5 files).
- Spring Boot `server.shutdown=graceful` with a 30s drain.
- A permanently unhealthy container (database down) fails readiness and is not reported healthy.

---

## 14. Outstanding Azure actions

These cannot be completed from this repository:

1. Diagnostic settings + alerts (sections 7–8).
2. Log Analytics retention.
3. Cost budget.
4. **PostgreSQL restore test** (`DISASTER_RECOVERY.md` §10) — still a production blocker.
5. Staging failure simulations (backend/frontend restart, invalid webhook, SMTP failure)
   against the live staging environment.

