import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/blood_request/models/blood_request.dart';
import 'package:netra_app/features/blood_request/widgets/request_status_badge.dart';
import 'package:netra_app/features/donation/models/donation_model.dart';
import 'package:netra_app/features/donation/widgets/donation_status_badge.dart';
import 'package:netra_app/features/donor_response/models/donor_match_response_model.dart';
import 'package:netra_app/features/donor_response/widgets/match_status_badge.dart';
import 'package:netra_app/features/fulfillment/models/fulfillment_model.dart';
import 'package:netra_app/features/fulfillment/screens/fulfillment_detail_screen.dart';
import 'package:netra_app/features/fulfillment/services/fulfillment_api_service.dart';
import 'package:netra_app/features/fulfillment/widgets/fulfillment_status_badge.dart';

class MockFulfillmentApiService extends FulfillmentApiService {
  FulfillmentModel? detailFulfillment;
  bool startCalled = false;
  bool completeCalled = false;
  bool cancelCalled = false;
  bool failCalled = false;
  Exception? throwErrorOnAction;

  @override
  Future<FulfillmentModel> getFulfillmentById(String fulfillmentId) async {
    if (throwErrorOnAction != null) {
      throw throwErrorOnAction!;
    }
    if (detailFulfillment != null) {
      return detailFulfillment!;
    }
    throw Exception('Fulfillment not found: $fulfillmentId');
  }

  @override
  Future<FulfillmentModel> startFulfillment(String fulfillmentId) async {
    if (throwErrorOnAction != null) {
      throw throwErrorOnAction!;
    }
    startCalled = true;
    detailFulfillment =
        detailFulfillment!.copyWith(status: FulfillmentStatus.inProgress);
    return detailFulfillment!;
  }

  @override
  Future<FulfillmentModel> completeFulfillment(String fulfillmentId) async {
    if (throwErrorOnAction != null) {
      throw throwErrorOnAction!;
    }
    completeCalled = true;
    detailFulfillment = detailFulfillment!.copyWith(
      status: FulfillmentStatus.fulfilled,
      completedAt: DateTime.now(),
    );
    return detailFulfillment!;
  }

  @override
  Future<FulfillmentModel> failFulfillment(
      String fulfillmentId, FailFulfillmentDto dto) async {
    if (throwErrorOnAction != null) {
      throw throwErrorOnAction!;
    }
    failCalled = true;
    detailFulfillment = detailFulfillment!.copyWith(
      status: FulfillmentStatus.failed,
      failureReason: dto.failureReason,
    );
    return detailFulfillment!;
  }

  @override
  Future<FulfillmentModel> cancelFulfillment(
      String fulfillmentId, CancelFulfillmentDto dto) async {
    if (throwErrorOnAction != null) {
      throw throwErrorOnAction!;
    }
    cancelCalled = true;
    detailFulfillment = detailFulfillment!.copyWith(
      status: FulfillmentStatus.cancelled,
      cancellationReason: dto.cancellationReason,
    );
    return detailFulfillment!;
  }
}

