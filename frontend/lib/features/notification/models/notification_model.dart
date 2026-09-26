enum NotificationType {
  matchCreated,
  matchAccepted,
  matchDeclined,
  matchExpired,
  bloodRequestCancelled,
  emergencyRequestCreated,
  donationSubmitted,
  donationVerified,
  donationRejected,
  fulfillmentCreated,
  fulfillmentStarted,
  fulfillmentCompleted,
  fulfillmentFailed,
  fulfillmentCancelled,
  unknown;

  static NotificationType fromString(String? value) {
    if (value == null) return NotificationType.unknown;
    switch (value.toUpperCase()) {
      case 'MATCH_CREATED':
        return NotificationType.matchCreated;
      case 'MATCH_ACCEPTED':
        return NotificationType.matchAccepted;
      case 'MATCH_DECLINED':
        return NotificationType.matchDeclined;
      case 'MATCH_EXPIRED':
        return NotificationType.matchExpired;
      case 'BLOOD_REQUEST_CANCELLED':
        return NotificationType.bloodRequestCancelled;
      case 'EMERGENCY_REQUEST_CREATED':
        return NotificationType.emergencyRequestCreated;
      case 'DONATION_SUBMITTED':
        return NotificationType.donationSubmitted;
      case 'DONATION_VERIFIED':
        return NotificationType.donationVerified;
      case 'DONATION_REJECTED':
        return NotificationType.donationRejected;
      case 'FULFILLMENT_CREATED':
        return NotificationType.fulfillmentCreated;
      case 'FULFILLMENT_STARTED':
        return NotificationType.fulfillmentStarted;
      case 'FULFILLMENT_COMPLETED':
        return NotificationType.fulfillmentCompleted;
      case 'FULFILLMENT_FAILED':
        return NotificationType.fulfillmentFailed;
      case 'FULFILLMENT_CANCELLED':
        return NotificationType.fulfillmentCancelled;
      default:
        return NotificationType.unknown;
    }
  }

  String toServerString() {
    switch (this) {
      case NotificationType.matchCreated:
        return 'MATCH_CREATED';
      case NotificationType.matchAccepted:
        return 'MATCH_ACCEPTED';
      case NotificationType.matchDeclined:
        return 'MATCH_DECLINED';
      case NotificationType.matchExpired:
        return 'MATCH_EXPIRED';
      case NotificationType.bloodRequestCancelled:
        return 'BLOOD_REQUEST_CANCELLED';
      case NotificationType.emergencyRequestCreated:
        return 'EMERGENCY_REQUEST_CREATED';
      case NotificationType.donationSubmitted:
        return 'DONATION_SUBMITTED';
      case NotificationType.donationVerified:
        return 'DONATION_VERIFIED';
      case NotificationType.donationRejected:
        return 'DONATION_REJECTED';
      case NotificationType.fulfillmentCreated:
        return 'FULFILLMENT_CREATED';
      case NotificationType.fulfillmentStarted:
        return 'FULFILLMENT_STARTED';
      case NotificationType.fulfillmentCompleted:
        return 'FULFILLMENT_COMPLETED';
      case NotificationType.fulfillmentFailed:
        return 'FULFILLMENT_FAILED';
      case NotificationType.fulfillmentCancelled:
        return 'FULFILLMENT_CANCELLED';
      case NotificationType.unknown:
        return 'UNKNOWN';
    }
  }
}

enum DeliveryStatus {
  pending,
  sent,
  failed,
  noDevices,
  unknown;

  static DeliveryStatus fromString(String? value) {
    if (value == null) return DeliveryStatus.unknown;
    switch (value.toUpperCase()) {
      case 'PENDING':
        return DeliveryStatus.pending;
      case 'SENT':
        return DeliveryStatus.sent;
      case 'FAILED':
        return DeliveryStatus.failed;
      case 'NO_DEVICES':
        return DeliveryStatus.noDevices;
      default:
        return DeliveryStatus.unknown;
    }
  }

  String toServerString() {
    switch (this) {
      case DeliveryStatus.pending:
        return 'PENDING';
      case DeliveryStatus.sent:
        return 'SENT';
      case DeliveryStatus.failed:
        return 'FAILED';
      case DeliveryStatus.noDevices:
        return 'NO_DEVICES';
      case DeliveryStatus.unknown:
        return 'UNKNOWN';
    }
  }
}

enum NotificationReferenceType {
  donorMatch,
  bloodRequest,
  donation,
  fulfillment,
  unknown;

