# NETRA: Blood Donation Platform

NETRA is a production-grade digital blood donation ecosystem engineered with zero-trust healthcare data safeguards, versioned clinical pre-screening, and a responsive Flutter mobile-first architecture.

---

## Implemented Modules & Capabilities

### 1. Authentication & Role-Based Access Control (RBAC)
- **Stateless Authentication**: Signed JWT access tokens with secure refresh token rotation (`/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`).
- **Granular RBAC**: Role enforcement supporting `ROLE_DONOR`, `ROLE_RECEIVER`, `ROLE_ORGANIZATION`, `ROLE_ADMIN`, and `ROLE_BLOODBANK`.
- **Security Audit Logging**: Comprehensive audit trail with SHA-256 IP hashing, client metadata capture, and zero sensitive credential leakage.

### 2. User & Donor Profiles
- **User Account Management**: User profile inspection and updates (`/api/v1/profile/me`).
- **Donor Profile Lifecycle**: Donor profiles (`/api/v1/donor/profile`) with blood group, verification status (`SELF_REPORTED` by default, `VERIFIED` upon clinical confirmation), availability status (`AVAILABLE`, `UNAVAILABLE`, `PAUSED`), and geographic coordinates.
- **Coordinate Validation & Privacy**: Optional latitude (-90.0 to 90.0) and longitude (-180.0 to 180.0) paired validation (both must be supplied or both omitted). Coordinates remain private to the donor and internal matching logic, never exposed through public donor endpoints.

### 3. Donation Eligibility Self-Screening
- **Clinical Rule Engine (`INDIA-NBTC-2026-01`)**: Server-side pre-screening modeled on National Blood Transfusion Council (NBTC) India and Drugs & Cosmetics Rules statutory guidelines.
- **Progressive 6-Step Questionnaire**: Evaluates age, weight, recovery interval (90-day male, 120-day female), recent illnesses, active medications, surgical/dental procedures, and chronic conditions.
- **Authoritative Preliminary Results**: Returns `LIKELY_ELIGIBLE`, `TEMPORARY_DEFERRAL` (with estimated return date), `MEDICAL_REVIEW_REQUIRED`, or `INSUFFICIENT_INFORMATION`.
- **Core Medical Safety Tenet**: Acts strictly as a preliminary self-screening assistant for decision support. Final medical clearance is performed by blood bank and clinical staff prior to collection.
- **Rate Limiting**: Fixed-window rate limiting of 20 requests per minute.

### 4. Blood Bank Registry
- **Directory & Verification**: Directory of licensed blood banks (`/api/v1/blood-banks`) with verification status, 24/7 emergency service indicators, contact details, and location coordinates.
- **Spatial Discovery**: Geographic bounding-box and distance filtering for discovering nearby blood banks.

### 5. Blood Inventory Management
- **Component-Level Tracking**: Inventory management (`/api/v1/blood-banks/{id}/inventory`) tracking units across Whole Blood, Packed Red Blood Cells (PRBC), Platelet Concentrates, Fresh Frozen Plasma (FFP), and Cryoprecipitate.
- **Concurrency Protection**: Optimistic locking (`@Version`) preventing race conditions and double-allocation during concurrent inventory operations.

### 6. Donation Events & Drives
- **Public & Blood Bank Drives**: Donation camp scheduling (`/api/v1/donation-events`) with venue information, date/time boundaries, and donor capacity limits.
- **Participant Registration**: Registration management with duplicate registration prevention and capacity controls.

### 7. Blood Requests
- **Patient & Hospital Requests**: Blood requests (`/api/v1/blood-requests`) specifying ABO/Rh group, units needed, hospital details, location coordinates, deadline (`requiredBy`), and urgency (`NORMAL`, `URGENT`, `CRITICAL`).
- **Lifecycle Management**: Strict state transitions across `OPEN`, `FULFILLED`, `CANCELLED`, and `EXPIRED`.
- **BOLA / IDOR Defense**: Broken Object Level Authorization enforcement ensuring requests can only be managed by verified owners or administrators.

