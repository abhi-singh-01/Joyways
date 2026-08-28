# JOYWAY Production Deployment Checklist

Use this checklist before going live. Do not commit real secrets to Git.

## Infrastructure

- [ ] Azure PostgreSQL configured and reachable from backend
- [ ] Database credentials stored in a secure secret manager (not in Git)
- [ ] Flyway migrations applied (`V1` through latest) on production database
- [ ] Backend deployed with `SPRING_PROFILES_ACTIVE=prod`
- [ ] Frontend built with production `NEXT_PUBLIC_*` variables at build time

## Security secrets (server-side only)

- [ ] `JWT_SECRET` — strong random value (32+ characters), not a placeholder
- [ ] `JWT_COOKIE_SECURE=true` in production
- [ ] `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` configured
- [ ] `RAZORPAY_KEY_SECRET` configured (never in frontend)
- [ ] `RAZORPAY_WEBHOOK_SECRET` configured and matching Razorpay dashboard
- [ ] `SMTP_PASSWORD` configured (never in frontend)
- [ ] Storage credentials configured if using cloud storage

## CORS & URLs

- [ ] `CORS_ALLOWED_ORIGINS` set to exact HTTPS origins (no `*`, no localhost)
- [ ] `PUBLIC_API_BASE_URL` set to production API URL
- [ ] `FRONTEND_BASE_URL` set to production site URL
- [ ] `NEXT_PUBLIC_API_BASE_URL` set before frontend build
- [ ] No localhost URLs in production environment

## Admin

- [ ] First admin account created
- [ ] `ADMIN_BOOTSTRAP_ENABLED=false` in production
- [ ] Admin login tested
- [ ] Non-admin users receive 403 on `/api/admin/**`

## Authentication & authorization

- [ ] JWT login/logout works with secure cookie
- [ ] Expired/invalid JWT returns 401
- [ ] Customer booking lookup requires matching email
- [ ] Booking reference enumeration returns generic "Booking not found"

## Payments

- [ ] Razorpay live keys configured
- [ ] Payment signature verified server-side
- [ ] Webhook endpoint configured in Razorpay dashboard
- [ ] Webhook signature verification tested
- [ ] Duplicate webhook events handled idempotently
- [ ] Booking marked PAID only after server verification

## Email

- [ ] SMTP authentication works with `hello@joyway.in`
- [ ] Booking emails from `bookings@joyway.in`
- [ ] Support emails from `support@joyway.in`
- [ ] Automated emails from `noreply@joyway.in`
- [ ] General emails from `hello@joyway.in`

## File uploads

- [ ] Document upload size/type validation tested
- [ ] Traveller documents not publicly accessible
- [ ] Package images served only from allowed category

## Docker (if used)

- [ ] `docker compose -f docker-compose.prod.yml config` validates
- [ ] `docker compose -f docker-compose.prod.yml build` succeeds
- [ ] Secrets passed via environment, not baked into images
- [ ] `backend/.dockerignore` and `frontend/.dockerignore` present
      (Docker reads the ignore file inside the build context, **not** the repo root —
      without them, local `.env` files enter the build and override production build args)
- [ ] Verified no `.env` file inside built images
- [ ] Health check responds at `/api/health/ready`
- [ ] Resource limits and `stop_grace_period` set for both services

## Application health

- [ ] Backend liveness: `GET /api/health` returns `UP`
- [ ] Backend readiness: `GET /api/health/ready` returns `UP` with `database: UP`
- [ ] Readiness returns `503` when the database is unreachable
- [ ] Frontend loads and calls production API
- [ ] Error responses do not expose stack traces
- [ ] Production logs do not contain passwords, JWT, or payment secrets

## Operations readiness (Phase 11)

### Monitoring and logging
- [ ] Azure monitoring configured (container CPU/memory/restarts, HTTP 4xx/5xx, latency)
- [ ] Azure PostgreSQL metrics configured (CPU, storage, connections, availability)
- [ ] Application logs reaching Log Analytics
- [ ] Log retention period set (recommended: 90 days app, 365 days audit)
- [ ] Request correlation (`X-Request-ID`) verified end to end
- [ ] No secrets in logs (verified by sampling)

### Backups and recovery
- [ ] Database backups verified (retention + geo-redundancy confirmed)
- [ ] **Restore procedure actually tested** on a non-production server, result recorded
      in `DISASTER_RECOVERY.md` §10
- [ ] Rollback procedure tested
- [ ] Disaster recovery documented and owners named

### Alerts
- [ ] Critical alerts configured (backend down, database down, crash loop, sustained 5xx,
      webhook failures)
- [ ] High alerts configured (CPU, memory, DB storage, connection exhaustion, auth failures)
- [ ] Medium alerts configured (email failures, latency, booking failures)
- [ ] Alert action group has a reachable on-call recipient

### Payment and email monitoring
- [ ] Payment monitoring verified (`payment.*` events visible)
- [ ] Payment/booking mismatch counters visible on the admin dashboard and reading `0`
- [ ] Email monitoring verified (`email_logs` populated, retries capped)
- [ ] All four aliases verified by a live send

### Security and cost
- [ ] Rate limiting verified on login, booking and payment endpoints
- [ ] Secret scan completed across source, images and frontend bundle
- [ ] Any previously exposed credential rotated
- [ ] Azure cost budget configured with threshold notifications
- [ ] Production database sizing reviewed against expected load

### Documentation
- [ ] `docs/MONITORING.md` reviewed
- [ ] `docs/DISASTER_RECOVERY.md` reviewed, owners filled in
- [ ] `docs/PRODUCTION_RUNBOOK.md` reviewed
- [ ] Staging failure simulations completed
- [ ] Staging end-to-end acceptance flow passes

## Testing

- [ ] Backend: `mvn test` passes
- [ ] Frontend: `npm run lint` passes
- [ ] Frontend: `npm run build` passes
- [ ] End-to-end booking + payment flow tested in staging
- [ ] Admin operations tested (Phase 8 trips/vehicles/drivers)

## GitHub / repository security

- [ ] `.env` files are NOT tracked by Git
- [ ] Secret scan completed (no committed passwords/tokens)
- [ ] If secrets were ever committed: rotate/revoke those credentials

## Manual platform configuration

### Azure PostgreSQL
- Firewall rules allow backend host only
- SSL required (`sslmode=require`)
- Automated backups enabled

### Razorpay
- Live mode keys in production env
- Webhook URL: `https://api.joyway.in/api/payments/webhook`
- Webhook events: `payment.captured`, `payment.failed` (as applicable)

### SMTP (Hostinger)
- SPF/DKIM configured for `joyway.in`
- Aliases configured: bookings@, support@, noreply@

### DNS / reverse proxy (recommended)
- `joyway.in` → frontend
- `api.joyway.in` → backend
- HTTPS/TLS certificates installed

## Post-deploy verification

- [ ] Create test booking end-to-end
- [ ] Verify payment and confirmation email
- [ ] Verify admin dashboard loads with real metrics
- [ ] Monitor logs for errors in first 24 hours
