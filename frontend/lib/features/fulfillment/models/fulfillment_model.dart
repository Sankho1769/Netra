import 'package:flutter/foundation.dart';

enum FulfillmentStatus {
  ready,
  inProgress,
  fulfilled,
  cancelled,
  failed,
  unknown;

  static FulfillmentStatus fromString(String? value) {
    if (value == null) return FulfillmentStatus.unknown;
    switch (value.toUpperCase()) {
      case 'READY':
        return FulfillmentStatus.ready;
      case 'IN_PROGRESS':
        return FulfillmentStatus.inProgress;
      case 'FULFILLED':
        return FulfillmentStatus.fulfilled;
      case 'CANCELLED':
        return FulfillmentStatus.cancelled;
      case 'FAILED':
        return FulfillmentStatus.failed;
      default:
        return FulfillmentStatus.unknown;
    }
  }

  String toServerString() {
    switch (this) {
      case FulfillmentStatus.ready:
        return 'READY';
      case FulfillmentStatus.inProgress:
        return 'IN_PROGRESS';
      case FulfillmentStatus.fulfilled:
        return 'FULFILLED';
      case FulfillmentStatus.cancelled:
        return 'CANCELLED';
      case FulfillmentStatus.failed:
        return 'FAILED';
      case FulfillmentStatus.unknown:
        return 'UNKNOWN';
    }
  }

  String get displayName {
    switch (this) {
      case FulfillmentStatus.ready:
        return 'Ready';
      case FulfillmentStatus.inProgress:
        return 'In Progress';
      case FulfillmentStatus.fulfilled:
        return 'Fulfilled';
      case FulfillmentStatus.cancelled:
        return 'Cancelled';
      case FulfillmentStatus.failed:
        return 'Failed';
      case FulfillmentStatus.unknown:
        return 'Unknown';
    }
  }

  bool get isTerminal =>
      this == FulfillmentStatus.fulfilled ||
      this == FulfillmentStatus.cancelled ||
      this == FulfillmentStatus.failed;
}

@immutable
class FulfillmentModel {
  final String id;
  final String bloodRequestId;
  final String donationId;
  final int units;
  final FulfillmentStatus status;
  final String createdByUserId;
  final String? startedByUserId;
  final String? completedByUserId;
  final String? failedByUserId;
  final String? cancelledByUserId;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final DateTime? failedAt;
  final DateTime? cancelledAt;
  final String? failureReason;
  final String? cancellationReason;
  final String? notes;
  final DateTime createdAt;
  final DateTime updatedAt;

  // Detail / Presentation metadata
  final String? hospitalName;
  final String? hospitalAddress;
  final String? city;
  final String? state;
  final String? requestedBloodGroup;
  final int? unitsRequired;
  final int? unitsFulfilled;
  final int? remainingUnits;
  final String? donorUserId;
  final String? donorBloodGroup;
  final String? donationDate;
  final bool isRequester;
  final bool isDonor;
  final bool canManage;

  const FulfillmentModel({
    required this.id,
    required this.bloodRequestId,
    required this.donationId,
    required this.units,
    required this.status,
    required this.createdByUserId,
    this.startedByUserId,
    this.completedByUserId,
    this.failedByUserId,
    this.cancelledByUserId,
    this.startedAt,
    this.completedAt,
    this.failedAt,
    this.cancelledAt,
    this.failureReason,
    this.cancellationReason,
    this.notes,
    required this.createdAt,
    required this.updatedAt,
    this.hospitalName,
    this.hospitalAddress,
    this.city,
    this.state,
    this.requestedBloodGroup,
    this.unitsRequired,
    this.unitsFulfilled,
    this.remainingUnits,
    this.donorUserId,
    this.donorBloodGroup,
    this.donationDate,
    this.isRequester = false,
    this.isDonor = false,
    this.canManage = false,
  });

