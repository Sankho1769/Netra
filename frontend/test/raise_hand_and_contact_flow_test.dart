import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/blood_request/models/blood_request.dart';
import 'package:netra_app/features/blood_request/widgets/blood_request_card.dart';
import 'package:netra_app/features/donor_response/models/donor_match_response_model.dart';
import 'package:netra_app/features/donor_response/screens/requester_match_list_screen.dart';
import 'package:netra_app/features/donor_response/services/donor_response_api_service.dart';
import 'package:netra_app/features/events/screens/donation_event_list_screen.dart';

class FakeDonorResponseApiService extends DonorResponseApiService {
  final List<RequesterDonorMatch> fakeMatches;

  FakeDonorResponseApiService({required this.fakeMatches});

  @override
  Future<List<RequesterDonorMatch>> getMatchResponses(String requestId) async {
    return fakeMatches;
  }

  @override
  Future<MatchContactInfo> getMatchContact(String matchId) async {
    return MatchContactInfo(
      matchId: matchId,
      bloodRequestId: 'req-1',
      matchStatus: 'ACCEPTED',
      donorUserId: 'donor-1',
      donorName: 'Rahul Verma',
      donorPhone: '+919123456780',
      donorBloodGroup: 'O+',
      requesterUserId: 'req-user-1',
      requesterName: 'Priya Sharma',
      requesterPhone: '+919876543210',
      hospitalName: 'SSKM Hospital Kolkata',
      hospitalAddress: '110 Harish Mukherjee Road',
      city: 'Kolkata',
      state: 'West Bengal',
      coordinationNotice: 'Contact details are shared strictly for blood donation coordination.',
    );
  }
}

