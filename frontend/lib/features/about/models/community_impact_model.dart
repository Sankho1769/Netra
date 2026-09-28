class CommunityImpactModel {
  final int totalVerifiedDonations;
  final int totalUnitsCollected;
  final int activeDonorsCount;
  final int fulfilledRequestsCount;
  final bool hasData;
  final String? notice;

  const CommunityImpactModel({
    required this.totalVerifiedDonations,
    required this.totalUnitsCollected,
    required this.activeDonorsCount,
    required this.fulfilledRequestsCount,
    required this.hasData,
    this.notice,
  });

  factory CommunityImpactModel.fromJson(Map<String, dynamic> json) {
    return CommunityImpactModel(
      totalVerifiedDonations: (json['totalVerifiedDonations'] as num?)?.toInt() ?? 0,
      totalUnitsCollected: (json['totalUnitsCollected'] as num?)?.toInt() ?? 0,
      activeDonorsCount: (json['activeDonorsCount'] as num?)?.toInt() ?? 0,
      fulfilledRequestsCount: (json['fulfilledRequestsCount'] as num?)?.toInt() ?? 0,
      hasData: json['hasData'] as bool? ?? false,
      notice: json['notice'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'totalVerifiedDonations': totalVerifiedDonations,
      'totalUnitsCollected': totalUnitsCollected,
      'activeDonorsCount': activeDonorsCount,
      'fulfilledRequestsCount': fulfilledRequestsCount,
      'hasData': hasData,
      'notice': notice,
    };
  }
}
