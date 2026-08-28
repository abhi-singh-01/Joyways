# JOYWAY — QA & Bug-Fix Report

Covers the Phase 9 QA/UAT pass and the defects found during the Phase 11 operations work.

| | |
|---|---|
| Backend tests | **78 passing**, 0 failures (baseline at start of QA: 53) |
| Frontend lint | **Clean** (7 errors at start) |
| Frontend build | **Passing** |
| Docker images | Backend and frontend both build and run |
| Defects found | 9 (2 critical, 4 high, 3 medium) — all fixed |

---

## Summary of defects

| ID | Severity | Area | Status |
|---|---|---|---|
| BUG-001 | **Critical** | Payments / inventory | Fixed |
| BUG-002 | **Critical** | Deployment / secrets | Fixed |
| BUG-003 | High | Payments | Fixed |
| BUG-004 | High | Deployment | Fixed |
| BUG-005 | High | API error handling | Fixed |
| BUG-006 | High | Frontend correctness | Fixed |
| BUG-007 | Medium | Validation UX | Fixed |
| BUG-008 | Medium | Security hardening | Fixed |
| BUG-009 | Medium | Observability | Fixed |

---

### BUG-001 — Cancelled bookings could be resurrected by a payment (Critical)

**Area:** `PaymentService`

Neither `confirmPayment` nor the webhook handler checked booking status before confirming.
A booking that had been cancelled or expired — with its seats already returned to inventory —
could be flipped back to `CONFIRMED` by a late payment or a replayed webhook. Every
occurrence permanently corrupted the seat count, overselling the trip.

**Fix.** Added an `isPayable` guard (only `PENDING_PAYMENT` qualifies) to both paths.
Verification on a non-payable booking is rejected. A webhook for a non-payable booking still
records the captured payment — the money is real and must not be lost — but leaves the
booking alone and raises a "Refund review required" admin notification.

**Tests.** `PaymentServiceTest`: `verifyPayment_rejectsCancelledBookingAndLeavesItCancelled`,
`webhook_capturesPaymentButDoesNotConfirmCancelledBooking`, `webhook_confirmsPendingBooking`,
`webhook_rejectsInvalidSignature`.

---

### BUG-002 — Local `.env` files entered the Docker build and overrode production config (Critical)

**Area:** `frontend/Dockerfile`, build context

`.dockerignore` is read from **inside the build context directory**, not the repository root.
Both images build with a subdirectory context (`./frontend`, `./backend`), so the root
`.dockerignore` was never applied.

Two consequences:

1. **Wrong configuration shipped to production.** The frontend Dockerfile does `COPY . .`,
   so the developer's `frontend/.env.local` was copied into the build stage. Next.js gives
   `.env.local` precedence over build args when inlining `NEXT_PUBLIC_*`, so a production
   image built on a developer machine would silently bake in the *local* API URL and
   Razorpay key instead of the intended production values — a working build pointing at the
   wrong backend.
2. **Build context bloat.** `node_modules` and `.next` were transferred on every build:
   **480MB+ and still climbing** after 10 minutes, versus 568KB after the fix.

**Fix.** Added `frontend/.dockerignore` and `backend/.dockerignore`, both excluding `.env*`,
dependency directories and build output.

**Verification.** Context transfer 480MB → **568KB**. Confirmed no `.env` file exists
anywhere in either built image, that the bundle contains the value passed via `--build-arg`,
and that no `localhost:8080` reference survives in the static bundle.

---

### BUG-003 — Race condition creating duplicate Razorpay orders (High)

**Area:** `PaymentService.createOrder`

Concurrent requests for the same booking could each find no existing payment record and each
create a separate Razorpay order, allowing a customer to be charged twice for one booking.

**Fix.** `createOrder` now takes a pessimistic row lock via
`findByBookingReferenceForUpdate` before checking for an existing order, serialising
concurrent attempts. Ownership is still verified before the lock is taken.

---

### BUG-004 — Uploads failed in production containers (High)

**Area:** `backend/Dockerfile`

The image switched to the non-root `joyway` user before `/app/storage` existed. Docker then
created the mount point as `root`, so the application could not write to it — every traveller
document and package image upload failed with a permission error. Not reproducible outside a
container, so local testing would never surface it.

**Fix.** Create and `chown` `/app/storage` to `joyway` before the `USER` directive.

---

### BUG-005 — Client mistakes returned HTTP 500 (High)

**Area:** `GlobalExceptionHandler`

Malformed JSON, a missing multipart part, a missing query parameter, a type mismatch, a wrong
HTTP method, an oversized upload and constraint violations all fell through to the catch-all
handler and returned `500`. This makes genuine server faults indistinguishable from client
errors in monitoring, and misleads API consumers into retrying unretryable requests.

