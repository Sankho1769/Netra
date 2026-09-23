import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/fulfillment/models/fulfillment_model.dart';
import 'package:netra_app/features/fulfillment/screens/fulfillment_detail_screen.dart';
import 'package:netra_app/features/fulfillment/screens/fulfillment_list_screen.dart';
import 'package:netra_app/features/fulfillment/services/fulfillment_api_service.dart';
import 'package:netra_app/features/fulfillment/widgets/fulfillment_card.dart';
import 'package:netra_app/features/fulfillment/widgets/fulfillment_status_badge.dart';
import 'package:netra_app/features/notification/models/notification_model.dart';
import 'package:netra_app/features/notification/services/push_notification_service.dart';

class FakeFulfillmentApiService extends FulfillmentApiService {
  List<FulfillmentModel> fulfillmentsToReturn = [];
  FulfillmentModel? detailFulfillment;
  bool startCalled = false;
  bool completeCalled = false;
  bool failCalled = false;
  bool cancelCalled = false;
  String? lastReason;

  @override
  Future<List<FulfillmentModel>> getMyFulfillments(
      {int page = 0, int size = 20}) async {
    return fulfillmentsToReturn;
  }

  @override
  Future<List<FulfillmentModel>> getPendingFulfillments(
      {int page = 0, int size = 20}) async {
    return fulfillmentsToReturn;
  }

  @override
  Future<FulfillmentModel> getFulfillmentById(String fulfillmentId) async {
    if (detailFulfillment != null) {
      return detailFulfillment!;
    }
    throw Exception('Not found');
  }

  @override
  Future<FulfillmentModel> startFulfillment(String fulfillmentId) async {
    startCalled = true;
    return detailFulfillment!.copyWith(status: FulfillmentStatus.inProgress);
  }

  @override
  Future<FulfillmentModel> completeFulfillment(String fulfillmentId) async {
    completeCalled = true;
    return detailFulfillment!.copyWith(
      status: FulfillmentStatus.fulfilled,
      completedAt: DateTime.now(),
    );
  }

  @override
  Future<FulfillmentModel> failFulfillment(
      String fulfillmentId, FailFulfillmentDto dto) async {
    failCalled = true;
    lastReason = dto.failureReason;
    return detailFulfillment!.copyWith(
      status: FulfillmentStatus.failed,
      failureReason: dto.failureReason,
    );
  }

  @override
  Future<FulfillmentModel> cancelFulfillment(
      String fulfillmentId, CancelFulfillmentDto dto) async {
    cancelCalled = true;
    lastReason = dto.cancellationReason;
    return detailFulfillment!.copyWith(
      status: FulfillmentStatus.cancelled,
      cancellationReason: dto.cancellationReason,
    );
  }
}

