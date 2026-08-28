# JOYWAY — Production Runbook

Day-to-day operational procedures. For incident recovery see
[`DISASTER_RECOVERY.md`](./DISASTER_RECOVERY.md); for what is monitored and how, see
[`MONITORING.md`](./MONITORING.md).

**This document contains no real secrets.** Credentials are referenced by variable name only.

---

## Common variables

```bash
RG=joyway-prod-rg
APP=joyway-backend
FRONTEND=joyway-frontend
PG=joyway-pg
API=https://api.joyway.in
SITE=https://joyway.in
```

---

## STARTUP

### Deploy

The frontend bakes `NEXT_PUBLIC_*` values in **at build time**. They cannot be changed later
with a runtime environment variable — a wrong value requires a rebuild.

```bash
# Backend
docker build -t $ACR.azurecr.io/joyway-backend:$TAG ./backend

# Frontend — build args must be correct at build time
docker build -t $ACR.azurecr.io/joyway-frontend:$TAG \
  --build-arg NEXT_PUBLIC_API_BASE_URL=$API \
  --build-arg NEXT_PUBLIC_RAZORPAY_KEY_ID=$RAZORPAY_KEY_ID \
  ./frontend

docker push $ACR.azurecr.io/joyway-backend:$TAG
docker push $ACR.azurecr.io/joyway-frontend:$TAG
```

Compose deployment:

```bash
docker compose -f docker-compose.prod.yml config    # validate before applying
docker compose -f docker-compose.prod.yml up -d
```

Secrets are injected from the environment. Never bake them into an image, and never commit
a real `.env`.

The backend refuses to start in the `prod` profile when security configuration is unsafe
(`ProductionSecurityValidator`). A startup failure naming `JWT_SECRET`, CORS,
`ADMIN_BOOTSTRAP_ENABLED`, `RAZORPAY_WEBHOOK_SECRET` or the database URL is that guard doing
its job — fix the setting rather than bypassing the check.

### Verify health

```bash
curl -s $API/api/health          | jq        # expect status UP
curl -s $API/api/health/ready    | jq        # expect status UP, database UP
curl -s -o /dev/null -w '%{http_code}\n' $SITE
```

Capture the correlation id for any failure:

```bash
curl -si $API/api/health/ready | grep -i x-request-id
```

### Verify database

```bash
az postgres flexible-server show -g $RG -n $PG --query state
```

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

All rows must show `success = true`. A failed migration leaves the schema partially applied
and must be resolved before serving traffic.

### Verify frontend

Load the site and confirm: home page renders, trips list populates, a trip detail page
opens, the booking form loads, and `/admin/login` is reachable while `/admin` redirects when
signed out.

---

## MONITORING

| What | Where |
|---|---|
| Application logs | `az containerapp logs show -g $RG -n $APP --tail 200` |
| Aggregated logs | Azure Log Analytics (`ContainerAppConsoleLogs_CL`) |
| Metrics | Azure Monitor → Container App / PostgreSQL |
| Alerts | Azure Monitor → Alerts |
| Audit trail | `audit_logs` table |
| Email delivery | `email_logs` table |
| Payment mismatches | Admin dashboard, plus `event=payment.reconciliation.*` |

Trace one customer complaint end to end using the id from the error response:

```kusto
ContainerAppConsoleLogs_CL
| where Log_s contains "<request-id>"
| order by TimeGenerated asc
```

---

## PAYMENTS

### Verify a payment

```sql
SELECT b.booking_reference, b.status, b.payment_status,
       p.razorpay_order_id, p.razorpay_payment_id,
       p.status AS payment_record_status, p.signature_verified
FROM bookings b
LEFT JOIN payments p ON p.booking_id = b.id
WHERE b.booking_reference = 'JW-2026-00001';
```

Healthy confirmed booking: `status=CONFIRMED`, `payment_status=PAID`,
`payment_record_status=CAPTURED`, `signature_verified=true`.

### Investigate a webhook failure

```bash
az containerapp logs show -g $RG -n $APP --tail 500 | grep 'event=payment.webhook'
```

| Log | Meaning | Action |
|---|---|---|
| `reason=signature_invalid` | Secret mismatch, or a forged call | Confirm `RAZORPAY_WEBHOOK_SECRET` matches the Razorpay dashboard |
| `reason=processing_error` | Handler threw | Read the surrounding stack trace by `requestId` |
| `payment.webhook.duplicate` | Replay | Normal and safe — no action |

Check which events were recorded:

```sql
SELECT event_id, event_type, processed, created_at
FROM payment_webhook_events
ORDER BY created_at DESC LIMIT 20;
```

If the secret is unset, webhook verification **fails closed** (all webhooks rejected) rather
than accepting unverified calls.

### Investigate a payment/booking mismatch

```sql
SELECT booking_reference, status, payment_status, total_amount, created_at
FROM bookings
WHERE (payment_status = 'PAID' AND status NOT IN ('CONFIRMED','COMPLETED'))
   OR (status = 'CONFIRMED'    AND payment_status <> 'PAID')
ORDER BY created_at DESC;
```

Resolution order:

1. Confirm the true state in the **Razorpay dashboard** — it is the source of truth for money.
2. If captured but the booking is cancelled/expired, the system intentionally left the
   booking alone and raised a "Refund review required" admin notification. Issue the refund
   through Razorpay.
