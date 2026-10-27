# JOYWAY

**Find Joy in Every Journey.**

Production group-travel booking platform for curated trips from Delhi and North India.

| | |
|---|---|
| **Live site** | [joyway.in](https://joyway.in) |
| **API** | [api.joyway.in](https://api.joyway.in) · [`/api/health/ready`](https://api.joyway.in/api/health/ready) |
| **Admin** | [joyway.in/admin](https://joyway.in/admin) |
| **Operator** | JOYWAY |

---

## What this product does

Guests book without creating an account. They pick a trip, enter travellers, pay with Razorpay (or complete payment later via booking lookup), and get email confirmation plus a PDF-ready receipt path.

Operators run the business from the admin panel: packages, seats, drivers, vehicles, trip assignments, refunds, offline/manual bookings, waitlist, and ops notifications.

```
Browse trips → Book seats → Pay (Razorpay / offline) → Email + lookup → Admin ops
```

---

## Repository layout

```text
joyway/
├── frontend/          Next.js 16 · React 19 · TypeScript · Tailwind · GSAP
├── backend/           Spring Boot 3.4 · Java 21 · PostgreSQL · Flyway
├── docs/              Production runbooks, monitoring, DR
├── scripts/           Deployment helpers
└── docker-compose*.yml
```

| Layer | Stack |
|-------|--------|
| Frontend | Next.js App Router, TypeScript, Tailwind CSS, Vitest |
| Backend | Spring Boot, Spring Security, JPA, Flyway, Maven |
| Database | PostgreSQL 15+ |
| Files | Cloudflare R2 (production) · local disk (dev) |
| Payments | Razorpay orders, verify, signed webhooks |
| Hosting | Vercel (frontend) · Render Docker (backend) · Supabase/Neon (DB) |

---

## Product features

**Public**
- Cinematic homepage wired to live published packages
- Trip browse / detail, seat availability, pickup-point selection
- Guest checkout (no customer login)
- Razorpay payment + booking lookup / resume unpaid holds
- Waitlist when a trip is sold out
- Post-trip reviews after completion
- WhatsApp deep-link + contact enquiry
- Privacy, Terms, Cancellation pages

**Admin** (`/admin`)
- Packages, images, publish status
- Bookings list + detail; cancel unpaid; refund / void offline
- **Manual booking** — create holds or mark paid offline
- Drivers, vehicles, trips, calendar, auto-assign on confirm
- Notifications, audit log, email log

**Platform**
- Server-side pricing, capacity locks, payment verification
- Webhook idempotency and amount checks
- Admin JWT in HttpOnly cookies + CSRF
- Rate limiting, CORS lockdown, `ProductionSecurityValidator`
- Flyway versioned migrations (never edit applied scripts)

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 24 LTS (`>=24 <25`) |
| Java | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 15+ (local) |
| Docker | Optional (local compose / Sonar) |

---

## Quick start (local)

### 1. Database

```sql
CREATE DATABASE joyway;
CREATE USER joyway WITH ENCRYPTED PASSWORD 'your-local-password';
GRANT ALL PRIVILEGES ON DATABASE joyway TO joyway;
```

### 2. Backend

```bash
cd backend
cp .env.example .env
# Set DATABASE_*, JWT_SECRET (32+ chars). Optional ADMIN_BOOTSTRAP_* for first admin.

mvn spring-boot:run
```

API: **http://localhost:8080**. Flyway runs all migrations on first startup.

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
# NEXT_PUBLIC_API_BASE_URL must match the backend (e.g. http://localhost:8080)

npm ci
npm run dev
```

Site: **http://localhost:3000**

> If the backend runs on another port (e.g. `8082`), set `NEXT_PUBLIC_API_BASE_URL` accordingly.

---

## Environment & secrets

**Never commit real secrets.** Copy the examples locally; Git only keeps the templates.

| File | On GitHub? | Why |
|------|------------|-----|
| `backend/.env`, `frontend/.env.local` | **No** (gitignored) | Real passwords, JWT, Razorpay secret |
| `backend/.env.example`, `frontend/.env.example` | **Yes** | Variable *names* + safe placeholders |
| `application.yml` / `application-*.yml` | **Yes** | Spring config using `${ENV_VAR}` — no hard-coded secrets |

| Where | What belongs there |
|-------|-------------------|
| Git | Source + placeholder examples |
| Docker images | App binaries only (`.dockerignore` excludes `.env*`) |
| Local `.env` / `.env.local` | Your machine only |
| Vercel | All frontend `NEXT_PUBLIC_*` env vars |
| Render | All backend runtime secrets (JWT, DB, Razorpay, SMTP, R2) |

```bash
cd backend  && cp .env.example .env        # then fill real values
cd frontend && cp .env.example .env.local  # then fill real values
```

- Frontend: [`frontend/.env.example`](frontend/.env.example) — only `NEXT_PUBLIC_*` is safe in the browser.
- Backend: [`backend/.env.example`](backend/.env.example) — payments, JWT, DB, SMTP, storage stay server-side.
- Production refuses unsafe config via `ProductionSecurityValidator` (e.g. localhost CORS in `prod` profile).

---

## Common commands

```bash
# Backend
cd backend
mvn test
mvn -q verify
mvn package

# Frontend
cd frontend
npm run typecheck
npm run lint
npm run test
npm run build

# SonarQube (local Docker on :9000; export SONAR_TOKEN in your shell)
docker start sonarqube   # if stopped
cd backend  && mvn verify sonar:sonar
cd frontend && npm run sonar
```

CI on `main`: tests, typecheck, production build, Docker build smoke-test, secret-pattern scan.

---

## Production

| Service | Provider | URL |
|---------|----------|-----|
| Frontend | **Vercel** | [joyway.in](https://joyway.in) |
| Backend (Docker) | **Render** | [api.joyway.in](https://api.joyway.in) |
| Database | **Supabase / Neon** | PostgreSQL 15+ |
| File storage | **Cloudflare R2** | S3-compatible, `joyway-documents` bucket |

### How deploys work

| What | How |
|------|-----|
| **Frontend** | Vercel auto-deploys on every push to `main` — no manual step needed |
| **Backend** | Render auto-deploys on every push to `main` via GitHub integration |
| **Manual redeploy** | GitHub Actions → **Deploy Production** (workflow_dispatch) — triggers Render deploy hook |

### Key environment variables

**Frontend** (set in Vercel dashboard):

| Variable | Example |
|----------|---------|
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.joyway.in` |
| `NEXT_PUBLIC_RAZORPAY_KEY_ID` | `rzp_live_…` |
| `NEXT_PUBLIC_STORAGE_BASE_URL` | `https://pub-abc123.r2.dev` |

> `NEXT_PUBLIC_*` values are baked in at **build time** by Vercel. Changing them triggers an automatic rebuild.

**Backend** (set in Render dashboard):

| Variable | Notes |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://…` (Supabase/Neon) |
| `JWT_SECRET` | 64+ random chars |
| `JWT_COOKIE_DOMAIN` | `.joyway.in` |
| `CORS_ALLOWED_ORIGINS` | `https://joyway.in,https://www.joyway.in` |
| `STORAGE_PROVIDER` | `s3` |
| `STORAGE_ENDPOINT` | `https://<account-id>.r2.cloudflarestorage.com` |
| `STORAGE_ACCESS_KEY` | R2 access key ID |
| `STORAGE_SECRET_KEY` | R2 secret access key |
| `STORAGE_PUBLIC_BASE_URL` | `https://pub-abc123.r2.dev` |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Server-side only |
| `SMTP_*` | Email delivery credentials |

> **Render free tier** spins down after 15 min of inactivity (~30–60 s cold start). Set up an [UptimeRobot](https://uptimerobot.com) monitor on `/api/health/ready` every 5 minutes to keep it warm.

### GitHub secrets required

| Secret | Where to get it |
|--------|----------------|
| `RENDER_DEPLOY_HOOK_URL` | Render → service → Settings → Deploy Hook |

---

## Admin bootstrap

There is no public admin signup.

1. Enable one-time bootstrap env vars (`ADMIN_BOOTSTRAP_*`) on the backend.
2. Start the app once so the admin user is created.
3. Set `ADMIN_BOOTSTRAP_ENABLED=false` immediately after.

Panel: **https://joyway.in/admin**

---

## API shape

Public routes include `/api/health`, `/api/packages`, `/api/bookings`, `/api/payments`.  
Admin routes live under `/api/admin/**` and require `ROLE_ADMIN`.

```json
{ "success": true, "data": {}, "message": "..." }
```

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [`docs/PRODUCTION_CHECKLIST.md`](docs/PRODUCTION_CHECKLIST.md) | Pre-go-live checks |
| [`docs/PRODUCTION_RUNBOOK.md`](docs/PRODUCTION_RUNBOOK.md) | Day-to-day ops |
| [`docs/MONITORING.md`](docs/MONITORING.md) | Health, alerts, metrics |
| [`docs/DISASTER_RECOVERY.md`](docs/DISASTER_RECOVERY.md) | Recovery procedures |

---

## Troubleshooting

| Problem | What to check |
|---------|--------------|
| Backend won't start | Postgres reachable; `DATABASE_*` + `JWT_SECRET` set in Render env |
| Frontend can't reach API | `NEXT_PUBLIC_API_BASE_URL` in Vercel env matches Render backend URL |
| Images not loading | `NEXT_PUBLIC_STORAGE_BASE_URL` set; R2 bucket has public access enabled |
| Sonar `ECONNREFUSED :9000` | `docker start sonarqube`, wait until UI is up |
| Prod startup refused | CORS origins — must not contain `localhost` in `prod` profile |
| Flyway conflict on prod | Do not edit applied migrations; add a new `Vnn__*.sql` |
| Payment hold expired | Guest unpaid holds auto-expire; admin-created holds do not |
| Render cold start slow | Add UptimeRobot monitor on `/api/health/ready` every 5 minutes |

---

## License

Proprietary — JOYWAY © 2026. All rights reserved.

Developed by [ajswork.space](https://www.ajswork.space/).