  static NotificationReferenceType? fromString(String? value) {
    if (value == null) return null;
    switch (value.toUpperCase()) {
      case 'DONOR_MATCH':
        return NotificationReferenceType.donorMatch;
      case 'BLOOD_REQUEST':
        return NotificationReferenceType.bloodRequest;
      case 'DONATION':
        return NotificationReferenceType.donation;
      case 'FULFILLMENT':
        return NotificationReferenceType.fulfillment;
      default:
        return NotificationReferenceType.unknown;
    }
  }

  String? toServerString() {
    switch (this) {
      case NotificationReferenceType.donorMatch:
        return 'DONOR_MATCH';
      case NotificationReferenceType.bloodRequest:
        return 'BLOOD_REQUEST';
      case NotificationReferenceType.donation:
        return 'DONATION';
      case NotificationReferenceType.fulfillment:
        return 'FULFILLMENT';
      case NotificationReferenceType.unknown:
        return 'UNKNOWN';
    }
  }
}

class AppNotification {
  final String id;
  final NotificationType type;
  final String title;
  final String body;
  final NotificationReferenceType? referenceType;
  final String? referenceId;
  final DateTime createdAt;
  final DateTime? readAt;
  final bool isRead;
  final DeliveryStatus deliveryStatus;

  const AppNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    this.referenceType,
    this.referenceId,
    required this.createdAt,
    this.readAt,
    required this.isRead,
    required this.deliveryStatus,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    final rawCreatedAt = json['createdAt'] as String?;
    if (rawCreatedAt == null) {
      throw const FormatException(
          'Missing required createdAt timestamp in Notification');
    }

    final rawReadAt = json['readAt'] as String?;
    final readAt = rawReadAt != null ? DateTime.parse(rawReadAt) : null;
    final isRead = json['read'] as bool? ?? (readAt != null);

    return AppNotification(
      id: json['id'] as String? ?? '',
      type: NotificationType.fromString(json['type'] as String?),
      title: json['title'] as String? ?? '',
      body: json['body'] as String? ?? '',
      referenceType: NotificationReferenceType.fromString(
          json['referenceType'] as String?),
      referenceId: json['referenceId'] as String?,
      createdAt: DateTime.parse(rawCreatedAt),
      readAt: readAt,
      isRead: isRead,
      deliveryStatus:
          DeliveryStatus.fromString(json['deliveryStatus'] as String?),
    );
  }

  AppNotification copyWith({
    String? id,
    NotificationType? type,
    String? title,
    String? body,
    NotificationReferenceType? referenceType,
    String? referenceId,
    DateTime? createdAt,
    DateTime? readAt,
    bool? isRead,
    DeliveryStatus? deliveryStatus,
  }) {
    return AppNotification(
      id: id ?? this.id,
      type: type ?? this.type,
      title: title ?? this.title,
      body: body ?? this.body,
      referenceType: referenceType ?? this.referenceType,
      referenceId: referenceId ?? this.referenceId,
      createdAt: createdAt ?? this.createdAt,
      readAt: readAt ?? this.readAt,
      isRead: isRead ?? this.isRead,
      deliveryStatus: deliveryStatus ?? this.deliveryStatus,
    );
  }
}

class DeviceTokenRegistration {
  final String token;
  final String platform;
  final String provider;

  const DeviceTokenRegistration({
    required this.token,
    this.platform = 'ANDROID',
    this.provider = 'FCM',
  });

  Map<String, dynamic> toJson() => {
        'token': token,
        'platform': platform,
        'provider': provider,
      };
}

class DeviceToken {
  final String id;
  final String platform;
  final String provider;
  final bool active;
  final DateTime createdAt;
  final DateTime lastSeenAt;

  const DeviceToken({
    required this.id,
    required this.platform,
    required this.provider,
    required this.active,
    required this.createdAt,
    required this.lastSeenAt,
  });

  factory DeviceToken.fromJson(Map<String, dynamic> json) {
    return DeviceToken(
      id: json['id'] as String? ?? '',
      platform: json['platform'] as String? ?? 'ANDROID',
      provider: json['provider'] as String? ?? 'FCM',
      active: (json['active'] ?? json['isActive']) as bool? ?? true,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      lastSeenAt: json['lastSeenAt'] != null
          ? DateTime.parse(json['lastSeenAt'] as String)
          : DateTime.now(),
    );
  }
}
