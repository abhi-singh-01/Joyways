# JOYWAY — Disaster Recovery

Scope: recovery procedures for production incidents. Read alongside
[`PRODUCTION_RUNBOOK.md`](./PRODUCTION_RUNBOOK.md) (day-to-day operations) and
[`MONITORING.md`](./MONITORING.md) (detection).

**No real secrets appear in this document.** Every credential is referenced by variable name.

---

## Recovery objectives

| Objective | Target | Basis |
|---|---|---|
| RPO (max data loss) | 5 minutes | Azure PostgreSQL point-in-time restore granularity |
| RTO (max downtime) — app only | 15 minutes | Container rollback to previous image |
| RTO — database restore | 60–90 minutes | PITR provisions a new server |

> These are targets, not measured values. They stay unverified until a restore test is
> actually performed (see §10).

## Roles

| Role | Responsibility |
|---|---|
| Incident lead | Declares the incident, owns comms, decides on rollback |
| Operator | Executes Azure/container commands |
| Reviewer | Verifies data integrity after recovery |

Record the named owners here before go-live.

---

## Common variables

```bash
RG=joyway-prod-rg
APP=joyway-backend
FRONTEND=joyway-frontend
PG=joyway-pg
ACR=joywayacr
```

---

## 1. Database failure

**Symptoms.** `GET /api/health/ready` returns `503` with `"database": "DOWN"`. Bookings and
payments fail. Logs show `event=health.database.unreachable`.

**Detection.** Readiness probe failure; "Database unavailable" alert; PostgreSQL availability
metric.

**Immediate action.**

1. Confirm scope — is it the server, or only this app instance?
   ```bash
   az postgres flexible-server show -g $RG -n $PG --query "{state:state,version:version}"
   curl -s -o /dev/null -w '%{http_code}' https://api.joyway.in/api/health/ready
   ```
2. Check for a firewall/network rule change and for maintenance events.
3. Do **not** restart backend containers first. Liveness is independent of the database, so
   restarting only discards warm state and in-flight requests.

**Recovery.**

- *Server stopped* — `az postgres flexible-server start -g $RG -n $PG`
- *Connection exhaustion* — identify and terminate idle sessions:
  ```sql
  SELECT pid, state, age(clock_timestamp(), query_start) AS age
  FROM pg_stat_activity WHERE state <> 'idle' ORDER BY age DESC;
  SELECT pg_terminate_backend(<pid>);
  ```
  Then reduce the app's pool size or scale in replicas.
- *Storage full* — grow storage (this is one-way; it cannot be shrunk):
  ```bash
  az postgres flexible-server update -g $RG -n $PG --storage-size 128
  ```
- *Corruption or unrecoverable failure* — restore (§7).

**Verification.** `/api/health/ready` returns `200`; a booking lookup succeeds; the
reconciliation counters on the admin dashboard are zero.

**Rollback.** None — restoring forward is the only path. Keep the failed server stopped
rather than deleted until the replacement is confirmed good.

---

## 2. Backend failure

**Symptoms.** 5xx responses, or connection refused on `/api/health`.

**Detection.** Liveness probe failure; "Backend unavailable" or sustained-5xx alert.

**Immediate action.**

```bash
az containerapp revision list -g $RG -n $APP -o table
az containerapp logs show -g $RG -n $APP --tail 200
```

Look for `event=request.unhandled_error`, then grep the `requestId` from the customer report.

**Recovery.**

- *Startup failure* — usually configuration. `ProductionSecurityValidator` intentionally
  refuses to start on insecure config, and the message names the offending setting
  (weak `JWT_SECRET`, wildcard CORS, `ADMIN_BOOTSTRAP_ENABLED=true`, missing
  `RAZORPAY_WEBHOOK_SECRET`, localhost database URL). Fix the setting and redeploy.
- *Bad deployment* — roll back to the previous revision (§6).
- *Resource exhaustion* — scale out, then investigate:
  ```bash
  az containerapp update -g $RG -n $APP --min-replicas 2 --max-replicas 5
  ```

**Verification.** `/api/health` and `/api/health/ready` both `200`; error rate back to
baseline.

**Rollback.** Pin traffic to the last known-good revision (§6).

