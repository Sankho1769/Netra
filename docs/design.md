# NETRA UI/UX Design System & Mobile Flow Specification (`design.md`)

## 1. Visual Design Principles & Color Tokens

NETRA adheres to clinical credibility, calmness under emergency stress, and zero deceptive states:

| Color Role | Hex Code | Visual Meaning | Usage |
|---|---|---|---|
| **Primary Brand Red** | `#DC2626` | Urgent, Vital, Blood | Primary buttons, urgency tags, key headers |
| **Success Clinical Green** | `#16A34A` | Verification, Accepted, Fulfilled | Acceptance badges, accepted status cards, copy feedback |
| **Advisory Amber** | `#D97706` / `#FEF3C7` | Pending, Caution, Advisory | Match pending review, clinical disclaimer backgrounds |
| **Trust Blue** | `#2563EB` / `#EFF6FF` | Privacy, Safeguards, Information | Ownership badges, privacy disclaimer banners |
| **Neutral Surface** | `#F9FAFB` | Background canvas | Screen background, card grouping |

---

## 2. Screen Specifications & Key Interactive Components

### 2.1 Discoverable Blood Request Card (`blood_request_card.dart`)
- **Header**: Blood Group Badge (red rounded tag) + Urgency Pill (Emergency / Urgent / Standard).
- **Fulfillment Progress Pill**:
  - Displays `${unitsFulfilled}/${unitsRequired} Fulfilled` in an emerald green container.
- **Helper Offers Counter**:
  - Displays `"${helperCount} offer(s)"` with volunteer activism icon when helpers have responded.
- **Action Strip**:
  - Non-owner view: Prominent `[ RAISE HAND / OFFER HELP ]` button.
  - Owner view: `[ View Helpers (${helperCount}) ]` button.

### 2.2 Blood Request Details Screen (`blood_request_details_screen.dart`)
- **Dual-State Dynamic Layout**:
  - **Requester / Owner Mode**:
    - Ownership verification card with blue verified checkmark.
    - Real-time helper banner: "X people have offered to help!".
    - `[ View Helpers (X) ]`: Opens `RequesterMatchListScreen` to accept/decline offers.
    - `[ Find Matching Donors ]`: Automated search for registered matching donors.
    - `[ Edit / Cancel Request ]`: Requester management controls.
  - **Donor / Helper Mode**:
    - When not yet offered: Prominent `[ RAISE HAND / OFFER HELP ]` button with commitment confirmation dialog.
    - When offer is pending (`MATCHED`): Amber card: "You Offered to Help! Waiting for requester review." with `[ Withdraw Help Offer ]`.
    - When offer is accepted (`ACCEPTED`): Green card: "Offer Accepted by Requester!" with `[ View Requester Contact & Coordinate ]`.
    - When request is fulfilled: Informative card: "This request is fully fulfilled. Thank you!".

### 2.3 Requester Match List & Detail Screens (`requester_match_list_screen.dart`)
- **Filter Chips**: All, Pending, Accepted, Declined, Expired.
- **Helper Response Card**:
  - Donor display name & compatibility badge.
  - Verified donor tag.
  - Approximate distance in km.
  - Action row for pending offers:
    - `[ Accept Helper ]` (Emerald green elevated button).
    - `[ Decline ]` (Red outline button).
  - Action row for accepted offers:
    - `[ View Contact & Coordinate ]` (Opens modal bottom sheet).

### 2.4 Secure Contact Bottom Sheet
- Renders authenticated coordination details:
  - Other party name & role avatar.
  - Verified phone number with copy-to-clipboard button.
  - Hospital name & verified address.
  - Clinical coordination instructions: "Contact the requester to coordinate arrival time and any donation requirements at the hospital blood bank."
  - Authorized privacy disclaimer.

### 2.5 Donation Camp Hosting Guard Modal (`donation_event_list_screen.dart`)
- For standard voluntary donors tapping "Host Blood Donation Camp":
  - Modal title: "Blood Donation Camp Guidelines (NBTC Policy)".
  - Educational explanation of NBTC regulations requiring licensed blood bank partnership.
  - Action to connect with registered local blood banks.
