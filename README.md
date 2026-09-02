# JOYWAY

**Find Joy in Every Journey.**

Production group travel booking platform for trips from Delhi and North India.

| | |
|---|---|
| **Website** | [joyway.in](https://joyway.in) |
| **API** | [api.joyway.in](https://api.joyway.in) |
| **Operator** | AJS Technologies Pvt Ltd. |

---

## Overview

JOYWAY is a full-stack travel booking product:

- **Public site** — browse trips, book seats, pay via Razorpay, manage bookings
- **Admin panel** — packages, bookings, drivers, vehicles, trips, notifications
- **API-first backend** — all business rules enforced server-side

```
joyway/
├── frontend/     Next.js 16 (TypeScript, Tailwind)
├── backend/      Spring Boot 3.4 (Java 21, PostgreSQL, Flyway)
├── docs/         Production runbooks and Azure guides
└── scripts/      Deployment helpers
```

| Layer | Stack |
|-------|--------|
| Frontend | Next.js App Router, TypeScript, Tailwind CSS |
| Backend | Spring Boot, Spring Security, JPA, Flyway |
| Database | PostgreSQL |
| Storage | Azure Blob Storage (production) |
| Payments | Razorpay (live in production) |
| Hosting | Azure Container Apps + Azure PostgreSQL |

---

## Security & secrets

**Never commit secrets to Git.** The repository only contains `.env.example` templates.

| Location | What goes there |
|----------|-----------------|
| **Git** | Source code, `.env.example` placeholders only |
| **Docker images** | Application binaries only — `.dockerignore` blocks all `.env*` files |
| **GitHub Actions** | Public build args only (`NEXT_PUBLIC_*`, CI placeholders) |
| **Azure Container Apps** | All runtime secrets (`JWT_SECRET`, DB, Razorpay, SMTP, storage) |

Production backend validates configuration at startup (`ProductionSecurityValidator`) and refuses to run with missing or insecure settings.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 24 LTS |
| Java | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 15+ (local dev) |

For Azure deployment: [Azure CLI](https://learn.microsoft.com/cli/azure/) and access to `rg-joyway-prod`.

---

## Local development

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
# Edit .env — set DATABASE_*, JWT_SECRET, optional ADMIN_BOOTSTRAP_* for first admin

mvn spring-boot:run
```

Runs at **http://localhost:8080**. Flyway applies migrations on startup.

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Runs at **http://localhost:3000**.

---

## Environment variables

### Frontend (`.env.local` / build args)

See [`frontend/.env.example`](frontend/.env.example). Only `NEXT_PUBLIC_*` values are embedded in the client bundle.

### Backend (`.env` or process env)

See [`backend/.env.example`](backend/.env.example). All payment, database, JWT, SMTP, and storage secrets stay server-side.

**Production:** inject variables in Azure Container Apps — not in GitHub or Dockerfiles.

---

## Build & test

```bash
# Backend
cd backend && mvn test && mvn package

# Frontend
cd frontend && npm run typecheck && npm run build
```

CI runs on every push to `main` (tests, typecheck, production build, Docker build, secret pattern scan).

---

## Production deployment (Azure)

Images are built in **Azure Container Registry** and deployed to **Container Apps**:

| Service | Resource |
|---------|----------|
| Frontend | `joyway-frontend-prod` → joyway.in |
| Backend | `joyway-backend-prod` → api.joyway.in |
| Registry | `joywayprodacr.azurecr.io` |
| Database | `joyway-pg-prod` |

```powershell
# From repo root (requires Azure CLI login)
.\scripts\deploy-azure-prod.ps1
```

Or trigger **Deploy Production** in GitHub Actions (pushes images to ACR; update Container Apps revision in Azure).

Detailed checklists: [`docs/PRODUCTION_CHECKLIST.md`](docs/PRODUCTION_CHECKLIST.md), [`docs/PRODUCTION_RUNBOOK.md`](docs/PRODUCTION_RUNBOOK.md).

---

## Key features

- Server-side pricing, seat locking, and payment verification
- Razorpay checkout + signed webhooks with idempotency
- Traveller ID documents in private Azure Blob storage
- Admin JWT in HttpOnly cookies with CSRF protection
- Rate limiting, CORS lockdown, and production security validator
- Privacy, Terms, and Cancellation policy pages

---

## Admin access

There is no public admin registration. Create the first admin locally or in Azure with bootstrap env vars, then set `ADMIN_BOOTSTRAP_ENABLED=false`.

Admin panel: **https://joyway.in/admin**

---

## API

Public endpoints include `/api/health`, `/api/packages`, `/api/bookings`, and `/api/payments`. Admin routes under `/api/admin/**` require `ROLE_ADMIN`.

Standard response format:

```json
{ "success": true, "data": {}, "message": "..." }
```

---

## Troubleshooting

| Issue | Check |
|-------|--------|
| Backend won't start | PostgreSQL running; `DATABASE_*` and `JWT_SECRET` set |
| Frontend can't reach API | `NEXT_PUBLIC_API_BASE_URL` matches backend URL |
| Production startup fails | Azure env vars — see `docs/PRODUCTION_CHECKLIST.md` |
| Flyway errors on prod | Do not edit applied migrations; add new versioned scripts |

---

## License

Proprietary — JOYWAY © 2026. All rights reserved.

Developed and managed by **AJS Technologies Pvt Ltd.**
