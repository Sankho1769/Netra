import 'package:flutter/material.dart';
import 'notification_api_service.dart';

class PushNotificationService {
  final NotificationApiService _apiService;
  String? _currentDeviceToken;

  PushNotificationService({NotificationApiService? apiService})
      : _apiService = apiService ?? NotificationApiService();

  String? get currentDeviceToken => _currentDeviceToken;

  /// Registers client device push token with the NETRA backend.
  Future<void> registerDeviceToken(String token,
      {String platform = 'ANDROID'}) async {
    try {
      _currentDeviceToken = token;
      await _apiService.registerDeviceToken(
        token: token,
        platform: platform,
        provider: 'FCM',
      );
    } catch (e) {
      debugPrint('Failed to register device token: $e');
    }
  }

  /// Revokes client device push token with the NETRA backend.
  Future<void> revokeCurrentDeviceToken() async {
    if (_currentDeviceToken != null && _currentDeviceToken!.isNotEmpty) {
      try {
        await _apiService.revokeDeviceTokenByToken(_currentDeviceToken!);
        _currentDeviceToken = null;
      } catch (e) {
        debugPrint('Failed to revoke device token: $e');
      }
    }
  }

  /// Handles incoming push payload and extracts notification reference.
  Map<String, dynamic>? parsePushPayload(Map<String, dynamic> data) {
    if (!data.containsKey('notificationId')) {
      return null;
    }
    return {
      'notificationId': data['notificationId'],
      'type': data['type'],
      'referenceType': data['referenceType'],
      'referenceId': data['referenceId'],
    };
  }

  /// Validates whether a push payload contains a recognized valid domain reference.
  bool isValidPayloadReference(Map<String, dynamic>? payload) {
    if (payload == null) return false;
    final refType = payload['referenceType'];
    final refId = payload['referenceId'];
    if (refType == null || refId == null) return false;
    if (refId.toString().trim().isEmpty) return false;
    return refType == 'DONOR_MATCH' ||
        refType == 'BLOOD_REQUEST' ||
        refType == 'DONATION' ||
        refType == 'FULFILLMENT';
  }

  /// Formats relative time (e.g. "5m ago", "2h ago", "Yesterday").
  static String formatRelativeTime(DateTime dateTime) {
    final now = DateTime.now();
    final difference = now.difference(dateTime);

    if (difference.inMinutes < 1) {
      return 'Just now';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes}m ago';
    } else if (difference.inHours < 24) {
      return '${difference.inHours}h ago';
    } else if (difference.inDays == 1) {
      return 'Yesterday';
    } else if (difference.inDays < 7) {
      return '${difference.inDays}d ago';
    } else {
      return '${dateTime.day}/${dateTime.month}/${dateTime.year}';
    }
  }
}