void main() {
  group('BloodRequestCard Fulfillment & Helper Count Tests', () {
    testWidgets('renders units fulfilled pill and helper count badge correctly', (tester) async {
      final request = BloodRequestSummary(
        id: 'req-123',
        bloodGroup: 'O+',
        unitsRequired: 3,
        unitsFulfilled: 1,
        helperCount: 2,
        urgency: BloodRequestUrgency.urgent,
        status: BloodRequestStatus.open,
        hospitalName: 'SSKM Hospital',
        city: 'Kolkata',
        state: 'West Bengal',
        distanceKm: 2.5,
        requiredBy: DateTime.now().add(const Duration(hours: 6)),
        createdAt: DateTime.now().subtract(const Duration(hours: 1)),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BloodRequestCard(
              request: request,
              onTap: () {},
            ),
          ),
        ),
      );

      // Verify blood group & hospital
      expect(find.text('O+'), findsOneWidget);
      expect(find.text('SSKM Hospital'), findsOneWidget);

      // Verify fulfillment progress pill
      expect(find.text('1/3 Fulfilled'), findsOneWidget);

      // Verify helper count
      expect(find.text('2 Offers'), findsOneWidget);
    });

    testWidgets('renders card without fulfillment pill when 0 units fulfilled', (tester) async {
      final request = BloodRequestSummary(
        id: 'req-456',
        bloodGroup: 'B+',
        unitsRequired: 2,
        unitsFulfilled: 0,
        helperCount: 0,
        urgency: BloodRequestUrgency.normal,
        status: BloodRequestStatus.open,
        hospitalName: 'Apollo Hospital',
        city: 'Kolkata',
        state: 'West Bengal',
        distanceKm: 4.0,
        requiredBy: DateTime.now().add(const Duration(hours: 12)),
        createdAt: DateTime.now().subtract(const Duration(hours: 2)),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BloodRequestCard(
              request: request,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('B+'), findsOneWidget);
      expect(find.text('Apollo Hospital'), findsOneWidget);
      expect(find.textContaining('Fulfilled'), findsNothing);
      expect(find.textContaining('Offers'), findsNothing);
    });
  });

  group('DonationEventListScreen NBTC Clinical Policy Guard Tests', () {
    testWidgets('shows NBTC clinical guidance dialog when standard donor taps Host Camp', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: DonationEventListScreen(),
        ),
      );

      await tester.pumpAndSettle();

      // Find the "Host a Camp" action in the AppBar
      final hostCampButton = find.byTooltip('Host a Camp');
      expect(hostCampButton, findsOneWidget);

      await tester.tap(hostCampButton);
      await tester.pumpAndSettle();

      // Verify NBTC clinical policy dialog appears
      expect(find.text('Clinical Organization Policy'), findsOneWidget);
      expect(
        find.textContaining('Under National Blood Transfusion Council (NBTC) safety regulations'),
        findsOneWidget,
      );
      expect(find.text('Browse Camps'), findsOneWidget);

      // Dismiss dialog
      await tester.tap(find.text('Browse Camps'));
      await tester.pumpAndSettle();

      expect(find.text('Clinical Organization Policy'), findsNothing);
    });
  });

  group('RequesterMatchListScreen Helper Acceptance & Contact Tests', () {
    testWidgets('renders Accept Helper and Decline buttons for pending match', (tester) async {
      final pendingMatch = RequesterDonorMatch(
        matchId: 'match-pending-1',
        bloodRequestId: 'req-1',
        donorDisplayName: 'Rohan Sharma',
        bloodGroup: 'O+',
        bloodGroupVerificationStatus: 'VERIFIED',
        availabilityStatus: 'AVAILABLE',
        distanceKm: 3.2,
        responseStatus: DonorMatchStatus.matched,
        createdAt: DateTime.now().subtract(const Duration(minutes: 30)),
        updatedAt: DateTime.now().subtract(const Duration(minutes: 30)),
        expiresAt: DateTime.now().add(const Duration(hours: 24)),
      );

      final fakeApi = FakeDonorResponseApiService(fakeMatches: [pendingMatch]);

      await tester.pumpWidget(
        MaterialApp(
          home: RequesterMatchListScreen(
            requestId: 'req-1',
            bloodGroup: 'O+',
            apiService: fakeApi,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify donor name and matched status
      expect(find.text('Rohan Sharma'), findsOneWidget);
      expect(find.text('Pending Response'), findsOneWidget);

      // Verify Accept Helper and Decline action buttons exist
      expect(find.text('Accept Helper'), findsOneWidget);
      expect(find.text('Decline'), findsOneWidget);
    });

    testWidgets('renders View Contact & Coordinate button for accepted match and opens sheet', (tester) async {
      final acceptedMatch = RequesterDonorMatch(
        matchId: 'match-accepted-1',
        bloodRequestId: 'req-1',
        donorDisplayName: 'Rahul Verma',
        bloodGroup: 'O+',
        bloodGroupVerificationStatus: 'VERIFIED',
        availabilityStatus: 'AVAILABLE',
        distanceKm: 1.5,
        responseStatus: DonorMatchStatus.accepted,
        createdAt: DateTime.now().subtract(const Duration(hours: 2)),
        updatedAt: DateTime.now().subtract(const Duration(minutes: 10)),
        respondedAt: DateTime.now().subtract(const Duration(minutes: 15)),
        expiresAt: DateTime.now().add(const Duration(hours: 24)),
      );

      final fakeApi = FakeDonorResponseApiService(fakeMatches: [acceptedMatch]);

      await tester.pumpWidget(
        MaterialApp(
          home: RequesterMatchListScreen(
            requestId: 'req-1',
            bloodGroup: 'O+',
            apiService: fakeApi,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify donor name and accepted badge
      expect(find.text('Rahul Verma'), findsOneWidget);
      expect(find.text('Accepted'), findsOneWidget);

      // Verify View Contact button
      final contactButton = find.text('View Contact & Coordinate');
      expect(contactButton, findsOneWidget);

      // Tap View Contact button
      await tester.tap(contactButton);
      await tester.pumpAndSettle();

      // Verify bottom sheet opens with contact phone and hospital
      expect(find.text('Donor Contact & Coordination'), findsOneWidget);
      expect(find.text('+919123456780'), findsOneWidget);
      expect(find.text('SSKM Hospital Kolkata'), findsOneWidget);
      expect(find.text('Close'), findsOneWidget);

      // Close bottom sheet
      await tester.tap(find.text('Close'));
      await tester.pumpAndSettle();
      expect(find.text('Donor Contact & Coordination'), findsNothing);
    });
  });
}
