# NETRA Architecture Specification (`aechiect.md`)

## 1. System Overview & Technology Stack

The NETRA system follows a decoupled, mobile-first client-server architecture:

```
+--------------------------------------------------------+
|                  Flutter Mobile Client                 |
|  - Material 3 Design System                            |
|  - Feature-First Architecture                          |
|  - AuthScope / State Management                        |
+--------------------------------------------------------+
                           |
                     HTTPS / JSON REST API
                           |
+--------------------------------------------------------+
|               Spring Boot 3.x Backend                  |
|  - Spring Security (Stateless JWT Filter)              |
|  - Role-Based Access Control (DONOR, BLOOD_BANK, ADMIN)|
|  - Feature Modules (Matching, Blood Request, Events)   |
|  - Jakarta Persistence / Spring Data JPA               |
+--------------------------------------------------------+
                           |
                           v
+--------------------------------------------------------+
|          Relational Database (H2 / MySQL)              |
|  - Flyway Database Migrations                          |
|  - Unique Constraints & Foreign Key Integrity          |
|  - Spatial Coordinates & Audit Logs                    |
+--------------------------------------------------------+
```

---

## 2. Core Modules Decomposition

### 2.1 Backend Modules (`org.netra.features.*`)
1. **`matching`**:
   - `DonorMatchingController`: Public & requester matching APIs (`/api/v1/blood-requests/{id}/raise-hand`, `/matches/{id}/accept`, etc.).
   - `DonorResponseController`: Donor self-service APIs (`/api/v1/donor/matches/**`).
   - `DonorResponseService`: State machine execution, mutual acceptance rules, contact disclosure authorization.
   - `DonorMatch`: JPA entity mapping donor offer lifecycle.
   - `MatchContactDto`: DTO delivering bilateral contact details exclusively upon mutual acceptance.
2. **`bloodrequest`**:
   - `BloodRequestService`: Request creation, hospital validation, units remaining, and fulfillment auto-closure.
   - `BloodRequest`: JPA entity with status (`OPEN`, `MATCHING`, `FULFILLED`, `CANCELLED`).
3. **`events`**:
   - `DonationEventService`: NBTC-compliant blood donation camp scheduling, restricted to Blood Banks and Administrators.
4. **`auth` & `security`**:
   - `JwtTokenProvider` & `JwtAuthenticationFilter`: Token validation and SecurityContextHolder population.
   - `User`: Entity storing hashed credentials (BCrypt), roles (`DONOR`, `BLOOD_BANK`, `ADMIN`), and verified phone number.

### 2.2 Frontend Modules (`lib/features/*`)
1. **`blood_request`**:
   - `BloodRequest`: Model with `unitsFulfilled`, `helperCount`, and `isFullyFulfilled`.
   - `BloodRequestDetailsScreen`: Dynamic view displaying Owner actions (Helpers count & review) vs Donor actions (Raise Hand button & offer status).
   - `BloodRequestCard`: Discoverable feed card with fulfillment pill and helper offer counter.
2. **`donor_response`**:
   - `DonorResponseApiService`: Client service handling `raiseHand`, `acceptHelper`, `declineHelper`, and `getMatchContact`.
   - `RequesterMatchListScreen`: Filterable queue of helper responses with immediate Accept/Decline and View Contact actions.
   - `RequesterMatchDetailScreen`: Safe donor profile and acceptance coordination.
   - `DonorMatchDetailScreen`: Donor's view of match status with View Requester Contact button upon acceptance.
3. **`events`**:
   - `DonationEventListScreen`: Checks user role and displays NBTC guidance dialog for standard donors.
   - `CreateDonationEventScreen`: Pre-flight check preventing deceptive 403 errors.

---

## 3. Donor Match State Machine

```
                   [ Donor Presses 'Raise Hand' ]
                                  |
                                  v
                             +----------+
                             | MATCHED  | (Pending Requester Review)
                             +----------+
                              /        \
         Requester Accepts   /          \   Requester Declines / Donor Cancels
                            v            v
                     +----------+    +----------+
                     | ACCEPTED |    | DECLINED | / CANCELLED
                     +----------+    +----------+
                          |
              +-----------+-----------+
              |                       |
              v                       v
     [ View Contact &           Donor Arrives at
       Coordinate ]             Hospital Blood Bank
                                      |
                                      v
                                +----------+
                                | ARRIVED  |
                                +----------+
                                      |
                           Medically Screened & Collected
                                      |
                                      v
                                +----------+
                                |FULFILLED |
                                +----------+
                                      |
                 (unitsFulfilled >= unitsRequired closes Request)
```

---

## 4. API Endpoints Specification

| Method | Path | Auth Required | Authorized Roles | Description |
|---|---|---|---|---|
| `POST` | `/api/v1/blood-requests/{id}/raise-hand` | Yes | DONOR, ADMIN | Offer assistance for an open blood request |
| `POST` | `/api/v1/blood-requests/{id}/matches/{matchId}/accept` | Yes | Requester, ADMIN | Accept helper offer and authorize contact sharing |
| `POST` | `/api/v1/blood-requests/{id}/matches/{matchId}/decline` | Yes | Requester, ADMIN | Decline helper offer |
| `GET` | `/api/v1/donor/matches/{matchId}/contact` | Yes | Requester, Donor, ADMIN | Retrieve coordination phone & hospital details (ACCEPTED only) |
| `DELETE`| `/api/v1/donor/matches/{matchId}` | Yes | Donor, ADMIN | Withdraw helper offer |
| `POST` | `/api/v1/donation-events` | Yes | BLOOD_BANK, ADMIN | Create NBTC-regulated blood donation camp |

---

## 5. Security Architecture & Invariants

1. **Information Barrier**:
   - Blood request and match list queries explicitly strip out donor and requester phone numbers, physical addresses, and email addresses.
2. **Authoritative Contact Boundary**:
   - Phone numbers are exclusively mapped through `DonorResponseService.getMatchContact()`.
   - The query verifies `match.getResponseStatus() == DonorMatchStatus.ACCEPTED || match.getResponseStatus() == DonorMatchStatus.ARRIVED`.
   - The query asserts `currentUserId.equals(donorUserId) || currentUserId.equals(requesterUserId) || isAdmin`. Violation yields an immediate HTTP 403 Forbidden.
3. **Double-Commitment & Race Condition Protection**:
   - Backed by unique database constraint `uq_donor_matches_request_donor (blood_request_id, donor_id)`.
   - Service layer locks on request row and verifies `unitsFulfilled < unitsRequired` before accepting offers.