void main() {
  group('Per-Aggregate State Machine Verification', () {
    test('DonorMatch Aggregate: Valid and terminal state properties', () {
      expect(DonorMatchStatus.fromString('MATCHED'),
          equals(DonorMatchStatus.matched));
      expect(DonorMatchStatus.fromString('ACCEPTED'),
          equals(DonorMatchStatus.accepted));
      expect(DonorMatchStatus.fromString('DECLINED'),
          equals(DonorMatchStatus.declined));
      expect(DonorMatchStatus.fromString('EXPIRED'),
          equals(DonorMatchStatus.expired));
      expect(DonorMatchStatus.fromString('CANCELLED'),
          equals(DonorMatchStatus.cancelled));

      // Terminal checks
      expect(DonorMatchStatus.matched.isTerminal, isFalse);
      expect(DonorMatchStatus.accepted.isTerminal, isTrue);
      expect(DonorMatchStatus.declined.isTerminal, isTrue);
      expect(DonorMatchStatus.expired.isTerminal, isTrue);
      expect(DonorMatchStatus.cancelled.isTerminal, isTrue);
    });

    test('Donation Aggregate: Valid and terminal state properties', () {
      expect(DonationVerificationStatus.fromString('PENDING_VERIFICATION'),
          equals(DonationVerificationStatus.pendingVerification));
      expect(DonationVerificationStatus.fromString('VERIFIED'),
          equals(DonationVerificationStatus.verified));
      expect(DonationVerificationStatus.fromString('REJECTED'),
          equals(DonationVerificationStatus.rejected));
      expect(DonationVerificationStatus.fromString('CANCELLED'),
          equals(DonationVerificationStatus.cancelled));

      // Terminal checks
      expect(
          DonationVerificationStatus.pendingVerification.isTerminal, isFalse);
      expect(DonationVerificationStatus.verified.isTerminal, isTrue);
      expect(DonationVerificationStatus.rejected.isTerminal, isTrue);
      expect(DonationVerificationStatus.cancelled.isTerminal, isTrue);
    });

    test('Fulfillment Aggregate: Valid transitions and terminal properties',
        () {
      expect(FulfillmentStatus.fromString('READY'),
          equals(FulfillmentStatus.ready));
      expect(FulfillmentStatus.fromString('IN_PROGRESS'),
          equals(FulfillmentStatus.inProgress));
      expect(FulfillmentStatus.fromString('FULFILLED'),
          equals(FulfillmentStatus.fulfilled));
      expect(FulfillmentStatus.fromString('CANCELLED'),
          equals(FulfillmentStatus.cancelled));
      expect(FulfillmentStatus.fromString('FAILED'),
          equals(FulfillmentStatus.failed));

      // Terminal state predicates
      expect(FulfillmentStatus.ready.isTerminal, isFalse);
      expect(FulfillmentStatus.inProgress.isTerminal, isFalse);
      expect(FulfillmentStatus.fulfilled.isTerminal, isTrue);
      expect(FulfillmentStatus.cancelled.isTerminal, isTrue);
      expect(FulfillmentStatus.failed.isTerminal, isTrue);
    });

    test('BloodRequest Aggregate: Valid states and terminal properties', () {
      expect(BloodRequestStatus.fromString('OPEN'),
          equals(BloodRequestStatus.open));
      expect(BloodRequestStatus.fromString('FULFILLED'),
          equals(BloodRequestStatus.fulfilled));
      expect(BloodRequestStatus.fromString('CANCELLED'),
          equals(BloodRequestStatus.cancelled));
      expect(BloodRequestStatus.fromString('EXPIRED'),
          equals(BloodRequestStatus.expired));
    });
  });

  group('UI Status Representation across all 11 lifecycle statuses', () {
    testWidgets('Renders all DonorMatch aggregate badges accurately',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: const [
                MatchStatusBadge(status: DonorMatchStatus.matched),
                MatchStatusBadge(status: DonorMatchStatus.accepted),
                MatchStatusBadge(status: DonorMatchStatus.declined),
                MatchStatusBadge(status: DonorMatchStatus.expired),
                MatchStatusBadge(status: DonorMatchStatus.cancelled),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Pending Response'), findsOneWidget);
      expect(find.text('Accepted'), findsOneWidget);
      expect(find.text('Declined'), findsOneWidget);
      expect(find.text('Expired'), findsOneWidget);
      expect(find.text('Cancelled'), findsOneWidget);
    });

    testWidgets('Renders all Donation aggregate badges accurately',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: const [
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

    testWidgets('Renders all Fulfillment aggregate badges accurately',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: const [
                FulfillmentStatusBadge(status: FulfillmentStatus.ready),
                FulfillmentStatusBadge(status: FulfillmentStatus.inProgress),
                FulfillmentStatusBadge(status: FulfillmentStatus.fulfilled),
                FulfillmentStatusBadge(status: FulfillmentStatus.cancelled),
                FulfillmentStatusBadge(status: FulfillmentStatus.failed),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Ready'), findsOneWidget);
      expect(find.text('In Progress'), findsOneWidget);
      expect(find.text('Fulfilled'), findsOneWidget);
      expect(find.text('Cancelled'), findsOneWidget);
      expect(find.text('Failed'), findsOneWidget);
    });

    testWidgets('Renders all BloodRequest aggregate badges accurately',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: const [
                RequestStatusBadge(status: BloodRequestStatus.open),
                RequestStatusBadge(status: BloodRequestStatus.fulfilled),
                RequestStatusBadge(status: BloodRequestStatus.cancelled),
                RequestStatusBadge(status: BloodRequestStatus.expired),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Open'), findsOneWidget);
      expect(find.text('Fulfilled'), findsOneWidget);
      expect(find.text('Cancelled'), findsOneWidget);
      expect(find.text('Expired'), findsOneWidget);
    });
  });

  group('End-to-End Operational Lifecycle & Error Handling in UI', () {
    late MockFulfillmentApiService apiService;

    setUp(() {
      apiService = MockFulfillmentApiService();
    });

    testWidgets(
        'Complete Fulfillment Lifecycle: READY -> IN_PROGRESS -> FULFILLED with backend updates',
        (tester) async {
      final initialFulfillment = FulfillmentModel(
        id: 'ful-e2e-1',
        bloodRequestId: 'req-e2e-1',
        donationId: 'don-e2e-1',
        units: 2,
        status: FulfillmentStatus.ready,
        createdByUserId: 'staff-1',
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
        hospitalName: 'Central Care Hospital',
        city: 'Mumbai',
        state: 'Maharashtra',
        requestedBloodGroup: 'O_POSITIVE',
        donorBloodGroup: 'O_POSITIVE',
        canManage: true,
      );

      apiService.detailFulfillment = initialFulfillment;

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentDetailScreen(
            fulfillmentId: 'ful-e2e-1',
            apiService: apiService,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify Initial Screen: Ready status, Start button present
      expect(find.text('Central Care Hospital'), findsOneWidget);
      expect(find.text('Ready'), findsOneWidget);
      expect(find.text('Start Fulfillment'), findsOneWidget);

      // Staff taps "Start Fulfillment"
      await tester.tap(find.text('Start Fulfillment'));
      await tester.pumpAndSettle();

      // Verify backend mutation occurred and UI transitions to In Progress
      expect(apiService.startCalled, isTrue);
      expect(find.text('In Progress'), findsOneWidget);
      expect(find.text('Complete Fulfillment'), findsOneWidget);
      expect(find.text('Fail Fulfillment'), findsOneWidget);

      // Staff taps "Complete Fulfillment"
      await tester.tap(find.text('Complete Fulfillment'));
      await tester.pumpAndSettle();

      // Verify backend completion mutation occurred and UI reflects terminal Fulfilled state
      expect(apiService.completeCalled, isTrue);
      expect(find.text('Fulfilled'), findsOneWidget);
      expect(find.text('Start Fulfillment'), findsNothing);
      expect(find.text('Complete Fulfillment'), findsNothing);
    });

    testWidgets('Error Handling: 409 Conflict displays clear error banner',
        (tester) async {
      apiService.throwErrorOnAction = Exception(
          'Conflict: Blood request was updated or cancelled by another operation.');

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentDetailScreen(
            fulfillmentId: 'ful-conflict-1',
            apiService: apiService,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify error view is rendered with safe retry
      expect(
          find.text(
              'Conflict: Blood request was updated or cancelled by another operation.'),
          findsOneWidget);
      expect(find.text('Retry'), findsOneWidget);

      // Now resolve error and tap Retry
      apiService.throwErrorOnAction = null;
      apiService.detailFulfillment = FulfillmentModel(
        id: 'ful-conflict-1',
        bloodRequestId: 'req-1',
        donationId: 'don-1',
        units: 1,
        status: FulfillmentStatus.ready,
        createdByUserId: 'staff-1',
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
        hospitalName: 'Apollo Hospital',
        city: 'Mumbai',
        state: 'Maharashtra',
        requestedBloodGroup: 'O_POSITIVE',
        donorBloodGroup: 'O_POSITIVE',
        canManage: true,
      );

      await tester.tap(find.text('Retry'));
      await tester.pumpAndSettle();

      // Verified recovered state
      expect(find.text('Apollo Hospital'), findsOneWidget);
      expect(find.text('Ready'), findsOneWidget);
    });
  });
}
