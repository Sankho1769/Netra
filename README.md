# NETRA: Blood Donation Platform

NETRA is a production-grade digital blood donation ecosystem engineered with zero-trust healthcare data safeguards, versioned clinical pre-screening, and a responsive Flutter mobile-first architecture.

---

## Feature: Blood Donation Eligibility Self-Check

The **"Check Donation Eligibility"** feature enables prospective donors to complete a confidential, progressive 6-step pre-screening questionnaire and receive an authoritative preliminary determination.

### Core Medical Safety Tenet
> **Pre-Screening Result Only**: Final eligibility is determined strictly by the blood bank or qualified medical staff after physical examination (temperature, pulse, blood pressure) and mandatory laboratory screening (hemoglobin, infectious markers: HIV, Hepatitis B/C, Syphilis, Malaria). This feature acts as a preliminary screening assistant and **never** claims to medically certify or clear a donor.

---

## 1. Clinical Rule Engine (`INDIA-NBTC-2026-01`)

The server-side rule engine implements statutory donor-selection standards modeled on the **National Blood Transfusion Council (NBTC) India / Drugs and Cosmetics Rules (Schedule F, Part XII-B)**:

| Category | Evaluation Criteria | Official Standard / Rule Outcome |
| :--- | :--- | :--- |
| **Age** | Minimum & Maximum limits | **18 to 65 years** (`TEMPORARY_DEFERRAL` if outside) |
| **Weight** | Minimum whole-blood threshold | **$\ge$ 45 kg** for 350 ml whole-blood (`TEMPORARY_DEFERRAL` if $< 45$ kg) |
| **Recovery Interval** | Male vs. Female donor interval | **90 days** for males; **120 days** for females (`TEMPORARY_DEFERRAL` with calculated next date) |
| **Recent Illness** | Fever or viral symptoms | **14-day symptom-free deferral** post recovery |
| **Active Medications**| Antibiotics / Blood thinners | `MEDICAL_REVIEW_REQUIRED` (Clinical evaluation by medical officer) |
| **Procedures** | Tattoos, piercings, acupuncture | **6-month deferral** |
| **Surgeries** | Major / Minor surgical interventions| **12-month deferral** (major) / **6-month** (minor) |
| **Dental Surgery** | Tooth extraction / oral surgery | **72-hour deferral** |
| **Chronic Conditions**| Cardiac, epilepsy, bleeding disorders | `MEDICAL_REVIEW_REQUIRED` (Specialist clearance needed) |
| **Day-of Readiness** | Sleep & meal self-check | Sleep $\ge 4$h, Meal within 4h (`INSUFFICIENT_INFORMATION` / preparation advisory) |

---

## 2. Exactly Four High-Level Results

1. **`LIKELY_ELIGIBLE`**:
   - Status text: *"Based on your answers, you appear eligible for donation."*
   - Mandatory disclaimer: *"Final eligibility will be confirmed by the blood bank after their screening and required tests."*
   - Actions: **[ Find Nearby Blood Banks ]**, **[ Find Donation Events ]**, **[ Register for Donation ]**.
2. **`TEMPORARY_DEFERRAL`**:
   - Status text: *"You may need to wait before donating."*
   - Displays reason, estimated next eligible date (safely calculated), and recommended next steps.
   - Actions: **[ Set Reminder ]**, **[ Find Blood Banks ]**.
3. **`MEDICAL_REVIEW_REQUIRED`**:
   - Status text: *"We can't determine your eligibility from the app alone. Please speak with the blood bank or qualified medical staff before donating."*
   - Non-diagnostic clinical guidance.
4. **`INSUFFICIENT_INFORMATION`**:
   - Explicitly displays what information or pre-donation readiness steps are needed.

---

## 3. Security & Zero-Trust Architecture

NETRA adheres to the 10 Critical Engineering Rules:
1. **Server Authority**: The client cannot override server decisions. Requests containing forged results (e.g. `{"result": "LIKELY_ELIGIBLE"}`) are discarded; the server deterministically calculates the result.
2. **Authorization & IDOR Defense**: User identity is derived strictly from `SecurityContextHolder`. Multi-tenant ownership checks prevent any donor from modifying or accessing another donor's session.
3. **Data Minimization & Health Privacy**:
   - Questionnaire answers are stored separately from user profile data.
   - Zero health answers in application logs, error messages, push notifications, or public DTOs.
   - IP addresses are hashed using SHA-256 in audit logs.
4. **Rate Limiting**: Sliding window token-bucket rate limiter prevents repeated hammering and rule-probing.
5. **Session Expiry**: Sessions automatically expire after 60 minutes.

---

## 4. Project Structure

```
Netra/
├── backend/
│   ├── pom.xml
│   ├── mvnw.cmd / mvnw
│   ├── src/main/java/org/netra/
│   │   ├── config/ (SecurityConfig, RateLimiting)
│   │   ├── controller/ (EligibilityController)
│   │   ├── dto/ (Safe public request & response DTOs)
│   │   ├── entity/ (Sessions, Answers, Questions, Rules, AuditLog)
│   │   ├── exception/ (GlobalExceptionHandler, sanitizing errors)
│   │   ├── repository/ (Spring Data JPA Repositories)
│   │   ├── security/ (JwtTokenProvider, SecurityUtils)
│   │   └── service/ (EligibilityRuleEngine, EligibilityService, AuditService)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/ (V1 Schema, V2 Seed Rules)
│   └── src/test/java/org/netra/
│       ├── controller/EligibilityControllerIntegrationTest.java
│       └── engine/EligibilityRuleEngineTest.java
├── frontend/
│   ├── pubspec.yaml
│   └── lib/
│       ├── main.dart
│       ├── core/ (Theme, API Client, Disclaimer Banner, Progress Bar)
│       └── features/
│           ├── eligibility/ (Intro, Flow Stepper, 6 Steps, Result Screen)
│           ├── home/ (HomeScreen with both required entry points)
│           └── location/ (NearbyBloodBanksScreen with approximate discovery)
└── docs/
    ├── SECURITY_THREAT_MODEL.md
    ├── DATA_FLOW_DIAGRAM.md
    ├── UX_FLOW_DIAGRAM.md
    └── API_DOCUMENTATION.md
```

---

## 5. Running the Backend & Tests

### Execute Automated Test Suite (26 Tests)
```powershell
cd backend
.\mvnw.cmd test
```

### Start the Spring Boot Backend Server
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
The API is available at `http://localhost:8080/api/v1/eligibility`.
