# NETRA Clinical, Business, and Security Rules (`rules.md`)

## 1. Clinical Governance & Regulatory Rules

### 1.1 NBTC Compliance (India)
1. **Donation Intervals**:
   - Whole Blood (Male): Minimum 90 days (3 calendar months) between donations.
   - Whole Blood (Female): Minimum 120 days (4 calendar months) between donations.
   - Platelets (Apheresis): Minimum 14 days between donations; max 24 times/year.
2. **Deferral Evaluation**:
   - Medical conditions, recent surgeries, medications, and international travel trigger automated clinical deferral calculations.
3. **Crossmatching Invariant**:
   - The NETRA platform provides preliminary compatibility matching based on ABO/RhD blood groupings.
   - Preliminary matching **never** constitutes clinical clearance. Official blood grouping, crossmatching, screening for transfusion-transmitted infections (TTI: HIV, Hepatitis B/C, Syphilis, Malaria) must occur exclusively at the receiving hospital or licensed blood bank.
4. **Blood Donation Camp Hosting**:
   - Under National Blood Transfusion Council regulations, blood donation camps must be organized and conducted solely by licensed Blood Banks and accredited clinical facilities.
   - Individual donors or unauthorized entities are strictly restricted from publishing donation camps.

---

## 2. Business & Workflow Rules

### 2.1 Blood Request Rules
1. **Unit Bounds**: Minimum 1 unit, maximum 10 units per blood request.
2. **Status Progression**:
   - `OPEN`: Request is verified and discoverable.
   - `MATCHING`: Donor offers are actively being evaluated.
   - `FULFILLED`: `unitsFulfilled >= unitsRequired`. No new offers accepted.
   - `CANCELLED`: Requester voluntarily withdrew request before fulfillment.
3. **Self-Donation Prohibition**:
   - A user who created a blood request cannot offer to donate on their own blood request (`requesterUserId != donorUserId`). Attempts return HTTP 400 Bad Request.

### 2.2 Donor "Raise Hand" Rules
1. **Profile Active & Verified**: Donor must have a verified, non-suspended account.
2. **ABO Compatibility Check**: Donors must be ABO/RhD compatible with the patient (or universal donor O-).
3. **Concurrent Offer Limit**: A donor cannot have more than 3 active unfulfilled commitments concurrently to prevent over-promising.
4. **Duplicate Offer Prevention**: A donor can have at most one active offer per blood request. Duplicate attempts return HTTP 409 Conflict.

### 2.3 Mutual Acceptance Rules
1. **Authoritative Acceptance**: Only the verified creator of the request or an Admin can accept or decline incoming helper offers.
2. **Unit Overflow Guard**: If `unitsFulfilled >= unitsRequired`, acceptance of further offers is rejected with HTTP 400 Bad Request.

---

## 3. Privacy & Security Rules

### 3.1 Personal Identifiable Information (PII) Protection
1. **Zero Public Phone Numbers**:
   - Phone numbers are never returned in public request summaries, detail feeds, or match lists.
2. **Controlled Contact Disclosure**:
   - Contact numbers are provided solely via `GET /api/v1/donor/matches/{matchId}/contact`.
   - Both of the following conditions must be satisfied:
     a) Match status is strictly `ACCEPTED` or `ARRIVED`.
     b) Caller's authenticated ID matches either the donor or the requester (or platform admin).
   - Any access attempt by an unauthenticated user returns 401 Unauthorized.
   - Any access attempt by a third party (User C) returns 403 Forbidden.
   - Any access attempt before mutual acceptance returns 400 Bad Request.
3. **No Exact Home/Work Geocoding**:
   - Donors' home coordinates are never sent to the client. Distances are calculated server-side or using coarse city/district granularity.