**Fix.** Added handlers mapping each to its correct status: `400` for malformed or missing
input, `405` for method not allowed, `415` for unsupported media type, `413` for oversized
uploads, `409` for data integrity violations.

**Tests.** `BookingControllerTest` covers invalid JSON, malformed JSON, missing parameter,
missing multipart part and unsupported method.

---

### BUG-006 — Stale responses could overwrite fresh data in admin views (High)

**Area:** 7 frontend components

`setState` called synchronously inside `useEffect` (flagged by `react-hooks/set-state-in-effect`,
7 lint errors). Beyond the lint failure, none of the affected fetches guarded against
out-of-order responses, so a slow earlier request could overwrite the results of a faster
later one — a user typing in the search box could be left looking at results for a previous
query.

**Fix.** Rewrote each effect with a `cancelled` guard so a superseded response is discarded,
and moved `setLoading(true)` into the event handlers that actually trigger a refetch.
Affected: `AdminTripsView`, `AdminVehiclesView`, `AdminDriversView`, `AdminTripCalendarView`,
`AdminNotificationBell`, admin booking detail page, booking lookup page.

---

### BUG-007 — Booking validation errors were opaque in production (Medium)

Programmatic validation threw `IllegalArgumentException`, which the production handler
flattened to "Invalid request." with no indication of which field was wrong.

**Fix.** Added `RequestValidationException` carrying per-field errors, and a handler
returning `400` with `errorCode: VALIDATION_FAILED` and the field map — matching the shape
already produced for `@Valid` failures.

---

### BUG-008 — Path traversal check missed Windows separators (Medium)

**Area:** `PublicFileController`

The filename guard rejected `..` and `/` but not `\`, leaving a gap on Windows hosts.

**Fix.** Added `\` to the rejected set.

---

### BUG-009 — No request correlation, and unsafe log formatting (Medium)

Log lines carried no correlation id, so a customer-reported failure could not be traced to
its log entries.

**Fix.** Added `RequestCorrelationFilter` (`X-Request-ID`), MDC-backed log pattern, and
`requestId` returned in server-error responses. Client-supplied ids are validated against
`^[A-Za-z0-9._-]{1,64}$` before use — echoing raw client input into logs would permit forged
log entries via injected newlines.

---

## Security review

| Check | Result |
|---|---|
| Secrets in source | **Clean** — no hardcoded credentials, no `rzp_live` keys |
| Secrets in images | **Clean** — no `.env` in either image (after BUG-002) |
| Secrets in frontend bundle | **Clean** — no `localhost`, only the supplied build arg |
| Secrets in logs | **Clean** — recipients masked, DB errors log exception type only |
| Secrets in Git | Not applicable — this workspace is not a Git repository |
| File upload validation | Robust — size, MIME and extension checks in `DocumentStorageService` |
| Booking enumeration | Mitigated — mismatched lookups return a generic "Booking not found" |
| Webhook forgery | Mitigated — HMAC verification fails closed when the secret is unset |
| Admin authorisation | Verified — `/api/admin/**` requires `ROLE_ADMIN` |

### Outstanding security action

**`backend/.env` on this machine contains real credentials** — database password, JWT secret,
SMTP password, Razorpay secret and the admin bootstrap password. The file is not in version
control and no longer reaches the Docker images, so there is no evidence of external
exposure. However, if this machine has ever been shared, or these values were ever pasted
into a chat, ticket or screenshot, **rotate all of them before go-live.**

---

## Observations (not defects)

**Customer accounts do not exist.** Earlier phase specifications referenced customer
registration and login. The implemented model is different and internally consistent: `User`
covers authenticated admins only; `Customer` is booking contact data with no credentials.
Customers book as guests and retrieve bookings with reference + email. No fix needed — but
test plans referring to "customer login" are testing a feature that does not exist.

**Duplicate customer rows.** `BookingService` creates a new `Customer` per booking rather
than reusing an existing record with the same email. This does not corrupt any reported
metric, and deduplicating touches the booking-history model, so it is a design decision
rather than a bug.

**Failed payments hold seats indefinitely.** A booking stuck in `PENDING_PAYMENT` keeps its
seats reserved with no expiry sweeper. On a nearly-full trip, abandoned checkouts will block
real bookings. Fixing this needs a business decision on the hold window (15 minutes? 24
hours?) and is a feature, not a bug fix. **Recommended before high-volume launch.**

---

## Not covered

These require a deployed staging environment and Azure access, neither available in this
environment:

- End-to-end flow against real Razorpay test infrastructure (webhook delivery from Razorpay)
- Real SMTP delivery and alias verification
- Load and concurrency testing against production-like infrastructure
- Backup/restore verification