### 8. Emergency Mode V1
- **Emergency Blood Request Fast-Path**: Dedicated endpoint (`/api/v1/emergency/blood-requests`) for expedited creation of high-urgency blood requests with `CRITICAL` urgency (record creation only; zero automated external notifications, SMS, or broadcast messaging).
- **Strict Rate Limiting**: Max 5 new requests per fixed 10-minute window per authenticated user, with counter rollback on downstream creation failures.
- **Idempotency Protection**: Idempotency-Key validation and request fingerprinting preventing duplicate emergency request submissions.
- **Audited Cancellation**: Secure cancellation endpoint (`/api/v1/emergency/blood-requests/{id}/cancel`) with authorization-first validation order.

### 9. Donor Matching Engine V1
- **Computed Candidate Discovery**: On-demand candidate matching endpoint (`GET /api/v1/blood-requests/{requestId}/matches`) finding compatible, available, and eligible verified donors for open blood requests.
- **Blood Compatibility Matrix**: Centralized compatibility rule set for preliminary donor candidate matching (scoped strictly to Red Blood Cell and Whole Blood transfusions; decision-support only; final compatibility, screening, and crossmatching are determined by qualified blood-bank and clinical staff).
- **Verified-Only Donor Pool**: Hard server-side filtering ensuring only donors with `VERIFIED` blood group status participate in candidate discovery; unverified and self-reported donors are strictly excluded.
- **Two-Stage Spatial Filtering**: Database-level bounding box hard filtering followed by exact spherical Haversine distance calculation within configurable search radii (10km, 25km, 50km, 100km).
- **Deterministic 3-Tier Ranking**:
  1. `EXACT` compatibility before `COMPATIBLE`
  2. Proximity ascending (`distanceKm`)
  3. Internal `donorProfileId` tie-breaker
- **Authorized Internal Selection Reference**: Exposes `candidateReference` (the donor profile reference, not a secret token) to decouple requester selection from internal user account identifiers (`users.id`).
- **Bounded Rate Limiting**: 30 matching requests per minute per authenticated user.

### 10. Donor Response V1
- **Persistent Match Workflow**: Stateful link between Blood Requests and matched donors backed by the `donor_matches` table.
- **Endpoints**:
  - `GET /api/v1/donor/matches`: Authenticated donors inspect incoming match requests for their profile.
  - `GET /api/v1/donor/matches/{matchId}`: Authenticated donors inspect match and request details.
  - `POST /api/v1/donor/matches/{matchId}/accept`: Donors accept an active match request.
  - `POST /api/v1/donor/matches/{matchId}/decline`: Donors decline an active match request.
  - `POST /api/v1/blood-requests/{requestId}/matches`: Authorized requesters create persistent matches with candidate revalidation and row locking.
  - `GET /api/v1/blood-requests/{requestId}/match-responses`: Requesters view persistent match responses for their request.
- **Strict Active Match Cap**: Max 10 active `MATCHED` records per blood request to prevent request spam.
- **State Machine Transitions**: Controlled lifecycle across `MATCHED`, `ACCEPTED`, `DECLINED`, `EXPIRED`, and `CANCELLED`. Terminal states (`ACCEPTED`, `DECLINED`, `EXPIRED`, `CANCELLED`) are strictly immutable with optimistic concurrency locking (`@Version`).
- **Cascading Lifecycle Management**: Automatic expiration and cancellation propagation when parent blood requests expire or are cancelled.
- **Zero Involuntary Side-Effects**: Accepting/declining a match is strictly a response recording operation. It does **not** fulfill the blood request, does **not** modify blood inventory, does **not** alter `lastDonationDate`, does **not** generate verified donation records, and does **not** grant clinical clearance.
- **Zero Automated Notifications**: No automated SMS, push notifications, emails, robocalls, or in-app messaging. Communication is entirely user-driven through dashboards and clinical staff.
- **Rate Limiting**: 20 match creation requests per minute per authenticated user.

### 11. Notifications V1 & Push Delivery
- **In-App Notification Feed**: Secure, paginated user notification retrieval and read status tracking (`/api/v1/notifications`, `/api/v1/notifications/unread-count`, `/api/v1/notifications/{id}/read`, `/api/v1/notifications/read-all`).
- **Domain Event Driven**: Automatically generates notifications upon verified domain state transitions:
  - `MATCH_CREATED` (for matched donors)
  - `MATCH_ACCEPTED` (for requesters)
  - `MATCH_DECLINED` (for requesters)
  - `MATCH_EXPIRED` (for matched donors)
  - `BLOOD_REQUEST_CANCELLED` (for matched donors)
  - `EMERGENCY_REQUEST_CREATED` (for high-urgency notifications)
