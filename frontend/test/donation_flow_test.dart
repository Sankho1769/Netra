import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/donation/models/donation_model.dart';
import 'package:netra_app/features/donation/screens/claim_donation_screen.dart';
import 'package:netra_app/features/donation/screens/donation_history_screen.dart';
import 'package:netra_app/features/donation/services/donation_api_service.dart';
import 'package:netra_app/features/donation/widgets/donation_card.dart';
import 'package:netra_app/features/donation/widgets/donation_status_badge.dart';
import 'package:netra_app/features/notification/models/notification_model.dart';
import 'package:netra_app/features/notification/services/push_notification_service.dart';

class FakeDonationApiService extends DonationApiService {
  List<DonationModel> donationsToReturn = [];
  bool cancelClaimCalled = false;
  CreateDonationClaimDto? submittedDto;

  @override
  Future<List<DonationModel>> getMyDonations(
      {int page = 0, int size = 20}) async {
    return donationsToReturn;
  }

  @override
  Future<DonationModel> cancelClaim(String donationId) async {
    cancelClaimCalled = true;
    return DonationModel(
      id: donationId,
      donorUserId: 'donor-1',
      sourceType: DonationSourceType.bloodRequest,
      donationDate: DateTime.now(),
      verificationStatus: DonationVerificationStatus.cancelled,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
  }

  @override
  Future<DonationModel> submitClaim(CreateDonationClaimDto dto) async {
    submittedDto = dto;
    return DonationModel(
      id: 'new-donation-id',
      donorUserId: 'donor-1',
      sourceType: dto.sourceType,
      bloodRequestId: dto.bloodRequestId,
      donationEventId: dto.donationEventId,
      donationDate: dto.donationDate,
      verificationStatus: DonationVerificationStatus.pendingVerification,
      notes: dto.notes,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
  }
}

void main() {
  group('Donation Model & Enums Tests', () {
    test('Parses complete valid donation JSON', () {
      final json = {
        'id': 'don-100',
        'donorUserId': 'user-100',
        'donorName': 'Jolly Banerjee',
        'sourceType': 'BLOOD_REQUEST',
        'bloodRequestId': 'req-100',
        'referenceTitle': 'City General Hospital',
        'referenceLocation': 'Kolkata, WB',
        'donationDate': '2026-09-22',
        'verificationStatus': 'VERIFIED',
        'verifiedAt': '2026-09-22T14:30:00.000Z',
        'verifiedByUserId': 'staff-100',
        'notes': '1 unit whole blood donated',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'updatedAt': '2026-09-22T14:30:00.000Z',
      };

      final donation = DonationModel.fromJson(json);

      expect(donation.id, equals('don-100'));
      expect(donation.donorUserId, equals('user-100'));
      expect(donation.donorName, equals('Jolly Banerjee'));
      expect(donation.sourceType, equals(DonationSourceType.bloodRequest));
      expect(donation.bloodRequestId, equals('req-100'));
      expect(donation.referenceTitle, equals('City General Hospital'));
      expect(donation.referenceLocation, equals('Kolkata, WB'));
      expect(donation.donationDate.year, equals(2026));
      expect(donation.verificationStatus,
          equals(DonationVerificationStatus.verified));
      expect(donation.verifiedByUserId, equals('staff-100'));
      expect(donation.notes, equals('1 unit whole blood donated'));
      expect(donation.verificationStatus.isTerminal, isTrue);
    });

    test('Parses rejected donation with rejectionReason', () {
      final json = {
        'id': 'don-101',
        'donorUserId': 'user-100',
        'sourceType': 'DONATION_EVENT',
        'donationEventId': 'event-200',
        'referenceTitle': 'Red Cross Blood Camp',
        'donationDate': '2026-09-20',
        'verificationStatus': 'REJECTED',
        'rejectionReason': 'Donor did not complete screening',
        'createdAt': '2026-09-20T10:00:00.000Z',
        'updatedAt': '2026-09-20T11:00:00.000Z',
      };

      final donation = DonationModel.fromJson(json);

      expect(donation.sourceType, equals(DonationSourceType.donationEvent));
      expect(donation.verificationStatus,
          equals(DonationVerificationStatus.rejected));
      expect(
          donation.rejectionReason, equals('Donor did not complete screening'));
      expect(donation.verificationStatus.isTerminal, isTrue);
    });

    test('SourceType and VerificationStatus enum mappings', () {
      expect(DonationSourceType.fromString('BLOOD_REQUEST'),
          equals(DonationSourceType.bloodRequest));
      expect(DonationSourceType.fromString('DONATION_EVENT'),
          equals(DonationSourceType.donationEvent));
      expect(DonationSourceType.fromString(null),
          equals(DonationSourceType.unknown));

      expect(DonationVerificationStatus.fromString('PENDING_VERIFICATION'),
          equals(DonationVerificationStatus.pendingVerification));
      expect(DonationVerificationStatus.fromString('VERIFIED'),
          equals(DonationVerificationStatus.verified));
      expect(DonationVerificationStatus.fromString('REJECTED'),
          equals(DonationVerificationStatus.rejected));
      expect(DonationVerificationStatus.fromString('CANCELLED'),
          equals(DonationVerificationStatus.cancelled));
      expect(DonationVerificationStatus.fromString(null),
          equals(DonationVerificationStatus.unknown));

      expect(
          DonationVerificationStatus.pendingVerification.isTerminal, isFalse);
      expect(DonationVerificationStatus.cancelled.isTerminal, isTrue);
    });

    test('CreateDonationClaimDto serializes correctly', () {
      final dto = CreateDonationClaimDto(
        sourceType: DonationSourceType.bloodRequest,
        bloodRequestId: 'req-300',
        donationDate: DateTime(2026, 9, 22),
        notes: 'Donation completed at blood center',
      );

      final json = dto.toJson();
      expect(json['sourceType'], equals('BLOOD_REQUEST'));
      expect(json['bloodRequestId'], equals('req-300'));
      expect(json['donationDate'], equals('2026-09-22'));
      expect(json['notes'], equals('Donation completed at blood center'));
    });
  });

  group('Donation Notification Integration Tests', () {
    test('NotificationType supports donation notification types', () {
      expect(NotificationType.fromString('DONATION_SUBMITTED'),
          equals(NotificationType.donationSubmitted));
      expect(NotificationType.fromString('DONATION_VERIFIED'),
          equals(NotificationType.donationVerified));
      expect(NotificationType.fromString('DONATION_REJECTED'),
          equals(NotificationType.donationRejected));

      expect(NotificationType.donationSubmitted.toServerString(),
          equals('DONATION_SUBMITTED'));
      expect(NotificationType.donationVerified.toServerString(),
          equals('DONATION_VERIFIED'));
      expect(NotificationType.donationRejected.toServerString(),
          equals('DONATION_REJECTED'));
    });

    test('NotificationReferenceType supports DONATION', () {
      expect(NotificationReferenceType.fromString('DONATION'),
          equals(NotificationReferenceType.donation));
      expect(NotificationReferenceType.donation.toServerString(),
          equals('DONATION'));
    });

    test('PushNotificationService recognizes DONATION payload reference', () {
      final pushService = PushNotificationService();
      final validDonationPayload = {
        'notificationId': 'nid-999',
        'type': 'DONATION_VERIFIED',
        'referenceType': 'DONATION',
        'referenceId': 'don-100',
      };

      expect(pushService.isValidPayloadReference(validDonationPayload), isTrue);
    });
  });

  group('Donation UI Widgets Tests', () {
    testWidgets('DonationStatusBadge displays correct label and colors',
        (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                DonationStatusBadge(
                    status: DonationVerificationStatus.pendingVerification),
                DonationStatusBadge(
                    status: DonationVerificationStatus.verified),
                DonationStatusBadge(
                    status: DonationVerificationStatus.rejected),
                DonationStatusBadge(
                    status: DonationVerificationStatus.cancelled),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Pending Verification'), findsOneWidget);
      expect(find.text('Verified'), findsOneWidget);
      expect(find.text('Rejected'), findsOneWidget);
      expect(find.text('Cancelled'), findsOneWidget);
    });

    testWidgets('DonationCard renders details, badge, and cancel action',
        (tester) async {
      bool cancelTapped = false;
      final donation = DonationModel(
        id: 'don-test-1',
        donorUserId: 'donor-1',
        sourceType: DonationSourceType.bloodRequest,
        referenceTitle: 'Apollo Multi-Specialty Hospital',
        referenceLocation: 'EM Bypass, Kolkata',
        donationDate: DateTime(2026, 9, 22),
        verificationStatus: DonationVerificationStatus.pendingVerification,
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DonationCard(
              donation: donation,
              onCancel: () {
                cancelTapped = true;
              },
            ),
          ),
        ),
      );

      expect(find.text('Direct Blood Request'), findsOneWidget);
      expect(find.text('Apollo Multi-Specialty Hospital'), findsOneWidget);
      expect(find.text('EM Bypass, Kolkata'), findsOneWidget);
      expect(find.text('Pending Verification'), findsOneWidget);
      expect(find.text('Cancel Claim'), findsOneWidget);

      await tester.tap(find.text('Cancel Claim'));
      await tester.pump();

      expect(cancelTapped, isTrue);
    });

    testWidgets('DonationHistoryScreen displays empty state when no donations',
        (tester) async {
      final fakeApi = FakeDonationApiService();
      fakeApi.donationsToReturn = [];

      await tester.pumpWidget(
        MaterialApp(
          home: DonationHistoryScreen(apiService: fakeApi),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('No Donations Recorded Yet'), findsOneWidget);
      expect(find.text('Claim a Donation'), findsWidgets);
    });

    testWidgets('DonationHistoryScreen displays donation list and badge',
        (tester) async {
      final fakeApi = FakeDonationApiService();
      fakeApi.donationsToReturn = [
        DonationModel(
          id: 'don-verified-1',
          donorUserId: 'donor-1',
          sourceType: DonationSourceType.donationEvent,
          referenceTitle: 'Kolkata Central Blood Camp',
          referenceLocation: 'Salt Lake, Sector 5',
          donationDate: DateTime(2026, 9, 21),
          verificationStatus: DonationVerificationStatus.verified,
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: DonationHistoryScreen(apiService: fakeApi),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('Kolkata Central Blood Camp'), findsOneWidget);
      expect(find.text('Salt Lake, Sector 5'), findsOneWidget);
      expect(find.text('Verified'), findsOneWidget);
      expect(find.text('No Donations Recorded Yet'), findsNothing);
    });

    testWidgets('ClaimDonationScreen toggles source type and validates input',
        (tester) async {
      final fakeApi = FakeDonationApiService();

      await tester.pumpWidget(
        MaterialApp(
          home: ClaimDonationScreen(apiService: fakeApi),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('Blood Request ID'), findsOneWidget);

      // Toggle to Donation Camp
      await tester.tap(find.text('Donation Camp'));
      await tester.pumpAndSettle();

      expect(find.text('Donation Camp ID'), findsOneWidget);

      // Try submitting without reference ID -> validation error
      await tester.ensureVisible(find.text('Submit Claim for Verification'));
      await tester.tap(find.text('Submit Claim for Verification'));
      await tester.pumpAndSettle();

      expect(find.text('Please enter the reference ID'), findsOneWidget);
    });
  });
}
