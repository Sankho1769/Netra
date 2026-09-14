# NETRA Blood Donation Platform: Eligibility API Specifications

Base Path: `/api/v1/eligibility`

---

### 1. Retrieve Active Rule Engine Version
- **Endpoint**: `GET /api/v1/eligibility/rules/version`
- **Description**: Returns the active rule engine version, effective date, and statutory authority.
- **Auth**: Public
- **Response** (`200 OK`):
```json
{
  "activeVersion": "INDIA-NBTC-2026-01",
  "effectiveDate": "2026-01-01T00:00:00Z",
  "policySource": "National Blood Transfusion Council (NBTC) Guidelines & Schedule F Part XII-B",
  "authority": "Government of India / National Blood Transfusion Council"
}
```

---

### 2. Retrieve Configured Questionnaire
- **Endpoint**: `GET /api/v1/eligibility/questions`
- **Description**: Retrieves questions configured for the active rule version, grouped into 5 progressive steps.
- **Auth**: Public / Authenticated
- **Response** (`200 OK`):
```json
{
  "ruleVersion": "INDIA-NBTC-2026-01",
  "totalSteps": 6,
  "sections": [
    {
      "stepNumber": 1,
      "stepTitle": "Basic Information",
      "stepSubtitle": "Age, weight, and biological criteria",
      "questions": [
        {
          "questionKey": "AGE",
          "stepNumber": 1,
          "category": "BASIC",
          "text": "How old are you?",
          "helpText": "Donors must be between 18 and 65 years old per national blood transfusion guidelines.",
          "type": "NUMBER",
          "options": [],
          "validation": "{\"min\": 10, \"max\": 120, \"required\": true}"
        }
      ]
    }
  ]
}
```

---

### 3. Create Pre-Screening Session
- **Endpoint**: `POST /api/v1/eligibility/sessions`
- **Description**: Initializes an eligibility session pinned to the active rule version with a 60-minute time-to-live.
- **Auth**: Optional/Bearer JWT. User ID is resolved from SecurityContext.
- **Request Body**:
```json
{
  "clientTimestamp": "2026-09-15T10:00:00Z"
}
```
- **Response** (`201 Created`):
```json
{
  "sessionId": "b47fa8c3-4d62-4217-91a5-8c09a8eb7122",
  "ruleVersion": "INDIA-NBTC-2026-01",
  "status": "IN_PROGRESS",
  "expiresAt": "2026-09-15T11:00:00Z"
}
```

---

### 4. Submit Screening Answers
- **Endpoint**: `POST /api/v1/eligibility/sessions/{sessionId}/answers`
- **Description**: Saves or updates questionnaire answers. Enforces session ownership (IDOR prevention).
- **Request Body**:
```json
{
  "answers": [
    { "questionKey": "AGE", "value": "26" },
    { "questionKey": "WEIGHT_KG", "value": "68" },
    { "questionKey": "BIOLOGICAL_SEX", "value": "MALE" },
    { "questionKey": "PREVIOUS_DONATION", "value": "true" },
    { "questionKey": "LAST_DONATION_DATE", "value": "2026-05-10" }
  ]
}
```
- **Response** (`200 OK`):
```json
{
  "status": "SUCCESS",
  "savedCount": 5
}
```

---

### 5. Evaluate Eligibility (Anti-Tampering)
- **Endpoint**: `POST /api/v1/eligibility/sessions/{sessionId}/check`
- **Description**: Triggers authoritative server-side rule engine calculation.
- **Request Body**:
```json
{
  "clientReviewConfirmed": true
}
```
*(Notice: Any client attempt to submit `"result": "LIKELY_ELIGIBLE"` is discarded).*
- **Response** (`200 OK`):
```json
{
  "sessionId": "b47fa8c3-4d62-4217-91a5-8c09a8eb7122",
  "ruleVersion": "INDIA-NBTC-2026-01",
  "result": "LIKELY_ELIGIBLE",
  "title": "You appear eligible to donate blood",
  "message": "Based on your answers, you meet the preliminary criteria for whole-blood donation.",
  "disclaimer": "Pre-screening result only. Final eligibility is determined by the blood bank/qualified medical staff after physical examination and required tests.",
  "estimatedNextEligibleDate": null,
  "deferralReasons": [],
  "missingFields": [],
  "nextActions": [
    { "actionType": "FIND_BLOOD_BANKS", "label": "Find Nearby Blood Banks", "route": "/blood-banks" },
    { "actionType": "FIND_EVENTS", "label": "Find Donation Events", "route": "/events" },
    { "actionType": "REGISTER_DONATION", "label": "Register for Donation", "route": "/register" }
  ]
}
```

---

### 6. Retrieve Session Result
- **Endpoint**: `GET /api/v1/eligibility/sessions/{sessionId}/result`
- **Description**: Retrieves public safe assessment result without leaking internal sensitive health answers.
- **Auth**: Caller must be session owner or anonymous session holder.
- **Response** (`200 OK`): Safe `EligibilityResultResponse`.