void main() {
  group('Fulfillment Model & Enums Tests', () {
    test('Parses complete valid fulfillment JSON', () {
      final json = {
        'id': 'ful-100',
        'bloodRequestId': 'req-100',
        'donationId': 'don-100',
        'units': 2,
        'status': 'READY',
        'createdByUserId': 'bank-user-1',
        'startedByUserId': null,
        'completedByUserId': null,
        'failedByUserId': null,
        'cancelledByUserId': null,
        'startedAt': null,
        'completedAt': null,
        'failedAt': null,
        'cancelledAt': null,
        'failureReason': null,
        'cancellationReason': null,
        'notes': 'Verified unit reserved for surgery',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'updatedAt': '2026-09-22T10:00:00.000Z',
        'hospitalName': 'Apollo Multispecialty Hospital',
        'hospitalAddress': 'EM Bypass',
        'city': 'Kolkata',
        'state': 'WB',
        'requestedBloodGroup': 'O_POSITIVE',
        'unitsRequired': 2,
        'unitsFulfilled': 0,
        'remainingUnits': 2,
        'donorUserId': 'donor-user-1',
        'donorBloodGroup': 'O_POSITIVE',
        'donationDate': '2026-09-22',
        'isRequester': false,
        'isDonor': false,
        'canManage': true,
      };

      final fulfillment = FulfillmentModel.fromJson(json);

      expect(fulfillment.id, equals('ful-100'));
      expect(fulfillment.bloodRequestId, equals('req-100'));
      expect(fulfillment.donationId, equals('don-100'));
      expect(fulfillment.units, equals(2));
      expect(fulfillment.status, equals(FulfillmentStatus.ready));
      expect(fulfillment.createdByUserId, equals('bank-user-1'));
      expect(fulfillment.notes, equals('Verified unit reserved for surgery'));
      expect(
          fulfillment.hospitalName, equals('Apollo Multispecialty Hospital'));
      expect(fulfillment.city, equals('Kolkata'));
      expect(fulfillment.state, equals('WB'));
      expect(fulfillment.requestedBloodGroup, equals('O_POSITIVE'));
      expect(fulfillment.donorBloodGroup, equals('O_POSITIVE'));
      expect(fulfillment.canManage, isTrue);
      expect(fulfillment.status.isTerminal, isFalse);
    });

    test('Parses terminal fulfillment JSONs (FULFILLED, FAILED, CANCELLED)',
        () {
      final fulfilledJson = {
        'id': 'ful-101',
        'bloodRequestId': 'req-101',
        'donationId': 'don-101',
        'units': 1,
        'status': 'FULFILLED',
        'createdByUserId': 'bank-user-1',
        'completedAt': '2026-09-22T14:00:00.000Z',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'updatedAt': '2026-09-22T14:00:00.000Z',
      };
      final fulfilled = FulfillmentModel.fromJson(fulfilledJson);
      expect(fulfilled.status, equals(FulfillmentStatus.fulfilled));
      expect(fulfilled.status.isTerminal, isTrue);
      expect(fulfilled.completedAt, isNotNull);

      final failedJson = {
        'id': 'ful-102',
        'bloodRequestId': 'req-102',
        'donationId': 'don-102',
        'units': 1,
        'status': 'FAILED',
        'createdByUserId': 'bank-user-1',
        'failureReason': 'Cold chain temperature excursion during transit',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'updatedAt': '2026-09-22T11:00:00.000Z',
      };
      final failed = FulfillmentModel.fromJson(failedJson);
      expect(failed.status, equals(FulfillmentStatus.failed));
      expect(failed.status.isTerminal, isTrue);
      expect(failed.failureReason,
          equals('Cold chain temperature excursion during transit'));

      final cancelledJson = {
        'id': 'ful-103',
        'bloodRequestId': 'req-103',
        'donationId': 'don-103',
        'units': 1,
        'status': 'CANCELLED',
        'createdByUserId': 'bank-user-1',
        'cancellationReason': 'Request satisfied by hospital internal reserve',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'updatedAt': '2026-09-22T10:30:00.000Z',
      };
      final cancelled = FulfillmentModel.fromJson(cancelledJson);
      expect(cancelled.status, equals(FulfillmentStatus.cancelled));
      expect(cancelled.status.isTerminal, isTrue);
      expect(cancelled.cancellationReason,
          equals('Request satisfied by hospital internal reserve'));
    });

    test('FulfillmentStatus enum mappings and labels', () {
      expect(FulfillmentStatus.fromString('READY'),
          equals(FulfillmentStatus.ready));
      expect(FulfillmentStatus.fromString('IN_PROGRESS'),
          equals(FulfillmentStatus.inProgress));
      expect(FulfillmentStatus.fromString('FULFILLED'),
          equals(FulfillmentStatus.fulfilled));
      expect(FulfillmentStatus.fromString('FAILED'),
          equals(FulfillmentStatus.failed));
      expect(FulfillmentStatus.fromString('CANCELLED'),
          equals(FulfillmentStatus.cancelled));
      expect(FulfillmentStatus.fromString('UNKNOWN_STATUS'),
          equals(FulfillmentStatus.unknown));
      expect(FulfillmentStatus.fromString(null),
          equals(FulfillmentStatus.unknown));

      expect(FulfillmentStatus.ready.toServerString(), equals('READY'));
      expect(
          FulfillmentStatus.inProgress.toServerString(), equals('IN_PROGRESS'));
      expect(FulfillmentStatus.fulfilled.toServerString(), equals('FULFILLED'));
      expect(FulfillmentStatus.failed.toServerString(), equals('FAILED'));
      expect(FulfillmentStatus.cancelled.toServerString(), equals('CANCELLED'));
      expect(FulfillmentStatus.unknown.toServerString(), equals('UNKNOWN'));

      expect(FulfillmentStatus.ready.displayName, equals('Ready'));
      expect(FulfillmentStatus.inProgress.displayName, equals('In Progress'));
      expect(FulfillmentStatus.fulfilled.displayName, equals('Fulfilled'));
      expect(FulfillmentStatus.failed.displayName, equals('Failed'));
      expect(FulfillmentStatus.cancelled.displayName, equals('Cancelled'));

      expect(FulfillmentStatus.ready.isTerminal, isFalse);
      expect(FulfillmentStatus.inProgress.isTerminal, isFalse);
      expect(FulfillmentStatus.fulfilled.isTerminal, isTrue);
      expect(FulfillmentStatus.failed.isTerminal, isTrue);
      expect(FulfillmentStatus.cancelled.isTerminal, isTrue);
    });

    test('DTOs serialize correctly to JSON', () {
      final createDto = CreateFulfillmentDto(
        bloodRequestId: 'req-200',
        donationId: 'don-200',
        units: 2,
        notes: 'Clinical reservation',
      );
      final createJson = createDto.toJson();
      expect(createJson['bloodRequestId'], equals('req-200'));
      expect(createJson['donationId'], equals('don-200'));
      expect(createJson['units'], equals(2));
      expect(createJson['notes'], equals('Clinical reservation'));

      final failDto = FailFulfillmentDto(failureReason: 'Damaged packaging');
      final failJson = failDto.toJson();
      expect(failJson['failureReason'], equals('Damaged packaging'));

      final cancelDto =
          CancelFulfillmentDto(cancellationReason: 'Duplicate entry');
      final cancelJson = cancelDto.toJson();
      expect(cancelJson['cancellationReason'], equals('Duplicate entry'));
    });
  });

  group('Fulfillment Notification Integration Tests', () {
    test('NotificationType supports fulfillment notification types', () {
      expect(NotificationType.fromString('FULFILLMENT_CREATED'),
          equals(NotificationType.fulfillmentCreated));
      expect(NotificationType.fromString('FULFILLMENT_STARTED'),
          equals(NotificationType.fulfillmentStarted));
      expect(NotificationType.fromString('FULFILLMENT_COMPLETED'),
          equals(NotificationType.fulfillmentCompleted));
      expect(NotificationType.fromString('FULFILLMENT_FAILED'),
          equals(NotificationType.fulfillmentFailed));
      expect(NotificationType.fromString('FULFILLMENT_CANCELLED'),
          equals(NotificationType.fulfillmentCancelled));

      expect(NotificationType.fulfillmentCreated.toServerString(),
          equals('FULFILLMENT_CREATED'));
      expect(NotificationType.fulfillmentStarted.toServerString(),
          equals('FULFILLMENT_STARTED'));
      expect(NotificationType.fulfillmentCompleted.toServerString(),
          equals('FULFILLMENT_COMPLETED'));
      expect(NotificationType.fulfillmentFailed.toServerString(),
          equals('FULFILLMENT_FAILED'));
      expect(NotificationType.fulfillmentCancelled.toServerString(),
          equals('FULFILLMENT_CANCELLED'));
    });

    test('NotificationReferenceType supports FULFILLMENT', () {
      expect(NotificationReferenceType.fromString('FULFILLMENT'),
          equals(NotificationReferenceType.fulfillment));
      expect(NotificationReferenceType.fulfillment.toServerString(),
          equals('FULFILLMENT'));
    });

    test('PushNotificationService recognizes FULFILLMENT payload reference',
        () {
      final pushService = PushNotificationService();
      final validFulfillmentPayload = {
        'notificationId': 'nid-888',
        'type': 'FULFILLMENT_COMPLETED',
        'referenceType': 'FULFILLMENT',
        'referenceId': 'ful-100',
      };

      expect(
          pushService.isValidPayloadReference(validFulfillmentPayload), isTrue);
    });
  });

  group('Fulfillment UI Widgets Tests', () {
    testWidgets('FulfillmentStatusBadge displays correct label and colors',
        (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                FulfillmentStatusBadge(status: FulfillmentStatus.ready),
                FulfillmentStatusBadge(status: FulfillmentStatus.inProgress),
                FulfillmentStatusBadge(status: FulfillmentStatus.fulfilled),
                FulfillmentStatusBadge(status: FulfillmentStatus.failed),
                FulfillmentStatusBadge(status: FulfillmentStatus.cancelled),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Ready'), findsOneWidget);
      expect(find.text('In Progress'), findsOneWidget);
      expect(find.text('Fulfilled'), findsOneWidget);
      expect(find.text('Failed'), findsOneWidget);
      expect(find.text('Cancelled'), findsOneWidget);
    });

    testWidgets('FulfillmentCard renders details, badge, and triggers onTap',
        (tester) async {
      bool tapped = false;
      final fulfillment = FulfillmentModel(
        id: 'ful-test-1',
        bloodRequestId: 'req-test-1',
        donationId: 'don-test-1',
        units: 1,
        status: FulfillmentStatus.inProgress,
        createdByUserId: 'user-1',
        hospitalName: 'AMRI Hospital',
        hospitalAddress: 'Salt Lake',
        city: 'Kolkata',
        state: 'WB',
        requestedBloodGroup: 'A_POSITIVE',
        donorBloodGroup: 'A_POSITIVE',
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: FulfillmentCard(
              fulfillment: fulfillment,
              onTap: () {
                tapped = true;
              },
            ),
          ),
        ),
      );

      expect(find.text('AMRI Hospital'), findsOneWidget);
      expect(find.text('A_POSITIVE'), findsWidgets);
      expect(find.text('1 unit'), findsOneWidget);
      expect(find.text('In Progress'), findsOneWidget);

      await tester.tap(find.byType(FulfillmentCard));
      expect(tapped, isTrue);
    });
  });

  group('Fulfillment Screens Tests', () {
    testWidgets(
        'FulfillmentListScreen shows empty state when no fulfillments exist',
        (tester) async {
      final fakeApi = FakeFulfillmentApiService();
      fakeApi.fulfillmentsToReturn = [];

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentListScreen(apiService: fakeApi),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Fulfillments'), findsOneWidget);
      expect(find.text('No fulfillments found.'), findsOneWidget);
    });

    testWidgets('FulfillmentListScreen renders cards when fulfillments exist',
        (tester) async {
      final fakeApi = FakeFulfillmentApiService();
      fakeApi.fulfillmentsToReturn = [
        FulfillmentModel(
          id: 'ful-1',
          bloodRequestId: 'req-1',
          donationId: 'don-1',
          units: 1,
          status: FulfillmentStatus.ready,
          createdByUserId: 'user-1',
          hospitalName: 'City Hospital',
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentListScreen(apiService: fakeApi),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('City Hospital'), findsOneWidget);
      expect(find.text('Ready'), findsOneWidget);
    });

    testWidgets(
        'FulfillmentDetailScreen renders actions for READY state (Start & Cancel)',
        (tester) async {
      final fakeApi = FakeFulfillmentApiService();
      fakeApi.detailFulfillment = FulfillmentModel(
        id: 'ful-detail-1',
        bloodRequestId: 'req-detail-1',
        donationId: 'don-detail-1',
        units: 1,
        status: FulfillmentStatus.ready,
        createdByUserId: 'user-1',
        hospitalName: 'Apollo Gleneagles',
        requestedBloodGroup: 'B_POSITIVE',
        canManage: true,
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentDetailScreen(
            fulfillmentId: 'ful-detail-1',
            apiService: fakeApi,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Fulfillment Details'), findsOneWidget);
      expect(find.text('Apollo Gleneagles'), findsOneWidget);
      expect(find.text('Start Fulfillment'), findsOneWidget);
      expect(find.text('Cancel Fulfillment'), findsOneWidget);
      expect(find.text('Complete Fulfillment'), findsNothing);

      // Tap start fulfillment
      await tester.tap(find.text('Start Fulfillment'));
      await tester.pumpAndSettle();
      expect(fakeApi.startCalled, isTrue);
    });

    testWidgets(
        'FulfillmentDetailScreen renders actions for IN_PROGRESS state (Complete & Fail)',
        (tester) async {
      final fakeApi = FakeFulfillmentApiService();
      fakeApi.detailFulfillment = FulfillmentModel(
        id: 'ful-detail-2',
        bloodRequestId: 'req-detail-2',
        donationId: 'don-detail-2',
        units: 2,
        status: FulfillmentStatus.inProgress,
        createdByUserId: 'user-1',
        hospitalName: 'Woodlands Hospital',
        requestedBloodGroup: 'AB_POSITIVE',
        canManage: true,
        startedAt: DateTime.now(),
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentDetailScreen(
            fulfillmentId: 'ful-detail-2',
            apiService: fakeApi,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Complete Fulfillment'), findsOneWidget);
      expect(find.text('Fail Fulfillment'), findsOneWidget);
      expect(find.text('Start Fulfillment'), findsNothing);

      // Tap complete fulfillment
      await tester.tap(find.text('Complete Fulfillment'));
      await tester.pumpAndSettle();
      expect(fakeApi.completeCalled, isTrue);
    });

    testWidgets(
        'FulfillmentDetailScreen hides actions for terminal state FULFILLED',
        (tester) async {
      final fakeApi = FakeFulfillmentApiService();
      fakeApi.detailFulfillment = FulfillmentModel(
        id: 'ful-detail-3',
        bloodRequestId: 'req-detail-3',
        donationId: 'don-detail-3',
        units: 1,
        status: FulfillmentStatus.fulfilled,
        createdByUserId: 'user-1',
        hospitalName: 'SSKM Hospital',
        canManage: true,
        completedAt: DateTime.now(),
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: FulfillmentDetailScreen(
            fulfillmentId: 'ful-detail-3',
            apiService: fakeApi,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Start Fulfillment'), findsNothing);
      expect(find.text('Complete Fulfillment'), findsNothing);
      expect(find.text('Fail Fulfillment'), findsNothing);
      expect(find.text('Cancel Fulfillment'), findsNothing);
    });
  });
}
