import 'package:flutter/material.dart';

enum BloodRequestUrgency {
  normal,
  urgent,
  critical;

  static BloodRequestUrgency fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'CRITICAL':
        return BloodRequestUrgency.critical;
      case 'URGENT':
        return BloodRequestUrgency.urgent;
      case 'NORMAL':
      default:
        return BloodRequestUrgency.normal;
    }
  }

  String get displayName {
    switch (this) {
      case BloodRequestUrgency.normal:
        return 'Normal';
      case BloodRequestUrgency.urgent:
        return 'Urgent';
      case BloodRequestUrgency.critical:
        return 'Critical';
    }
  }

  Color get color {
    switch (this) {
      case BloodRequestUrgency.normal:
        return const Color(0xFF2563EB); // Blue
      case BloodRequestUrgency.urgent:
        return const Color(0xFFD97706); // Amber / Orange
      case BloodRequestUrgency.critical:
        return const Color(0xFFDC2626); // Red
    }
  }
}

enum BloodRequestStatus {
  open,
  fulfilled,
  cancelled,
  expired;

  static BloodRequestStatus fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'FULFILLED':
        return BloodRequestStatus.fulfilled;
      case 'CANCELLED':
        return BloodRequestStatus.cancelled;
      case 'EXPIRED':
        return BloodRequestStatus.expired;
      case 'OPEN':
      default:
        return BloodRequestStatus.open;
    }
  }

  String get displayName {
    switch (this) {
      case BloodRequestStatus.open:
        return 'Open';
      case BloodRequestStatus.fulfilled:
        return 'Fulfilled';
      case BloodRequestStatus.cancelled:
        return 'Cancelled';
      case BloodRequestStatus.expired:
        return 'Expired';
    }
  }

  bool get isDiscoverable => this == BloodRequestStatus.open;
  bool get isTerminal => this != BloodRequestStatus.open;

  Color get color {
    switch (this) {
      case BloodRequestStatus.open:
        return const Color(0xFF16A34A); // Green
      case BloodRequestStatus.fulfilled:
        return const Color(0xFF0284C7); // Sky Blue
      case BloodRequestStatus.cancelled:
        return const Color(0xFF6B7280); // Grey
      case BloodRequestStatus.expired:
        return const Color(0xFF991B1B); // Dark Red
    }
  }
}

class BloodRequestSummary {
  final String id;
  final String bloodGroup;
  final int unitsRequired;
  final BloodRequestUrgency urgency;
  final BloodRequestStatus status;
  final String hospitalName;
  final String city;
  final String state;
  final DateTime requiredBy;
  final double? distanceKm;
  final DateTime createdAt;

  const BloodRequestSummary({
    required this.id,
    required this.bloodGroup,
    required this.unitsRequired,
    required this.urgency,
    required this.status,
    required this.hospitalName,
    required this.city,
    required this.state,
    required this.requiredBy,
    this.distanceKm,
    required this.createdAt,
  });

  factory BloodRequestSummary.fromJson(Map<String, dynamic> json) {
    return BloodRequestSummary(
      id: json['id'] as String,
      bloodGroup: json['bloodGroup'] as String,
      unitsRequired: json['unitsRequired'] as int,
      urgency: BloodRequestUrgency.fromString(json['urgency'] as String?),
      status: BloodRequestStatus.fromString(json['status'] as String?),
      hospitalName: json['hospitalName'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      requiredBy: DateTime.parse(json['requiredBy'] as String),
      distanceKm: json['distanceKm'] != null ? (json['distanceKm'] as num).toDouble() : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class BloodRequestDetail extends BloodRequestSummary {
  final String? requesterUserId;
  final bool isOwner;
  final bool canManage;
  final String hospitalAddress;
  final String postalCode;
  final double? latitude;
  final double? longitude;
  final String? description;
  final DateTime updatedAt;
  final DateTime? cancelledAt;
  final String? cancellationReason;
  final DateTime? fulfilledAt;

  const BloodRequestDetail({
    required super.id,
    required super.bloodGroup,
    required super.unitsRequired,
    required super.urgency,
    required super.status,
    required super.hospitalName,
    required super.city,
    required super.state,
    required super.requiredBy,
    super.distanceKm,
    required super.createdAt,
    this.requesterUserId,
    this.isOwner = false,
    this.canManage = false,
    required this.hospitalAddress,
    required this.postalCode,
    this.latitude,
    this.longitude,
    this.description,
    required this.updatedAt,
    this.cancelledAt,
    this.cancellationReason,
    this.fulfilledAt,
  });

  factory BloodRequestDetail.fromJson(Map<String, dynamic> json) {
    return BloodRequestDetail(
      id: json['id'] as String,
      bloodGroup: json['bloodGroup'] as String,
      unitsRequired: json['unitsRequired'] as int,
      urgency: BloodRequestUrgency.fromString(json['urgency'] as String?),
      status: BloodRequestStatus.fromString(json['status'] as String?),
      hospitalName: json['hospitalName'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      requiredBy: DateTime.parse(json['requiredBy'] as String),
      distanceKm: json['distanceKm'] != null ? (json['distanceKm'] as num).toDouble() : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
      requesterUserId: json['requesterUserId'] as String?,
      isOwner: json['isOwner'] as bool? ?? false,
      canManage: json['canManage'] as bool? ?? false,
      hospitalAddress: json['hospitalAddress'] as String,
      postalCode: json['postalCode'] as String,
      latitude: json['latitude'] != null ? (json['latitude'] as num).toDouble() : null,
      longitude: json['longitude'] != null ? (json['longitude'] as num).toDouble() : null,
      description: json['description'] as String?,
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      cancelledAt: json['cancelledAt'] != null ? DateTime.parse(json['cancelledAt'] as String) : null,
      cancellationReason: json['cancellationReason'] as String?,
      fulfilledAt: json['fulfilledAt'] != null ? DateTime.parse(json['fulfilledAt'] as String) : null,
    );
  }
}
