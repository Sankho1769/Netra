enum EventRegistrationStatus {
  registered,
  waitlisted,
  cancelled,
  checkedIn,
  completed,
  noShow,
  rejected;

  static EventRegistrationStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'WAITLISTED':
        return EventRegistrationStatus.waitlisted;
      case 'CANCELLED':
        return EventRegistrationStatus.cancelled;
      case 'CHECKED_IN':
        return EventRegistrationStatus.checkedIn;
      case 'COMPLETED':
        return EventRegistrationStatus.completed;
      case 'NO_SHOW':
        return EventRegistrationStatus.noShow;
      case 'REJECTED':
        return EventRegistrationStatus.rejected;
      case 'REGISTERED':
      default:
        return EventRegistrationStatus.registered;
    }
  }

  String get displayName {
    switch (this) {
      case EventRegistrationStatus.registered:
        return 'Registered';
      case EventRegistrationStatus.waitlisted:
        return 'Waitlisted';
      case EventRegistrationStatus.cancelled:
        return 'Cancelled';
      case EventRegistrationStatus.checkedIn:
        return 'Checked In';
      case EventRegistrationStatus.completed:
        return 'Donation Completed';
      case EventRegistrationStatus.noShow:
        return 'No Show';
      case EventRegistrationStatus.rejected:
        return 'Ineligible';
    }
  }

  bool get isActiveSlot => this == registered || this == checkedIn;
}

class EventRegistration {
  final String id;
  final String eventId;
  final String eventTitle;
  final String donorUserId;
  final EventRegistrationStatus status;
  final DateTime registeredAt;
  final DateTime? cancelledAt;
  final DateTime? checkedInAt;
  final DateTime? completedAt;

  const EventRegistration({
    required this.id,
    required this.eventId,
    required this.eventTitle,
    required this.donorUserId,
    required this.status,
    required this.registeredAt,
    this.cancelledAt,
    this.checkedInAt,
    this.completedAt,
  });

  factory EventRegistration.fromJson(Map<String, dynamic> json) {
    return EventRegistration(
      id: json['id'] as String,
      eventId: json['eventId'] as String,
      eventTitle: json['eventTitle'] as String? ?? 'Blood Donation Camp',
      donorUserId: json['donorUserId'] as String,
      status: EventRegistrationStatus.fromString(json['status'] as String?),
      registeredAt: DateTime.parse(json['registeredAt'] as String),
      cancelledAt: json['cancelledAt'] != null
          ? DateTime.parse(json['cancelledAt'] as String)
          : null,
      checkedInAt: json['checkedInAt'] != null
          ? DateTime.parse(json['checkedInAt'] as String)
          : null,
      completedAt: json['completedAt'] != null
          ? DateTime.parse(json['completedAt'] as String)
          : null,
    );
  }
}

class EventAttendee {
  final String registrationId;
  final String donorUserId;
  final String donorName;
  final EventRegistrationStatus status;
  final DateTime registeredAt;
  final DateTime? checkedInAt;

  const EventAttendee({
    required this.registrationId,
    required this.donorUserId,
    required this.donorName,
    required this.status,
    required this.registeredAt,
    this.checkedInAt,
  });

  factory EventAttendee.fromJson(Map<String, dynamic> json) {
    return EventAttendee(
      registrationId: json['registrationId'] as String,
      donorUserId: json['donorUserId'] as String,
      donorName: json['donorName'] as String? ?? 'Anonymous Donor',
      status: EventRegistrationStatus.fromString(json['status'] as String?),
      registeredAt: DateTime.parse(json['registeredAt'] as String),
      checkedInAt: json['checkedInAt'] != null
          ? DateTime.parse(json['checkedInAt'] as String)
          : null,
    );
  }
}
