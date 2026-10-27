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
├── docs/              Production runbooks, Azure, monitoring, DR
├── scripts/           Deployment helpers
└── docker-compose*.yml
```

| Layer | Stack |
|-------|--------|
| Frontend | Next.js App Router, TypeScript, Tailwind CSS, Vitest |
| Backend | Spring Boot, Spring Security, JPA, Flyway, Maven |
| Database | PostgreSQL 15+ |
| Files | Azure Blob Storage (production) · local disk (dev) |
| Payments | Razorpay orders, verify, signed webhooks |
| Hosting | Azure Container Apps · Azure PostgreSQL · ACR |

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
| Docker | Optional (images / Sonar) |

Azure deploy also needs Azure CLI and access to resource group `rg-joyway-prod`.

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

API: **http://localhost:8080** (or the `SERVER_PORT` you set). Flyway migrates on startup.

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
| `backend/.env.example`, `frontend/.env.example` | **Yes** | Variable *names* + empty/safe placeholders so others know what to set |
| `application.yml` / `application-*.yml` | **Yes** | Spring config using `${ENV_VAR}` — no hard-coded secrets |
| Local `application-local.*`, DB dumps, `/storage` | **No** (gitignored) | Machine-specific data |

| Where | What belongs there |
|-------|--------------------|
| Git | Source + placeholder examples |
| Docker images | App binaries only (`.dockerignore` excludes `.env*`) |
| Local `.env` / `.env.local` | Your machine only |
| Azure Container Apps | All runtime secrets (JWT, DB, Razorpay secret, SMTP, storage) |

```bash
cd backend  && cp .env.example .env          # then fill real values
cd frontend && cp .env.example .env.local    # then fill real values
```

- Frontend: [`frontend/.env.example`](frontend/.env.example) — only `NEXT_PUBLIC_*` is safe in the browser.
- Backend: [`backend/.env.example`](backend/.env.example) — payments, JWT, DB, SMTP, storage stay server-side.
- Production refuses unsafe config via `ProductionSecurityValidator` (e.g. localhost CORS in `prod`).

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

CI on `main`: tests, typecheck, production build, Docker build, secret-pattern scan.

---

## Production (Azure)

| Resource | Role |
|----------|------|
| `joyway-frontend-prod` | [joyway.in](https://joyway.in) |
| `joyway-backend-prod` | [api.joyway.in](https://api.joyway.in) |
| `joywayprodacr` | Azure Container Registry |
| `joyway-pg-prod` | Azure Database for PostgreSQL |

Images are tagged like `prod-YYYYMMDD-HHMM` and also `latest` in ACR.

```bash
# Example: build in ACR (Azure CLI logged in)
az acr build -r joywayprodacr -t joyway-backend:TAG -f backend/Dockerfile backend
az acr build -r joywayprodacr -t joyway-frontend:TAG \
  --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.joyway.in \
  --build-arg NEXT_PUBLIC_RAZORPAY_KEY_ID="$RAZORPAY_KEY_ID" \
  -f frontend/Dockerfile frontend

# Roll Container Apps to the new tag
az containerapp update -g rg-joyway-prod -n joyway-backend-prod \
  --image joywayprodacr.azurecr.io/joyway-backend:TAG
az containerapp update -g rg-joyway-prod -n joyway-frontend-prod \
  --image joywayprodacr.azurecr.io/joyway-frontend:TAG
```

Helpers: `scripts/deploy-azure-prod.ps1`, GitHub Action **Deploy Production**.

**Frontend note:** `NEXT_PUBLIC_*` values are baked in at **image build time**. Changing the API URL or Razorpay public key requires a rebuild, not only a runtime env edit.

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
| [`docs/AZURE_BLOB_STORAGE.md`](docs/AZURE_BLOB_STORAGE.md) | Package / ID document storage |
| [`docs/AZURE_STORAGE_SETUP.md`](docs/AZURE_STORAGE_SETUP.md) | Storage setup |

---

## Troubleshooting

| Problem | What to check |
|---------|----------------|
| Backend won't start | Postgres up; `DATABASE_*` + `JWT_SECRET` set |
| Frontend can't reach API | `NEXT_PUBLIC_API_BASE_URL` matches backend host/port |
| Sonar `ECONNREFUSED :9000` | `docker start sonarqube`, wait until UI is up |
| Prod startup refused | Azure env / CORS — see production checklist |
| Flyway conflict on prod | Do not change applied migrations; add a new `Vnn__*.sql` |
| Payment hold expired | Guest unpaid holds expire; admin-created holds do not auto-expire |

---

## License

Proprietary — JOYWAY © 2026. All rights reserved.

Developed by [ajswork.space](https://www.ajswork.space/).
#   J o y w a y s  
 