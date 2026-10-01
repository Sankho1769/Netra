# Product Requirements Document (PRD) — NETRA Blood Donation Network

## 1. Executive Summary & Vision

**NETRA** is an authoritative, clinical-grade blood donation and emergency blood requirement network built strictly in compliance with National Blood Transfusion Council (NBTC) regulations in India. 

The primary objective is to connect verified blood requesters with voluntary, altruistic blood donors in real-time while strictly guarding patient and donor privacy, preventing fraud and commercial exploitation, and ensuring complete compliance with clinical safety guidelines.

---

## 2. Core User Personas

| Persona | Role | Key Objectives | Privacy & Security Needs |
|---|---|---|---|
| **Requester (Patient Attendant / Family)** | Creates blood requests on behalf of patients admitted to certified hospitals | Quickly find voluntary donors; review incoming offers; coordinate direct hospital arrivals | Sensitive patient data (diagnosis, exact ward) protected; verified contact exchanged only with confirmed donors |
| **Voluntary Donor** | Discovers open blood requests nearby, offers help ("Raise Hand"), donates at hospital | Discover urgent local requirements matching blood type; receive clear directions; track karma | Phone number & exact address NEVER exposed publicly; contact shared only after requester accepts offer |
| **Hospital / Blood Bank Staff** | Verifies hospital admissions, conducts mandatory laboratory crossmatching & donation fulfillment | Verify patient admission; conduct pre-donation hemoglobin & viral markers tests; record fulfilled units | Authoritative validation of fulfilled units; regulatory audit trail |
| **Blood Bank / Platform Administrator** | Oversees platform integrity, organizes regulatory blood donation camps | Publish compliant donation camps; review fraud flags; maintain system integrity | Full administrative oversight under NBTC camp hosting rules |

---

## 3. Product Features & Detailed Workflows

### 3.1 Feature 1: Blood Request Lifecycle
- **Creation & Geolocation**: Requester selects certified hospital with verified coordinates, specifies blood group, units required (1–10), urgency window (within 2h, 6h, 12h, 24h, 48h), and optional hospital contact info.
- **Verification**: Requests undergo server-authoritative hospital verification. Once confirmed, status transitions to `OPEN`.
- **Fulfillment Tracking**: Requests maintain `unitsRequired`, `unitsFulfilled`, and dynamically computed `unitsRemaining`.
- **Automatic Closure**: When `unitsFulfilled >= unitsRequired`, request status automatically transitions to `FULFILLED` and ceases accepting new donor offers.

### 3.2 Feature 2: Donor "Raise Hand" / "Offer Help" Flow
- **Discovery**: Eligible donors view open requests filtered by blood compatibility and proximity.
- **Pre-flight Checks**:
  1. Donor must be authenticated and verified.
  2. Donor cannot raise hand on their own blood request (Self-requesting prohibited).
  3. Donor must meet NBTC donation deferral criteria (e.g., minimum 90-day interval for males, 120 days for females).
  4. Request must be in `OPEN` status with `unitsRemaining > 0`.
  5. Duplicate prevention: DB-level unique constraint (`blood_request_id`, `donor_id`) prevents multiple pending offers.
- **Commitment Action**: Donor reviews confirmation modal outlining hospital location and voluntary commitment, then confirms "Raise Hand".
- **State Transition**: A `DonorMatch` record is created in `MATCHED` status.

### 3.3 Feature 3: Requester Review & Acceptance Flow
- **Helper Offer Queue**: Requester views real-time incoming offers with privacy-safe donor summaries:
  - Donor display name & compatibility badge
  - Approximate distance (km) without revealing donor's home address
  - Verified donor badge
  - Responded timestamp
- **Action Options**:
  - `[ Accept Helper ]`: Requester accepts the donor. Match transitions to `ACCEPTED`.
  - `[ Decline ]`: Requester declines offer. Match transitions to `DECLINED`. Request remains open for other donors.

### 3.4 Feature 4: Verified Contact Sharing Mechanism
- **Anti-Doxxing Invariant**: Phone numbers, emails, and direct communication channels are **NEVER** returned in public request listings, match lists, or search APIs.
- **Authorized Contact Disclosure**:
  - Endpoint: `GET /api/v1/donor/matches/{matchId}/contact`
  - Access is restricted strictly to:
    1. The authenticated Requester who created the request.
    2. The authenticated Donor whose offer was accepted.
    3. Platform Administrators.
  - Pre-condition: Match must be in `ACCEPTED` or `ARRIVED` status. Any call in `MATCHED`, `DECLINED`, `EXPIRED`, or `CANCELLED` status returns HTTP 400 Bad Request.
  - Third-party callers (User C or unauthenticated users) receive HTTP 403 Forbidden.

### 3.5 Feature 5: Donation Fulfillment & Hospital Verification
- Upon arrival at the hospital blood bank:
  1. Blood bank performs mandatory clinical pre-screening, hemoglobin test, and crossmatching.
  2. If medically cleared, blood collection is completed.
  3. Fulfillments increment `unitsFulfilled`. When `unitsFulfilled == unitsRequired`, the blood request automatically marks `FULFILLED`.

### 3.6 Feature 6: Donation Camp Regulatory Access (NBTC Compliance)
- Under Indian National Blood Transfusion Council (NBTC) regulations, blood donation camps may **only** be organized and hosted by licensed Blood Banks and accredited healthcare institutions.
- Normal voluntary donors attempting to access the "Host Donation Camp" feature receive an informative, respectful clinical policy modal explaining NBTC standards, instead of a confusing 403 error.
- Verified Blood Banks and Admins retain authorization to organize, publish, and manage community donation camps.

---

## 4. Non-Functional Requirements (NFRs)

1. **Security & Privacy**: Zero plaintext PII leakage. Sensitive coordinates truncated for approximate distance calculations.
2. **Server-Side Authority**: All validation, role checks, and state transitions are executed and enforced on the Spring Boot backend. Client UI serves strictly as a presentation layer.
3. **Auditability**: All state transitions (`MATCHED`, `ACCEPTED`, `DECLINED`, `CANCELLED`, `ARRIVED`, `FULFILLED`) are persisted with timestamps and user identifiers.
4. **Performance**: Matching lookups and contact resolution execute under 200ms with indexed foreign keys and database constraints.
