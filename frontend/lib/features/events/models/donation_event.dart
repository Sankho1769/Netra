enum DonationEventType {
  bloodDonationCamp;

  static DonationEventType fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'BLOOD_DONATION_CAMP':
      default:
        return DonationEventType.bloodDonationCamp;
    }
  }

  String get displayName {
    switch (this) {
      case DonationEventType.bloodDonationCamp:
        return 'Blood Donation Camp';
    }
  }
}

enum DonationEventStatus {
  draft,
  pendingApproval,
  published,
  registrationClosed,
  ongoing,
  completed,
  cancelled,
  rejected;

  static DonationEventStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'PENDING_APPROVAL':
        return DonationEventStatus.pendingApproval;
      case 'PUBLISHED':
        return DonationEventStatus.published;
      case 'REGISTRATION_CLOSED':
        return DonationEventStatus.registrationClosed;
      case 'ONGOING':
        return DonationEventStatus.ongoing;
      case 'COMPLETED':
        return DonationEventStatus.completed;
      case 'CANCELLED':
        return DonationEventStatus.cancelled;
      case 'REJECTED':
        return DonationEventStatus.rejected;
      case 'DRAFT':
      default:
        return DonationEventStatus.draft;
    }
  }

  String get displayName {
    switch (this) {
      case DonationEventStatus.draft:
        return 'Draft';
      case DonationEventStatus.pendingApproval:
        return 'Pending Approval';
      case DonationEventStatus.published:
        return 'Registration Open';
      case DonationEventStatus.registrationClosed:
        return 'Registration Closed';
      case DonationEventStatus.ongoing:
        return 'Camp Ongoing';
      case DonationEventStatus.completed:
        return 'Completed';
      case DonationEventStatus.cancelled:
        return 'Cancelled';
      case DonationEventStatus.rejected:
        return 'Rejected';
    }
  }
}

class DonationEventSummary {
  final String id;
  final String bloodBankId;
  final String bloodBankName;
  final String title;
  final String? description;
  final DonationEventType eventType;
  final DonationEventStatus status;
  final String venueName;
  final String address;
  final String city;
  final String state;
  final String postalCode;
  final DateTime startAt;
  final DateTime endAt;
  final DateTime registrationOpenAt;
  final DateTime registrationCloseAt;
  final int donorCapacity;
  final int currentRegistrationCount;
  final int remainingCapacity;
  final double? distanceKm;
  final bool isRegistrationOpen;

  const DonationEventSummary({
    required this.id,
    required this.bloodBankId,
    required this.bloodBankName,
    required this.title,
    this.description,
    required this.eventType,
    required this.status,
    required this.venueName,
    required this.address,
    required this.city,
    required this.state,
    required this.postalCode,
    required this.startAt,
    required this.endAt,
    required this.registrationOpenAt,
    required this.registrationCloseAt,
    required this.donorCapacity,
    required this.currentRegistrationCount,
    required this.remainingCapacity,
    this.distanceKm,
    required this.isRegistrationOpen,
  });

  factory DonationEventSummary.fromJson(Map<String, dynamic> json) {
    return DonationEventSummary(
      id: json['id'] as String,
      bloodBankId: json['bloodBankId'] as String,
      bloodBankName: json['bloodBankName'] as String? ?? 'NETRA Blood Centre',
      title: json['title'] as String,
      description: json['description'] as String?,
      eventType: DonationEventType.fromString(json['eventType'] as String?),
      status: DonationEventStatus.fromString(json['status'] as String?),
      venueName: json['venueName'] as String,
      address: json['address'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      postalCode: json['postalCode'] as String,
      startAt: DateTime.parse(json['startAt'] as String),
      endAt: DateTime.parse(json['endAt'] as String),
      registrationOpenAt: DateTime.parse(json['registrationOpenAt'] as String),
      registrationCloseAt:
          DateTime.parse(json['registrationCloseAt'] as String),
      donorCapacity: json['donorCapacity'] as int,
      currentRegistrationCount: json['currentRegistrationCount'] as int? ?? 0,
      remainingCapacity: json['remainingCapacity'] as int? ?? 0,
      distanceKm: json['distanceKm'] != null
          ? (json['distanceKm'] as num).toDouble()
          : null,
      isRegistrationOpen: json['isRegistrationOpen'] as bool? ?? false,
    );
  }
}

class DonationEventDetail extends DonationEventSummary {
  final double? latitude;
  final double? longitude;
  final DateTime? publishedAt;
  final DateTime? cancelledAt;
  final String? cancellationReason;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const DonationEventDetail({
    required super.id,
    required super.bloodBankId,
    required super.bloodBankName,
    required super.title,
    super.description,
    required super.eventType,
    required super.status,
    required super.venueName,
    required super.address,
    required super.city,
    required super.state,
    required super.postalCode,
    required super.startAt,
    required super.endAt,
    required super.registrationOpenAt,
    required super.registrationCloseAt,
    required super.donorCapacity,
    required super.currentRegistrationCount,
    required super.remainingCapacity,
    super.distanceKm,
    required super.isRegistrationOpen,
    this.latitude,
    this.longitude,
    this.publishedAt,
    this.cancelledAt,
    this.cancellationReason,
    this.createdAt,
    this.updatedAt,
  });

  factory DonationEventDetail.fromJson(Map<String, dynamic> json) {
    return DonationEventDetail(
      id: json['id'] as String,
      bloodBankId: json['bloodBankId'] as String,
      bloodBankName: json['bloodBankName'] as String? ?? 'NETRA Blood Centre',
      title: json['title'] as String,
      description: json['description'] as String?,
      eventType: DonationEventType.fromString(json['eventType'] as String?),
      status: DonationEventStatus.fromString(json['status'] as String?),
      venueName: json['venueName'] as String,
      address: json['address'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      postalCode: json['postalCode'] as String,
      startAt: DateTime.parse(json['startAt'] as String),
      endAt: DateTime.parse(json['endAt'] as String),
      registrationOpenAt: DateTime.parse(json['registrationOpenAt'] as String),
      registrationCloseAt:
          DateTime.parse(json['registrationCloseAt'] as String),
      donorCapacity: json['donorCapacity'] as int,
      currentRegistrationCount: json['currentRegistrationCount'] as int? ?? 0,
      remainingCapacity: json['remainingCapacity'] as int? ?? 0,
      distanceKm: json['distanceKm'] != null
          ? (json['distanceKm'] as num).toDouble()
          : null,
      isRegistrationOpen: json['isRegistrationOpen'] as bool? ?? false,
      latitude: json['latitude'] != null
          ? (json['latitude'] as num).toDouble()
          : null,
      longitude: json['longitude'] != null
          ? (json['longitude'] as num).toDouble()
          : null,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
      cancelledAt: json['cancelledAt'] != null
          ? DateTime.parse(json['cancelledAt'] as String)
          : null,
      cancellationReason: json['cancellationReason'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }
}