- **Delivery Status Semantics**: Fully audited delivery lifecycle supporting `PENDING`, `SENT`, `FAILED`, and `NO_DEVICES`. If a recipient has no registered active devices, the notification is explicitly marked `NO_DEVICES` (never marked `SENT`).
- **Device Token Management**: Authenticated registration and revocation of mobile push tokens (`/api/v1/devices/tokens`, `/api/v1/devices/tokens/{id}`).
  - Server-controlled provider: strictly enforces `provider = 'FCM'` via Bean Validation and PostgreSQL database check constraint `chk_device_tokens_provider`.
  - Privacy safeguards: Raw tokens are stored securely, masked in operational logs (`***`), and never exposed through public API responses.
- **Decoupled Asynchronous Execution**: Push delivery runs via a dedicated bounded named thread pool executor (`@Async("notificationTaskExecutor")`), completely isolated from core business transactions. Push network timeouts or failures never roll back domain transactions (`ACCEPTED`, `DECLINED`, `CANCELLED`, `EXPIRED`, `MATCH_CREATED`).
- **Pluggable Push Providers**:
  - `NOOP` (default for development and local testing): logs delivery simulations safely without third-party network dependencies (`netra.notifications.push.provider=noop`).
  - `FCM` (production Firebase Cloud Messaging HTTP v1): uses Google Firebase Admin SDK (`firebase-admin:9.2.0`). If credentials or project configuration are missing when FCM is active, fails explicitly and safely (never silently fakes delivery).
- **PostgreSQL V13 Migration**: Flyway migration `V13__create_notifications.sql` provisions `notifications` and `user_device_tokens` tables with UUID primary keys, idempotency uniqueness, and check constraints.

### 12. Verified Donation V1
- **Authoritative Clinical Verification**: Donor match acceptance (`ACCEPTED`) records initial donor consent only. Verified Donation V1 is the sole authoritative source for actual donation completion, clinical verification, donor recovery cooldown tracking, and request/event attribution.
- **Controlled State Machine**:
  - `PENDING_VERIFICATION`: Initial claim submitted by donor, awaiting clinical confirmation.
  - `VERIFIED`: Authoritatively verified by authorized staff (`ROLE_BLOODBANK`, `ROLE_ORGANIZATION`, or `ROLE_ADMIN`). Triggers authoritative `DonorProfile.lastDonationDate` update and NBTC cooldown reset.
  - `REJECTED`: Clinical staff rejects claim with mandatory reason (e.g. donor no-show or screening deferral). `lastDonationDate` remains unchanged.
  - `CANCELLED`: Donor cancels an unverified pending claim.
- **Multi-Source Support**:
  - `BLOOD_REQUEST`: Claims linked to verified fulfilled patient requests; strictly requires an existing `ACCEPTED` `DonorMatch`.
  - `DONATION_EVENT`: Claims linked to verified blood drives/camps; strictly requires a valid `DonationEventRegistration`.
- **Granular Verification RBAC**:
  - `ROLE_BLOODBANK`: Authorized only for donations linked to their active blood bank facility or blood bank-sponsored events.
  - `ROLE_ORGANIZATION`: Authorized only for events created by the organization.
  - `ROLE_ADMIN`: Platform-wide verification and administrative correction authority (`/api/v1/donations/{id}/admin-correction`).
  - Regular donors/receivers are strictly prohibited from verifying or approving donations.
- **Optimistic Concurrency & Unique Constraints**: Database-level unique constraint preventing duplicate claims per request/event per donor; `@Version` concurrency control preventing conflicting simultaneous verifications.
- **Domain Event & Notification Integration**: Publishes `DonationSubmittedEvent`, `DonationVerifiedEvent`, and `DonationRejectedEvent`, automatically delivering in-app notifications and push alerts to donors.
- **PostgreSQL V14 Migration**: Flyway migration `V14__create_donations.sql` creates `donations` table, check constraints (`chk_donations_source_type`, `chk_donations_verification_status`, `chk_donations_source_references`), unique constraints, and alters `chk_notifications_type`.
- **Flutter UI & Components**: `DonationHistoryScreen` for tracking past and pending claims, `ClaimDonationScreen` with source selector and date constraints, `DonationStatusBadge` presentation chip, and `DonationCard` with status badges and cancellation actions.

