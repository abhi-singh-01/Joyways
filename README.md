# JOYWAY

**Find Joy in Every Journey.**

JOYWAY is a modern group travel booking platform for trips departing from Delhi to destinations across North India.

- **Website:** [joyway.in](https://joyway.in)
- **Email:** hello@joyway.in

---

## Architecture

```
joyway/
├── frontend/          Next.js 16 (App Router, TypeScript, Tailwind CSS)
├── backend/           Spring Boot 3.4 (Java 21, PostgreSQL, Flyway)
└── README.md
```

| Layer | Technology |
|-------|------------|
| Frontend | Next.js, TypeScript, Tailwind CSS, Framer Motion, React Three Fiber |
| Backend | Spring Boot 3, Spring Security, Spring Data JPA, Flyway |
| Database | PostgreSQL |
| Payments | Razorpay (Phase 5+) |
| Email | SMTP via hello@joyway.in (Phase 7+) |

The frontend and backend are **logically independent**. All business logic lives in the Spring Boot API. The Next.js app is a client that consumes REST APIs.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 24 LTS |
| Java | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 15+ |

---

## Quick Start

### 1. PostgreSQL Setup

```sql
CREATE DATABASE joyway;
CREATE USER joyway WITH ENCRYPTED PASSWORD 'joyway';
GRANT ALL PRIVILEGES ON DATABASE joyway TO joyway;
```

### 2. Backend

```bash
cd backend
cp .env.example .env
# Edit .env with your database credentials and JWT secret

# Set environment variables (PowerShell example)
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/joyway"
$env:DATABASE_USERNAME="joyway"
$env:DATABASE_PASSWORD="joyway"
$env:JWT_SECRET="your-long-random-secret-key-minimum-256-bits"
$env:ADMIN_BOOTSTRAP_PASSWORD="your-secure-admin-password"

mvn spring-boot:run
```

Backend runs at **http://localhost:8080**

Flyway automatically creates the schema and seeds 5 demo packages (Nainital, Mussoorie, Rishikesh, Haridwar, Dalhousie).

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local

npm install
npm run dev
```

Frontend runs at **http://localhost:3000**

---

## Environment Variables

### Frontend (`frontend/.env.local`)

| Variable | Description |
|----------|-------------|
| `NEXT_PUBLIC_API_BASE_URL` | Backend API URL (default: `http://localhost:8080`) |
| `NEXT_PUBLIC_RAZORPAY_KEY_ID` | Razorpay public key (Phase 5+) |

### Backend (`backend/.env` or system env)

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Database username |
| `DATABASE_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (256+ bits) |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `ADMIN_BOOTSTRAP_ENABLED` | Create first admin on startup |
| `ADMIN_BOOTSTRAP_EMAIL` | Bootstrap admin email |
| `ADMIN_BOOTSTRAP_PASSWORD` | Bootstrap admin password |
| `RAZORPAY_KEY_ID` | Razorpay key (Phase 5+) |
| `RAZORPAY_KEY_SECRET` | Razorpay secret (Phase 5+) |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook secret (Phase 6+) |
| `SMTP_*` | Email configuration (Phase 7+) |
| `STORAGE_*` | Document storage (Phase 4+) |

See `.env.example` files in each directory for the full list.

---

## Admin Bootstrap

There is **no public admin registration page**. The first admin is created via secure bootstrap:

1. Set `ADMIN_BOOTSTRAP_ENABLED=true`
2. Set `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD`
3. Start the backend — admin is created once
4. Set `ADMIN_BOOTSTRAP_ENABLED=false` after first login

Admin panel: **http://localhost:3000/admin** (Phase 3+)

---

## API Endpoints

### Public

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/packages` | List published packages |
| GET | `/api/packages/id/{id}` | Package by ID (for booking) |
| GET | `/api/packages/{slug}` | Package details |
| POST | `/api/bookings/preview` | Server-side price & availability preview |
| POST | `/api/bookings` | Create booking (multipart: JSON + ID documents) |

### Admin (requires ROLE_ADMIN + JWT cookie)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/auth/login` | Admin login (sets HttpOnly cookie) |
| POST | `/api/admin/auth/logout` | Admin logout |
| GET | `/api/admin/auth/me` | Current admin profile |
| GET | `/api/admin/dashboard` | Dashboard metrics |
| GET | `/api/admin/packages` | List all packages (all statuses) |
| POST | `/api/admin/packages` | Create package |
| GET | `/api/admin/packages/{id}` | Package detail |
| PUT | `/api/admin/packages/{id}` | Update package |
| PATCH | `/api/admin/packages/{id}/status` | Publish/pause/etc. |
| DELETE | `/api/admin/packages/{id}` | Delete package |
| POST | `/api/admin/packages/{id}/images/url` | Add image by URL |
| POST | `/api/admin/packages/{id}/images/upload` | Upload image file |
| PATCH | `/api/admin/packages/{id}/images/{imageId}` | Update image |
| DELETE | `/api/admin/packages/{id}/images/{imageId}` | Delete image |

All responses use the standard format:

```json
{ "success": true, "data": {}, "message": "..." }
```

---

## Customer Booking Flow (Phase 4)

1. Select a trip → **Book Now** → `/book/{packageId}`
2. Choose number of travellers (respects available seats)
3. Enter primary contact + per-traveller details and upload ID documents
4. Review server-calculated pricing via `POST /api/bookings/preview`
5. Confirm → booking created with status `PENDING_PAYMENT` and reference `JW-YYYY-XXXXX`

**Server guarantees:**
- Price calculated on the backend (`pricePerPerson × travellers`)
- Pessimistic row lock on package prevents overselling
- ID documents stored via private `DocumentStorageService` (category `traveller-documents`)
- Document storage keys are never exposed in public API responses

Payment (Razorpay) is Phase 5.

---

## Database Schema

Tables created by Flyway (`V1__initial_schema.sql`):

- `roles`, `users`, `user_roles` — Admin authentication
- `packages`, `package_images` — Travel packages
- `customers`, `bookings`, `travellers` — Booking flow
- `payments`, `payment_webhook_events` — Payments (Phase 5+)

Demo data seeded in `V2__seed_demo_packages.sql` (clearly marked DEMO).

---

## Build & Test

### Backend

```bash
cd backend
mvn compile          # Compile
mvn test             # Run tests (19 tests)
mvn package          # Build JAR
```

### Frontend

```bash
cd frontend
npm run lint         # ESLint
npx tsc --noEmit     # TypeScript check
npm run build        # Production build
```

---

## Security Notes

- Admin and customer experiences are **completely separated**
- Spring Security enforces `ROLE_ADMIN` on all `/api/admin/**` endpoints
- JWT tokens stored in HttpOnly cookies (Phase 3+)
- Identity documents use private storage abstraction (never public URLs)
- Never commit `.env` files or secrets to Git
- Razorpay secrets stay server-side only

---

## Phase Roadmap

| Phase | Status | Scope |
|-------|--------|-------|
| 1 | **Complete** | Project setup, homepage, package API, demo data |
| 2 | **Complete** | Trip listing, trip details, responsive UI, animations |
| 3 | **Complete** | Admin auth, dashboard, package CRUD |
| 4 | **Complete** | Customer booking flow (no payment yet) |
| 5 | Pending | Razorpay test mode |
| 6 | Pending | Webhooks, payment state |
| 7 | Pending | Email, receipts, booking lookup |
| 8 | Pending | Admin booking/customer/payment management |
| 9 | Pending | Security hardening, testing |
| 10 | Pending | Production deployment docs |

---

## Troubleshooting

**Backend won't start — database connection refused**
- Ensure PostgreSQL is running and credentials match `.env`

**Frontend shows no trips**
- Ensure backend is running at `NEXT_PUBLIC_API_BASE_URL`
- Check `GET http://localhost:8080/api/packages` returns data

**Flyway migration failed**
- Drop and recreate the `joyway` database for a clean start

**Admin bootstrap skipped**
- Set `ADMIN_BOOTSTRAP_PASSWORD` to a non-empty value

---

## License

Proprietary — JOYWAY © 2026