---

## 3. Frontend failure

**Symptoms.** Site does not load, or loads but every API call fails.

**Detection.** Availability test; user reports.

**Immediate action.** Distinguish the two cases — if `api.joyway.in/api/health` is healthy,
the fault is in the frontend or its configuration.

**Recovery.**

- *Wrong API base URL baked into the bundle* — `NEXT_PUBLIC_*` values are compiled in at
  build time, so this cannot be fixed with an environment variable at runtime. Rebuild with
  the correct `--build-arg` and redeploy.
- *Container not starting* — check logs; confirm the standalone output is intact.
- *Bad deployment* — roll back to the previous image tag.

**Verification.** Home page renders, trips list loads, booking form submits.

> Guard already in place: the production build fails if `NEXT_PUBLIC_API_BASE_URL` is unset,
> and `frontend/.dockerignore` prevents a local `.env.local` from overriding the build args.

---

## 4. Container failure / crash loop

**Symptoms.** Restart count climbing; healthchecks never pass.

**Detection.** "Repeated container crashes" alert.

**Immediate action.**

```bash
az containerapp logs show -g $RG -n $APP --tail 300
```

Read the **first** error after startup, not the last — crash loops bury the root cause.

**Recovery.**

1. If it is a config failure, fix the environment variable and redeploy.
2. If it is a bad image, roll back (§6).
3. If it is resource starvation, raise the memory limit. Note that the JVM in a 512M
   container will be OOM-killed under load; the production compose file sets a 1G limit.

**Verification.** Restart count stable for 30 minutes; readiness passing.

---

## 5. Azure regional issue

**Symptoms.** Multiple unrelated Azure services failing at once.

**Detection.** Azure Service Health alert.

**Immediate action.** Confirm on Azure Service Health, then communicate status to customers.
Do not attempt infrastructure changes during a regional event — they usually fail and add
confusion.

**Recovery.** Wait for Azure, unless the business accepts the cost of a standby region.

> **Current limitation.** JOYWAY is single-region with no cross-region replica. A regional
> outage is a full outage. If the business requires regional resilience, add a geo-redundant
> PostgreSQL replica and a second container environment — that is a cost and architecture
> decision, not a configuration change.

**Verification.** Full end-to-end acceptance flow (§ runbook) once the region recovers.

---

## 6. Accidental bad deployment

**Symptoms.** Errors or wrong behaviour immediately after a release.

**Detection.** Error-rate alert following a deployment; smoke test failure.

**Immediate action — roll back first, diagnose afterwards.**

```bash
# List revisions, newest first
az containerapp revision list -g $RG -n $APP -o table

# Send 100% of traffic to the previous good revision
az containerapp ingress traffic set -g $RG -n $APP \
  --revision-weight <previous-revision>=100
```

Docker Compose equivalent:

```bash
APP_VERSION=<previous-tag> docker compose -f docker-compose.prod.yml up -d
```

**Verification.** Smoke test: health, trips list, booking lookup, admin login.

**Rollback caveat.** Application rollback is safe. A **database migration** may not be —
Flyway migrations are forward-only. If the bad release added a migration, do not assume
rolling back the image undoes it. Check `flyway_schema_history` and treat schema reversal as
a separate, reviewed change.

---

## 7. Data corruption / accidental data loss

**Symptoms.** Missing or wrong bookings, payments or packages.

**Detection.** Reconciliation mismatch counters; customer reports; audit log review.

**Immediate action.**

1. **Do not repair production by hand under time pressure.** Capture evidence first.
2. Establish the last known-good timestamp from `audit_logs` and application logs.
3. Restore to a **new** server — never over the top of production:

```bash
az postgres flexible-server restore \
  --resource-group $RG \
  --name joyway-pg-restored \
  --source-server $PG \
  --restore-time "2026-08-28T01:30:00Z"
```

**Recovery.** Compare the restored copy against production, extract only the affected rows,
and apply a reviewed correction. Repoint the application only if the damage is widespread.

**Verification.** Affected bookings reconcile; `paidButNotConfirmed` and `confirmedButNotPaid`
are zero; the audit trail explains the correction.

---