  factory FulfillmentModel.fromJson(Map<String, dynamic> json) {
    return FulfillmentModel(
      id: json['id'] as String,
      bloodRequestId: json['bloodRequestId'] as String,
      donationId: json['donationId'] as String,
      units: (json['units'] as num?)?.toInt() ?? 1,
      status: FulfillmentStatus.fromString(json['status'] as String?),
      createdByUserId: json['createdByUserId'] as String? ?? '',
      startedByUserId: json['startedByUserId'] as String?,
      completedByUserId: json['completedByUserId'] as String?,
      failedByUserId: json['failedByUserId'] as String?,
      cancelledByUserId: json['cancelledByUserId'] as String?,
      startedAt: json['startedAt'] != null
          ? DateTime.tryParse(json['startedAt'] as String)
          : null,
      completedAt: json['completedAt'] != null
          ? DateTime.tryParse(json['completedAt'] as String)
          : null,
      failedAt: json['failedAt'] != null
          ? DateTime.tryParse(json['failedAt'] as String)
          : null,
      cancelledAt: json['cancelledAt'] != null
          ? DateTime.tryParse(json['cancelledAt'] as String)
          : null,
      failureReason: json['failureReason'] as String?,
      cancellationReason: json['cancellationReason'] as String?,
      notes: json['notes'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : DateTime.now(),
      hospitalName: json['hospitalName'] as String?,
      hospitalAddress: json['hospitalAddress'] as String?,
      city: json['city'] as String?,
      state: json['state'] as String?,
      requestedBloodGroup: json['requestedBloodGroup'] as String?,
      unitsRequired: (json['unitsRequired'] as num?)?.toInt(),
      unitsFulfilled: (json['unitsFulfilled'] as num?)?.toInt(),
      remainingUnits: (json['remainingUnits'] as num?)?.toInt(),
      donorUserId: json['donorUserId'] as String?,
      donorBloodGroup: json['donorBloodGroup'] as String?,
      donationDate: json['donationDate'] as String?,
      isRequester: json['isRequester'] as bool? ?? false,
      isDonor: json['isDonor'] as bool? ?? false,
      canManage: json['canManage'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'bloodRequestId': bloodRequestId,
      'donationId': donationId,
      'units': units,
      'status': status.toServerString(),
      'createdByUserId': createdByUserId,
      'startedByUserId': startedByUserId,
      'completedByUserId': completedByUserId,
      'failedByUserId': failedByUserId,
      'cancelledByUserId': cancelledByUserId,
      'startedAt': startedAt?.toIso8601String(),
      'completedAt': completedAt?.toIso8601String(),
      'failedAt': failedAt?.toIso8601String(),
      'cancelledAt': cancelledAt?.toIso8601String(),
      'failureReason': failureReason,
      'cancellationReason': cancellationReason,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
      'hospitalName': hospitalName,
      'hospitalAddress': hospitalAddress,
      'city': city,
      'state': state,
      'requestedBloodGroup': requestedBloodGroup,
      'unitsRequired': unitsRequired,
      'unitsFulfilled': unitsFulfilled,
      'remainingUnits': remainingUnits,
      'donorUserId': donorUserId,
      'donorBloodGroup': donorBloodGroup,
      'donationDate': donationDate,
      'isRequester': isRequester,
      'isDonor': isDonor,
      'canManage': canManage,
    };
  }

  FulfillmentModel copyWith({
    String? id,
    String? bloodRequestId,
    String? donationId,
    int? units,
    FulfillmentStatus? status,
    String? createdByUserId,
    String? startedByUserId,
    String? completedByUserId,
    String? failedByUserId,
    String? cancelledByUserId,
    DateTime? startedAt,
    DateTime? completedAt,
    DateTime? failedAt,
    DateTime? cancelledAt,
    String? failureReason,
    String? cancellationReason,
    String? notes,
    DateTime? createdAt,
    DateTime? updatedAt,
    String? hospitalName,
    String? hospitalAddress,
    String? city,
    String? state,
    String? requestedBloodGroup,
    int? unitsRequired,
    int? unitsFulfilled,
    int? remainingUnits,
    String? donorUserId,
    String? donorBloodGroup,
    String? donationDate,
    bool? isRequester,
    bool? isDonor,
    bool? canManage,
  }) {
    return FulfillmentModel(
      id: id ?? this.id,
      bloodRequestId: bloodRequestId ?? this.bloodRequestId,
      donationId: donationId ?? this.donationId,
      units: units ?? this.units,
      status: status ?? this.status,
      createdByUserId: createdByUserId ?? this.createdByUserId,
      startedByUserId: startedByUserId ?? this.startedByUserId,
      completedByUserId: completedByUserId ?? this.completedByUserId,
      failedByUserId: failedByUserId ?? this.failedByUserId,
      cancelledByUserId: cancelledByUserId ?? this.cancelledByUserId,
      startedAt: startedAt ?? this.startedAt,
      completedAt: completedAt ?? this.completedAt,
      failedAt: failedAt ?? this.failedAt,
      cancelledAt: cancelledAt ?? this.cancelledAt,
      failureReason: failureReason ?? this.failureReason,
      cancellationReason: cancellationReason ?? this.cancellationReason,
      notes: notes ?? this.notes,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      hospitalName: hospitalName ?? this.hospitalName,
      hospitalAddress: hospitalAddress ?? this.hospitalAddress,
      city: city ?? this.city,
      state: state ?? this.state,
      requestedBloodGroup: requestedBloodGroup ?? this.requestedBloodGroup,
      unitsRequired: unitsRequired ?? this.unitsRequired,
      unitsFulfilled: unitsFulfilled ?? this.unitsFulfilled,
      remainingUnits: remainingUnits ?? this.remainingUnits,
      donorUserId: donorUserId ?? this.donorUserId,
      donorBloodGroup: donorBloodGroup ?? this.donorBloodGroup,
      donationDate: donationDate ?? this.donationDate,
      isRequester: isRequester ?? this.isRequester,
      isDonor: isDonor ?? this.isDonor,
      canManage: canManage ?? this.canManage,
    );
  }
}

class CreateFulfillmentDto {
  final String bloodRequestId;
  final String donationId;
  final int units;
  final String? notes;

  const CreateFulfillmentDto({
    required this.bloodRequestId,
    required this.donationId,
    this.units = 1,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'bloodRequestId': bloodRequestId,
      'donationId': donationId,
      'units': units,
      if (notes != null) 'notes': notes,
    };
  }
}

class FailFulfillmentDto {
  final String failureReason;
  final String? notes;

  const FailFulfillmentDto({
    required this.failureReason,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'failureReason': failureReason,
      if (notes != null) 'notes': notes,
    };
  }
}

class CancelFulfillmentDto {
  final String cancellationReason;
  final String? notes;

  const CancelFulfillmentDto({
    required this.cancellationReason,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    return {
      'cancellationReason': cancellationReason,
      if (notes != null) 'notes': notes,
    };
  }
}
