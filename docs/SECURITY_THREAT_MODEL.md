# NETRA Blood Donation Platform: Security & Privacy Threat Model

## 1. Executive Summary & Clinical Scope

NETRA is a healthcare-adjacent blood donation platform connecting voluntary donors with licensed blood centres and donation camps. The **"Check Donation Eligibility"** feature acts as a confidential, preliminary pre-screening assistant.

**Primary Safety Invariant**:
> **Pre-Screening Result Only**: Final eligibility is determined exclusively by the blood bank or qualified medical staff after physical examination (temperature, pulse, blood pressure) and mandatory laboratory tests (hemoglobin, infectious disease markers: HIV, Hepatitis B & C, Syphilis, Malaria). The system never claims to medically certify or approve donors.

---

## 2. STRIDE Threat Analysis Matrix

| Threat Category | Potential Attack / Vulnerability Vector | Severity | NETRA Defense & Architectural Mitigation |
| :--- | :--- | :--- | :--- |
| **Spoofing (Identity)** | Attacker spoofs donor identity or injects forged `user_id` in eligibility session requests. | **CRITICAL** | **Context-Derived Identity**: User ID is never trusted from request payloads. Identity is derived strictly from the cryptographically verified JWT in `SecurityContextHolder`. Anonymous sessions are separated into isolated transient session records. |
| **Tampering (Integrity)** | Client submits `{"result": "LIKELY_ELIGIBLE"}` or alters rule version parameter to force clearance. | **CRITICAL** | **Server Source of Truth**: The client DTO (`CheckEligibilityRequest`) cannot submit results or rule versions. The server deterministically executes the versioned `EligibilityRuleEngine` on stored responses. |
| **Repudiation** | User denies undertaking pre-screening or authorized admin alters clinical rules without audit trail. | **HIGH** | **Privacy-Preserving Audit Trail**: Audit events (`SESSION_STARTED`, `SESSION_COMPLETED`, `RULES_EVALUATED`, `RULE_VERSION_CHANGED`) recorded in `eligibility_audit_logs` with SHA-256 IP hashes and timestamps. |
| **Information Disclosure** | Sensitive health answers leaked via logs, URLs, error messages, push notifications, or public DTOs. | **CRITICAL** | **Data Minimization & Segregation**: Health answers are segregated from profile data in `eligibility_answers`. Responses return safe public DTOs (`EligibilityResultResponse`). Zero health answers in server logs, URLs, or exception messages. |
| **Denial of Service** | Automated bot hammers eligibility evaluation endpoint to probe internal logic or exhaust server resources. | **HIGH** | **Token-Bucket Rate Limiting**: Per-IP and per-user sliding window rate limiting (20 requests/minute). Request size bounds and timeout limits enforced at filter layer. |
| **Elevation of Privilege / IDOR (BOLA)** | Donor A manipulates `sessionId` in REST URL to view or alter Donor B's pre-screening questionnaire answers. | **CRITICAL** | **Multi-Tenant Ownership Verification**: `validateSessionAccess(sessionId)` verifies that `session.getUserId()` strictly matches the authenticated principal. Unauthorized requests immediately fail with HTTP 403 Forbidden. |

---

## 3. Data Minimization & Privacy Protection Rules

1. **No Sensitive Health Data in Application Logs**:
   Logs record session lifecycle events (`sessionId`, `ruleVersion`, `resultType`). Raw questionnaire answers (e.g., specific illnesses, surgery status, pregnancy) are **never** logged to stdout, files, or cloud monitoring.
2. **Hashed Network Metadata**:
   IP addresses are processed through a SHA-256 hash before storage in audit logs to enable abuse mitigation while preventing geographic donor tracking.
3. **Session Expiration & Retention**:
   Eligibility sessions automatically expire after 60 minutes (`expires_at`). Expired sessions cannot be updated or evaluated.
4. **Third-Party Isolation**:
   No healthcare answers are sent to analytics platforms, crashlytics, or push notification payloads. Push reminders only contain schedule times (e.g. "You can check your blood donation eligibility today"), never clinical deferral codes.
