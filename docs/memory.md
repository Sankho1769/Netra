# NETRA Project Memory & Context Knowledge Base (`memory.md`)

## 1. System Invariants & Architectural Memory

1. **Server-Authoritative Security**:
   - The Flutter mobile client is never trusted for security or validation decisions.
   - All state transitions (`OPEN` -> `FULFILLED`, `MATCHED` -> `ACCEPTED`) are computed and audited on the Spring Boot backend.
2. **Contact Masking Invariant**:
   - Phone numbers and emails are masked across all listing endpoints.
   - Contact resolution occurs solely on `/api/v1/donor/matches/{matchId}/contact` after mutual acceptance is verified.
3. **Database Configuration**:
   - Persistent file-backed H2 (`jdbc:h2:file:./data/netradb`) is used in local and physical device development to ensure zero data loss across Spring Boot restarts.
   - Passwords are encrypted using BCrypt (`PasswordEncoder`).
4. **Physical Android Device Setup**:
   - Localhost (`127.0.0.1`) cannot be accessed from Android devices. ADB reverse proxy (`adb reverse tcp:8080 tcp:8080`) is used to map Android loopback calls to the development host.
   - Flutter executable is located at `C:\Users\SHUBHAM\flutter\bin\flutter.bat`.

---

## 2. Resolved Issues & Root Causes

1. **Password / Confirm Password Mismatch Bug**:
   - *Symptom*: Users typing identical passwords saw "Passwords do not match".
   - *Root Cause*: Asynchronous state lag between text controller listener and form field validation state.
   - *Fix*: Direct controller value inspection in `validator` callback with immediate re-evaluation on text change.
2. **403 Forbidden on "Host Camp"**:
   - *Symptom*: Standard donors tapping "Host Camp" encountered an uncaught 403 Forbidden error.
   - *Root Cause*: Server role requirement (`BLOOD_BANK` / `ADMIN`) lacked client-side context handling for NBTC regulatory policy.
   - *Fix*: Client-side role pre-flight check in `DonationEventListScreen` and `CreateDonationEventScreen` presenting an educational NBTC clinical policy modal.
3. **Blood Request -> Donor Raise Hand Product Gap**:
   - *Symptom*: Users could create blood requests, but donors had no direct mechanism to discover requests, raise hands, or exchange contact information upon acceptance.
   - *Fix*: Built end-to-end raise-hand, requester acceptance, mutual contact disclosure, and fulfillment tracking.
