# NETRA Task Tracker & Implementation Plan (`task.md`)

## 1. Feature: Blood Request -> Donor "Raise Hand" -> Acceptance -> Contact -> Fulfillment

- [x] **Backend Domain & Persistence Model**
  - [x] Create `MatchContactDto.java` for secure mutual contact disclosure.
  - [x] Update `DonorMatchRepository.java` with helper counting and request match lookup queries.
  - [x] Implement `raiseHand(requestId, currentUserId)` in `DonorResponseService.java`.
  - [x] Implement `acceptMatchByRequester(requestId, matchId, currentUserId)` with remaining units check.
  - [x] Implement `declineMatchByRequester(requestId, matchId, currentUserId)`.
  - [x] Implement `getMatchContact(matchId, currentUserId)` with 401/403/400 security boundaries.
  - [x] Expose REST endpoints in `DonorMatchingController` and `DonorResponseController`.
  - [x] Add `unitsFulfilled` and `helperCount` to `BloodRequestSummaryDto` and `BloodRequestPublicDetailDto`.
  - [x] Auto-close request to `FULFILLED` when `unitsFulfilled >= unitsRequired`.

- [x] **Frontend Architecture & Screens**
  - [x] Create `MatchContactInfo` model in `donor_match_response_model.dart`.
  - [x] Extend `BloodRequest` model with `unitsFulfilled`, `helperCount`, and `isFullyFulfilled`.
  - [x] Implement API calls in `DonorResponseApiService` (`raiseHand`, `acceptHelper`, `declineHelper`, `getMatchContact`).
  - [x] Update `BloodRequestCard` with fulfillment pill, helper counter, and action strip.
  - [x] Update `BloodRequestDetailsScreen` with dynamic Owner vs Donor views, Raise Hand action, and Contact modal.
  - [x] Update `RequesterMatchListScreen` with Accept/Decline action buttons and View Contact bottom sheet.
  - [x] Update `RequesterMatchDetailScreen` with Accept/Decline action buttons and View Contact sheet.
  - [x] Update `DonorMatchDetailScreen` with accepted state banner and View Requester Contact button.
  - [x] Update `DonationEventListScreen` and `CreateDonationEventScreen` with NBTC clinical guidance modal.

- [x] **Comprehensive Documentation**
  - [x] `docs/PRD.md`
  - [x] `docs/aechiect.md`
  - [x] `docs/rules.md`
  - [x] `docs/design.md`
  - [x] `docs/memory.md`
  - [x] `docs/task.md`

- [x] **Automated Regression Testing & Verification**
  - [x] Backend automated test suite verifying raise hand, duplicate prevention, contact disclosure security, and auto-fulfillment closure (571 tests passed).
  - [x] Frontend widget test suite verifying Raise Hand UI, Accept/Decline flow, and NBTC dialog (131 tests passed).
  - [x] Clean build verification across Spring Boot (`mvnw clean test`), Flutter (`flutter analyze`, `flutter test`), and Android APK build (`flutter build apk --debug`).
  - [x] Git commit: `feat(requests): implement donor offer acceptance contact sharing and fulfillment flow`.


