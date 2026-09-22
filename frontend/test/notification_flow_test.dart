import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/notification/models/notification_model.dart';
import 'package:netra_app/features/notification/services/push_notification_service.dart';
import 'package:netra_app/features/notification/widgets/notification_bell_icon.dart';
import 'package:netra_app/features/notification/widgets/notification_item_card.dart';

void main() {
  group('Notification Model & DeliveryStatus Tests', () {
    test('Parses complete valid notification JSON', () {
      final json = {
        'id': 'notif-123',
        'type': 'MATCH_CREATED',
        'title': 'New Match Found',
        'body': 'A donor match is available for your request.',
        'referenceType': 'DONOR_MATCH',
        'referenceId': 'match-456',
        'createdAt': '2026-09-22T10:00:00.000Z',
        'readAt': null,
        'read': false,
        'deliveryStatus': 'NO_DEVICES',
      };

      final notif = AppNotification.fromJson(json);

      expect(notif.id, equals('notif-123'));
      expect(notif.type, equals(NotificationType.matchCreated));
      expect(notif.title, equals('New Match Found'));
      expect(
          notif.body, equals('A donor match is available for your request.'));
      expect(notif.referenceType, equals(NotificationReferenceType.donorMatch));
      expect(notif.referenceId, equals('match-456'));
      expect(notif.isRead, isFalse);
      expect(notif.deliveryStatus, equals(DeliveryStatus.noDevices));
      expect(notif.deliveryStatus.toServerString(), equals('NO_DEVICES'));
    });

    test('Handles unknown and edge case values defensively', () {
      final json = {
        'id': 'notif-unknown',
        'type': 'FUTURE_UNKNOWN_TYPE',
        'title': 'System Notification',
        'body': 'System announcement',
        'referenceType': 'STRANGE_REFERENCE',
        'referenceId': null,
        'createdAt': '2026-09-22T10:00:00.000Z',
        'readAt': '2026-09-22T10:30:00.000Z',
        'read': true,
        'deliveryStatus': 'MYSTERIOUS_STATUS',
      };

      final notif = AppNotification.fromJson(json);

      expect(notif.type, equals(NotificationType.unknown));
      expect(notif.referenceType, equals(NotificationReferenceType.unknown));
      expect(notif.deliveryStatus, equals(DeliveryStatus.unknown));
      expect(notif.isRead, isTrue);
      expect(notif.readAt, isNotNull);
    });

    test(
        'DeliveryStatus correctly parses all valid statuses including NO_DEVICES',
        () {
      expect(
          DeliveryStatus.fromString('PENDING'), equals(DeliveryStatus.pending));
      expect(DeliveryStatus.fromString('SENT'), equals(DeliveryStatus.sent));
      expect(
          DeliveryStatus.fromString('FAILED'), equals(DeliveryStatus.failed));
      expect(DeliveryStatus.fromString('NO_DEVICES'),
          equals(DeliveryStatus.noDevices));
      expect(
          DeliveryStatus.fromString('INVALID'), equals(DeliveryStatus.unknown));
      expect(DeliveryStatus.fromString(null), equals(DeliveryStatus.unknown));

      expect(DeliveryStatus.pending.toServerString(), equals('PENDING'));
      expect(DeliveryStatus.sent.toServerString(), equals('SENT'));
      expect(DeliveryStatus.failed.toServerString(), equals('FAILED'));
      expect(DeliveryStatus.noDevices.toServerString(), equals('NO_DEVICES'));
      expect(DeliveryStatus.unknown.toServerString(), equals('UNKNOWN'));
    });

    test('Throws FormatException when createdAt is missing', () {
      final json = {
        'id': 'notif-bad',
        'type': 'MATCH_CREATED',
        'title': 'Bad Notification',
        'body': 'Missing createdAt',
      };

      expect(() => AppNotification.fromJson(json),
          throwsA(isA<FormatException>()));
    });
  });

  group('PushNotificationService Tests', () {
    final pushService = PushNotificationService();

    test('parsePushPayload extracts fields when notificationId is present', () {
      final data = {
        'notificationId': 'nid-789',
        'type': 'MATCH_ACCEPTED',
        'referenceType': 'DONOR_MATCH',
        'referenceId': 'mid-001',
      };

      final parsed = pushService.parsePushPayload(data);

      expect(parsed, isNotNull);
      expect(parsed!['notificationId'], equals('nid-789'));
      expect(parsed['type'], equals('MATCH_ACCEPTED'));
      expect(parsed['referenceType'], equals('DONOR_MATCH'));
      expect(parsed['referenceId'], equals('mid-001'));
    });

    test('parsePushPayload returns null when notificationId is absent', () {
      final data = {
        'type': 'MATCH_ACCEPTED',
        'referenceId': 'mid-001',
      };

      expect(pushService.parsePushPayload(data), isNull);
    });

    test('isValidPayloadReference validates recognized reference types safely',
        () {
      expect(
          pushService.isValidPayloadReference({
            'referenceType': 'DONOR_MATCH',
            'referenceId': 'uuid-123',
          }),
          isTrue);

      expect(
          pushService.isValidPayloadReference({
            'referenceType': 'BLOOD_REQUEST',
            'referenceId': 'uuid-456',
          }),
          isTrue);

      // Rejects unknown reference types
      expect(
          pushService.isValidPayloadReference({
            'referenceType': 'ARBITRARY_PAGE',
            'referenceId': 'some_id',
          }),
          isFalse);

      // Rejects empty or null reference ids
      expect(
          pushService.isValidPayloadReference({
            'referenceType': 'DONOR_MATCH',
            'referenceId': '',
          }),
          isFalse);

      expect(
          pushService.isValidPayloadReference({
            'referenceType': 'DONOR_MATCH',
            'referenceId': null,
          }),
          isFalse);

      expect(pushService.isValidPayloadReference(null), isFalse);
    });

    test('formatRelativeTime produces accurate human readable relative strings',
        () {
      final now = DateTime.now();
      expect(
          PushNotificationService.formatRelativeTime(
              now.subtract(const Duration(seconds: 15))),
          equals('Just now'));
      expect(
          PushNotificationService.formatRelativeTime(
              now.subtract(const Duration(minutes: 5))),
          equals('5m ago'));
      expect(
          PushNotificationService.formatRelativeTime(
              now.subtract(const Duration(hours: 3))),
          equals('3h ago'));
      expect(
          PushNotificationService.formatRelativeTime(
              now.subtract(const Duration(days: 1))),
          equals('Yesterday'));
      expect(
          PushNotificationService.formatRelativeTime(
              now.subtract(const Duration(days: 3))),
          equals('3d ago'));
    });
  });

  group('Notification Widgets Tests', () {
    testWidgets('NotificationBellIcon renders unread badge correctly',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            appBar: AppBar(
              actions: const [
                NotificationBellIcon(initialUnreadCount: 4),
              ],
            ),
          ),
        ),
      );

      expect(find.text('4'), findsOneWidget);
      expect(find.byIcon(Icons.notifications_outlined), findsOneWidget);
    });

    testWidgets('NotificationBellIcon caps badge display at 99+',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            appBar: AppBar(
              actions: const [
                NotificationBellIcon(initialUnreadCount: 150),
              ],
            ),
          ),
        ),
      );

      expect(find.text('99+'), findsOneWidget);
    });

    testWidgets(
        'NotificationItemCard displays notification information properly',
        (tester) async {
      final notification = AppNotification(
        id: 'test-card-1',
        type: NotificationType.matchCreated,
        title: 'Emergency Donor Match',
        body: 'Urgent O+ donor required at City Hospital.',
        referenceType: NotificationReferenceType.donorMatch,
        referenceId: 'match-xyz',
        createdAt: DateTime.now().subtract(const Duration(minutes: 2)),
        isRead: false,
        deliveryStatus: DeliveryStatus.sent,
      );

      bool tapped = false;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: NotificationItemCard(
              notification: notification,
              onTap: () => tapped = true,
            ),
          ),
        ),
      );

      expect(find.text('Emergency Donor Match'), findsOneWidget);
      expect(find.text('Urgent O+ donor required at City Hospital.'),
          findsOneWidget);
      expect(find.text('2m ago'), findsOneWidget);

      await tester.tap(find.byType(NotificationItemCard));
      expect(tapped, isTrue);
    });
  });
}
