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
- **Donor Profile Lifecycle**: Donor profiles (`/api/v1/donor/profile`) with blood group, verification status (`SELF_REPORTED` by default, `VERIFIED` upon clinical confirmation), availability status (`AVAILABLE`, `BUSY`, `UNAVAILABLE`, `PAUSED`), and geographic coordinates.
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
- **Rapid Emergency Broadcast**: Dedicated endpoint (`/api/v1/emergency/blood-requests`) for high-urgency blood requests with `CRITICAL` urgency.
- **Strict Rate Limiting**: Max 5 new requests per fixed 10-minute window per authenticated user, with counter rollback on downstream creation failures.
- **Idempotency Protection**: Idempotency-Key validation and request fingerprinting preventing duplicate emergency broadcasts.
- **Audited Cancellation**: Secure cancellation endpoint (`/api/v1/emergency/blood-requests/{id}/cancel`) with authorization-first validation order.

### 9. Donor Matching Engine V1
- **Decision-Support Matching**: On-demand matching endpoint (`GET /api/v1/blood-requests/{requestId}/matches`) finding compatible donors for open blood requests.
- **Blood Compatibility Matrix**: Centralized compatibility rule set for preliminary donor candidate matching (scoped strictly to Red Blood Cell and Whole Blood transfusions; decision-support only; final compatibility, screening, and crossmatching are determined by qualified blood-bank and clinical staff).
- **Verified-Only Donor Pool**: Hard server-side filtering ensuring only donors with `VERIFIED` blood group status are included; unverified and self-reported donors are strictly excluded.
- **Two-Stage Spatial Filtering**: Database-level bounding box hard filtering followed by exact spherical Haversine distance calculation within configurable search radii (10km, 25km, 50km, 100km).
- **Overdue Protection**: Rejects matching requests for overdue blood requests (`requiredBy <= now`).
- **Batch Eligibility Loading**: Single-query batch session retrieval to prevent N+1 queries.
- **Deterministic 3-Tier Ranking**:
  1. `EXACT` compatibility before `COMPATIBLE`
  2. Proximity ascending (`distanceKm`)
  3. Internal `donorProfileId` tie-breaker
- **Data Minimization & Privacy**: Masked donor display names (e.g. "John D."), distance in km, zero donor contact details, zero exact coordinates, and zero medical questionnaire answers in responses.
- **Bounded Rate Limiting**: 30 matching requests per minute per authenticated user with bounded in-memory key cleanup preventing memory accumulation.
- **Pure Read Operation**: Zero matching-side state mutations; no persistent `donor_matches` table.

---

## Security & Architecture Principles

1. **Server Authority**: The server deterministically computes eligibility, compatibility, ranking, and authorization. Client-supplied status claims are rejected.
2. **Data Minimization**: Donor coordinates and medical screening answers are never exposed through public discovery or matching APIs.
3. **Defense in Depth**: Database-level hard filtering, service-level business validation, and Jakarta Bean Validation on DTO inputs.
4. **Rate Limiting**: Fixed-window rate limiting with deterministic Clock testing and bounded in-memory cleanup:
   - Eligibility: 20 requests/minute
   - Emergency: 5 new requests per fixed 10-minute window per authenticated user (with counter rollback on downstream failure)
   - Matching: 30 requests/minute per authenticated user (with bounded in-memory key cleanup)

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
│   │       ├── donor/          (Donor profiles, availability, coordinates, coordinate validation)
│   │       ├── eligibility/    (INDIA-NBTC-2026-01 rule engine, 6-step self-screening)
│   │       ├── emergency/      (Emergency Mode V1, idempotency records, rate limit rollback)
│   │       ├── events/         (Donation events, drives, participant registration)
│   │       ├── matching/       (Donor Matching V1 engine, compatibility matrix, deterministic ranking)
│   │       └── user/           (User accounts, roles, profile management)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/       (Flyway V1 - V11 migrations)
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
│           ├── donor/          (Donor profile setup, status toggles)
│           ├── eligibility/    (6-step self-check flow, deferral calculator)
│           ├── emergency/      (Emergency request broadcast, confirmation)
│           ├── events/         (Event schedules, drive registration)
│           ├── home/           (Dashboard, navigation, quick actions)
│           ├── matching/       (Donor matches screen, candidate cards, radius filters)
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