## 8. Payment service outage (Razorpay)

**Symptoms.** Order creation fails; webhooks stop arriving.

**Detection.** `event=payment.webhook.failed`; `PAYMENT_GATEWAY_ERROR` responses; Razorpay
status page.

**Immediate action.** Confirm at the Razorpay status page. Bookings can still be created —
they simply remain `PENDING_PAYMENT`, and seats stay held.

**Recovery.**

- Razorpay replays undelivered webhooks. Duplicate delivery is safe: the `event_id` unique
  constraint plus the `razorpay_payment_id` unique index make processing idempotent.
- After recovery, reconcile:
  ```sql
  SELECT booking_reference, status, payment_status
  FROM bookings
  WHERE (payment_status = 'PAID'   AND status NOT IN ('CONFIRMED','COMPLETED'))
     OR (status = 'CONFIRMED'      AND payment_status <> 'PAID');
  ```
- Cross-check against the Razorpay dashboard before changing any booking.

**Verification.** Dashboard mismatch counters return to zero; a test payment succeeds
end to end.

> **Never** mark a booking paid manually without confirming capture in the Razorpay
> dashboard first.

---

## 9. SMTP outage

**Symptoms.** Customers do not receive confirmations. `email_logs` fills with `FAILED`.

**Detection.** Email-failure alert; the query in `MONITORING.md` §5.

**Immediate action.** Verify SMTP host reachability and credential validity. Email failure
never blocks a booking or payment — all notification calls are wrapped so a mail failure
cannot roll back a business transaction.

**Recovery.** `EmailRetryScheduler` retries automatically up to
`joyway.mail.max-retry-attempts`. For messages that exhausted their attempts after a long
outage, reset them deliberately and in bounded batches:

```sql
UPDATE email_logs
SET status = 'PENDING', attempt_count = 0
WHERE status = 'FAILED'
  AND created_at > NOW() - INTERVAL '24 hours';
```

**Verification.** `email_logs` transitions to `SENT`; a test booking delivers mail from the
correct alias.

**Rollback.** None required. Retries are capped, so there is no runaway send loop.

---

## 10. Backup and restore

### Configuration — **Requires Azure**

```bash
az postgres flexible-server show -g $RG -n $PG \
  --query "{retention:backup.backupRetentionDays, geoRedundant:backup.geoRedundantBackup}"
```

Recommended production settings:

| Setting | Recommended |
|---|---|
| Backup retention | 14–35 days |
| Geo-redundant backup | Enabled |
| Point-in-time restore | Available within retention |

```bash
az postgres flexible-server update -g $RG -n $PG --backup-retention 30
```

### Restore test — **NOT YET PERFORMED**

> A backup is not a backup until a restore has been proven. "Backups enabled" in the portal
> is a configuration claim, not evidence of recoverability. This test is an outstanding
> production blocker.

Procedure (safe — creates a new server, never touches production):

```bash
# 1. Restore to a new server at a chosen point in time
az postgres flexible-server restore \
  --resource-group $RG \
  --name joyway-pg-restoretest \
  --source-server $PG \
  --restore-time "$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)"

# 2. Confirm schema and migration history arrived intact
psql "$RESTORED_URL" -c "\dt"
psql "$RESTORED_URL" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 3. Confirm row counts are plausible
psql "$RESTORED_URL" -c "SELECT COUNT(*) FROM bookings;"
psql "$RESTORED_URL" -c "SELECT COUNT(*) FROM payments;"

# 4. Point a NON-production backend at the restored server and check readiness
curl -s https://staging-api.joyway.in/api/health/ready

# 5. Delete the test server
az postgres flexible-server delete -g $RG -n joyway-pg-restoretest --yes
```

Record for each test: date, operator, restore point, elapsed time, and outcome.

| Date | Operator | Restore point | Duration | Result |
|---|---|---|---|---|
| _pending_ | | | | **Not yet run** |

---

## Post-incident

1. Write a short timeline: detection, actions, resolution.
2. Identify the root cause, not just the trigger.
3. Add or tune the alert that should have caught it sooner.
4. Add a regression test where the failure was a code defect.
5. Update this document if a procedure proved wrong or incomplete.