---

## Security & Architecture Principles

1. **Server Authority**: The server deterministically computes eligibility, compatibility, ranking, and authorization. Client-supplied status claims are rejected.
2. **Data Minimization**: Donor coordinates and medical screening answers are never exposed through public discovery or matching APIs.
3. **Defense in Depth**: Database-level hard filtering, service-level business validation, and Jakarta Bean Validation on DTO inputs.
4. **Rate Limiting**: Fixed-window rate limiting with deterministic Clock testing and bounded in-memory cleanup:
   - Eligibility: 20 requests/minute
   - Emergency: 5 new requests per fixed 10-minute window per authenticated user (with counter rollback on downstream failure)
   - Matching Discovery: 30 requests/minute per authenticated user (with bounded in-memory key cleanup)
   - Match Creation: 20 requests/minute per authenticated user

---

## Project Structure

```
Netra/
├── backend/
│   ├── pom.xml
│   ├── mvnw.cmd / mvnw
│   ├── src/main/java/org/netra/
│   │   ├── NetraApplication.java
│   │   ├── core/
│   │   │   ├── audit/          (Security audit logging, event publisher)
│   │   │   ├── config/         (SecurityConfig, PasswordConfig, RateLimitingConfig)
│   │   │   ├── exception/      (GlobalExceptionHandler, domain exceptions)
│   │   │   ├── ratelimit/      (RateLimitingService, fixed-window counters, bounded in-memory cleanup)
│   │   │   └── security/       (JwtTokenProvider, JwtAuthenticationFilter, SecurityUtils)
│   │   └── features/
│   │       ├── auth/           (Authentication, login, register, token refresh)
│   │       ├── bloodbank/      (Blood banks, inventory components, concurrency control)
│   │       ├── bloodrequest/   (Blood requests, lifecycle, BOLA authorization)
│   │       ├── donation/       (Verified donation lifecycle, clinical verification, donor claims, cooldowns)
│   │       ├── donor/          (Donor profiles, availability, coordinates, coordinate validation)
│   │       ├── eligibility/    (INDIA-NBTC-2026-01 rule engine, 6-step self-screening)
│   │       ├── emergency/      (Emergency Mode V1, idempotency records, rate limit rollback)
│   │       ├── events/         (Donation events, drives, participant registration)
│   │       ├── matching/       (Donor Matching candidate engine, Donor Response persistent lifecycle, compatibility matrix)
│   │       ├── notification/   (In-app notifications, FCM & NOOP push dispatch, async executor, device tokens)
│   │       └── user/           (User accounts, roles, profile management)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/       (Flyway V1 - V14 migrations)
│   └── src/test/java/org/netra/
│       ├── core/               (Security, audit, and rate-limiting tests)
│       └── features/           (Feature-specific integration, security, and rule tests)
├── frontend/
│   ├── pubspec.yaml
│   └── lib/
│       ├── main.dart
│       ├── core/               (Theme, API clients, shared widgets, disclaimers)
│       └── features/
│           ├── auth/           (Login, registration, token storage)
│           ├── blood_request/  (Blood request creation, list, details)
│           ├── bloodbank/      (Blood bank discovery, inventory views)
│           ├── donation/       (Donation history, claim submission, status badges, donation cards)
│           ├── donor/          (Donor profile setup, status toggles)
│           ├── donor_response/ (Incoming matches, detail screen, accept/decline flows, requester match lists)
│           ├── eligibility/    (6-step self-check flow, deferral calculator)
│           ├── emergency/      (Emergency request creation, confirmation)
│           ├── events/         (Event schedules, drive registration)
│           ├── home/           (Dashboard, navigation, quick actions)
│           ├── matching/       (Donor matches screen, candidate cards, radius filters)
│           ├── notification/   (Notification bell badge, in-app feed, push handling, read tracking)
│           └── profile/        (User profile, settings)
└── docs/
    ├── SECURITY_THREAT_MODEL.md
    ├── DATA_FLOW_DIAGRAM.md
    ├── UX_FLOW_DIAGRAM.md
    └── API_DOCUMENTATION.md
```

---

## Running the Backend & Tests

### Execute Automated Test Suite
Run the backend test suite with:
```powershell
cd backend
.\mvnw.cmd test
```

### Start the Spring Boot Backend Server
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
The backend API server starts at `http://localhost:8080`.
