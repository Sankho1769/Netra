class CommunityMetricPoint {
  final String date;
  final int bloodRequestsReceived;
  final int bloodRequestsFulfilled;
  final int donationsRecorded;
  final int unitsCollected;
  final int emergencyRequests;
  final int emergencyRequestsFulfilled;

  const CommunityMetricPoint({
    required this.date,
    required this.bloodRequestsReceived,
    required this.bloodRequestsFulfilled,
    required this.donationsRecorded,
    required this.unitsCollected,
    required this.emergencyRequests,
    required this.emergencyRequestsFulfilled,
  });

  factory CommunityMetricPoint.fromJson(Map<String, dynamic> json) {
    return CommunityMetricPoint(
      date: json['date'] as String? ?? '',
      bloodRequestsReceived:
          (json['bloodRequestsReceived'] as num?)?.toInt() ?? 0,
      bloodRequestsFulfilled:
          (json['bloodRequestsFulfilled'] as num?)?.toInt() ?? 0,
      donationsRecorded: (json['donationsRecorded'] as num?)?.toInt() ?? 0,
      unitsCollected: (json['unitsCollected'] as num?)?.toInt() ?? 0,
      emergencyRequests: (json['emergencyRequests'] as num?)?.toInt() ?? 0,
      emergencyRequestsFulfilled:
          (json['emergencyRequestsFulfilled'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
        'date': date,
        'bloodRequestsReceived': bloodRequestsReceived,
        'bloodRequestsFulfilled': bloodRequestsFulfilled,
        'donationsRecorded': donationsRecorded,
        'unitsCollected': unitsCollected,
        'emergencyRequests': emergencyRequests,
        'emergencyRequestsFulfilled': emergencyRequestsFulfilled,
      };
}

class CommunityTimeSeriesModel {
  final int days;
  final String? startDate;
  final String? endDate;
  final List<CommunityMetricPoint> dataPoints;
  final int totalRequestsReceived;
  final int totalRequestsFulfilled;
  final int totalDonations;
  final int totalUnitsCollected;
  final int totalEmergencyRequests;
  final int totalEmergencyFulfilled;
  final int activeDonors;
  final int verifiedCenters;
  final bool hasData;
  final String? emptyStateMessage;

  const CommunityTimeSeriesModel({
    required this.days,
    this.startDate,
    this.endDate,
    required this.dataPoints,
    required this.totalRequestsReceived,
    required this.totalRequestsFulfilled,
    required this.totalDonations,
    required this.totalUnitsCollected,
    required this.totalEmergencyRequests,
    required this.totalEmergencyFulfilled,
    required this.activeDonors,
    required this.verifiedCenters,
    required this.hasData,
    this.emptyStateMessage,
  });

  factory CommunityTimeSeriesModel.fromJson(Map<String, dynamic> json) {
    final rawPoints = json['dataPoints'] as List<dynamic>? ?? [];
    return CommunityTimeSeriesModel(
      days: (json['days'] as num?)?.toInt() ?? 30,
      startDate: json['startDate'] as String?,
      endDate: json['endDate'] as String?,
      dataPoints: rawPoints
          .map((p) => CommunityMetricPoint.fromJson(p as Map<String, dynamic>))
          .toList(),
      totalRequestsReceived:
          (json['totalRequestsReceived'] as num?)?.toInt() ?? 0,
      totalRequestsFulfilled:
          (json['totalRequestsFulfilled'] as num?)?.toInt() ?? 0,
      totalDonations: (json['totalDonations'] as num?)?.toInt() ?? 0,
      totalUnitsCollected: (json['totalUnitsCollected'] as num?)?.toInt() ?? 0,
      totalEmergencyRequests:
          (json['totalEmergencyRequests'] as num?)?.toInt() ?? 0,
      totalEmergencyFulfilled:
          (json['totalEmergencyFulfilled'] as num?)?.toInt() ?? 0,
      activeDonors: (json['activeDonors'] as num?)?.toInt() ?? 0,
      verifiedCenters: (json['verifiedCenters'] as num?)?.toInt() ?? 0,
      hasData: json['hasData'] as bool? ?? false,
      emptyStateMessage: json['emptyStateMessage'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
        'days': days,
        if (startDate != null) 'startDate': startDate,
        if (endDate != null) 'endDate': endDate,
        'dataPoints': dataPoints.map((p) => p.toJson()).toList(),
        'totalRequestsReceived': totalRequestsReceived,
        'totalRequestsFulfilled': totalRequestsFulfilled,
        'totalDonations': totalDonations,
        'totalUnitsCollected': totalUnitsCollected,
        'totalEmergencyRequests': totalEmergencyRequests,
        'totalEmergencyFulfilled': totalEmergencyFulfilled,
        'activeDonors': activeDonors,
        'verifiedCenters': verifiedCenters,
        'hasData': hasData,
        if (emptyStateMessage != null) 'emptyStateMessage': emptyStateMessage,
      };
}