3. If captured and the booking is still `PENDING_PAYMENT`, a webhook was missed. Re-deliver
   it from the Razorpay dashboard rather than editing the database.
4. Only edit data as a last resort, with a second person reviewing.

> Never mark a booking `PAID` without confirming capture in Razorpay first.

---

## EMAIL

### Investigate a failure

```sql
SELECT id, template, recipient, status, attempt_count, failure_reason, created_at
FROM email_logs
WHERE status = 'FAILED'
ORDER BY created_at DESC LIMIT 50;
```

Retries run automatically on a fixed scheduler tick, with exponential backoff between
attempts, capped at `joyway.mail.max-retry-attempts` (default 3). Retries re-send a logged
email; they never re-run booking or payment logic, so they cannot duplicate a business
operation.

### Verify SMTP

```bash
az containerapp logs show -g $RG -n $APP --tail 200 | grep -i 'email'
```

Confirm `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and that
`SMTP_SSL`/`SMTP_STARTTLS` match the provider (port 465 → SSL; port 587 → STARTTLS).

### Verify aliases

```sql
SELECT template, status, COUNT(*), MAX(created_at)
FROM email_logs
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY template, status;
```

| Category | Expected From |
|---|---|
| `BOOKING` | bookings@joyway.in |
| `SUPPORT` | support@joyway.in |
| `AUTOMATED` | noreply@joyway.in |
| `GENERAL` / `CUSTOMER` | hello@joyway.in |

Send one test booking and confirm the received `From` header matches.

---

## DATABASE

### Backup verification

```bash
az postgres flexible-server show -g $RG -n $PG \
  --query "{retention:backup.backupRetentionDays, geo:backup.geoRedundantBackup}"
```

Confirming the setting is **not** the same as confirming recoverability. Run the restore
test in `DISASTER_RECOVERY.md` §10 on a schedule (quarterly is reasonable) and record the
result there.

### Restore

See `DISASTER_RECOVERY.md` §10. Always restore to a **new** server; never in place.

### Migrations

Flyway runs automatically at startup and is forward-only.

```sql
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC LIMIT 10;
```

Before deploying a migration: take a manual restore point, review it for backward
compatibility with the currently running version (a rolling deploy runs both simultaneously),
and prefer additive changes. Avoid dropping or renaming a column in the same release that
stops using it — split it across two releases.

If a migration fails, the application will not start. Resolve the schema state deliberately;
do not delete rows from `flyway_schema_history` to force startup.

---

## INCIDENTS

Detailed procedures live in `DISASTER_RECOVERY.md`. Quick index:

| Incident | Section | First action |
|---|---|---|
| Backend outage | §2 | Check revision + logs; roll back if it followed a deploy |
| Database outage | §1 | Check server state; do **not** restart backends first |
| Payment outage | §8 | Check Razorpay status; bookings still work as `PENDING_PAYMENT` |
| Email outage | §9 | Non-blocking; retries handle it |
| Crash loop | §4 | Read the *first* error after startup |
| Bad deploy | §6 | Roll back first, diagnose after |

Severity guide:

| Severity | Definition | Response |
|---|---|---|
| SEV1 | Customers cannot book or pay | Page immediately |
| SEV2 | Degraded (slow, partial failure) | Same business day |
| SEV3 | Cosmetic or internal only | Next working day |

---

## ROLLBACK

### Application

```bash
az containerapp revision list -g $RG -n $APP -o table
az containerapp ingress traffic set -g $RG -n $APP --revision-weight <previous>=100
```

### Container (Compose)

```bash
APP_VERSION=<previous-tag> docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps        # confirm healthy
```

### Database

Schema changes are **not** reversed by an application rollback. If the bad release included
a migration, treat schema reversal as a separate reviewed change, or restore per
`DISASTER_RECOVERY.md` §7.

### Post-rollback smoke test

```bash
curl -s $API/api/health/ready | jq
curl -s $API/api/packages | jq '.data | length'
curl -s -o /dev/null -w '%{http_code}\n' $SITE
```

Then confirm: admin login works, a booking lookup returns, and the dashboard mismatch
counters are zero.

---

## OPERATIONAL ACCEPTANCE TEST

Run after any significant deployment. Use **Razorpay test credentials only**.

| # | Step | Verify |
|---|---|---|
| 1 | `GET /api/health/ready` | `200`, database `UP` |
| 2 | Browse trips | Packages render |
| 3 | Open a trip | Details, price, dates correct |
| 4 | Create booking (with traveller documents) | `PENDING_PAYMENT`; `event=booking.created` |
| 5 | Start Razorpay test payment | `event=payment.order.created` |
| 6 | Complete payment | `event=payment.verified` |
| 7 | Webhook received | `event=payment.webhook.received`; row in `payment_webhook_events` |
| 8 | Database state | `status=CONFIRMED`, `payment_status=PAID` |
| 9 | Confirmation email | `email_logs.status=SENT`, from bookings@joyway.in |
| 10 | Booking lookup (reference + email) | Booking and receipt visible |
| 11 | Admin dashboard | Booking and payment visible; mismatch counters `0` |
| 12 | Cancel booking | `CANCELLED`; seats returned; cancellation email sent |

> There is no customer login. Customers book as guests and retrieve bookings at
> `/bookings/lookup` using booking reference + email. Only admins authenticate.
