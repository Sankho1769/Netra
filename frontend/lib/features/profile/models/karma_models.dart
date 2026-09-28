class KarmaTransactionModel {
  final String id;
  final int points;
  final String eventType;
  final String? referenceType;
  final String? referenceId;
  final String? reason;
  final DateTime createdAt;
  final int previousBalance;
  final int resultingBalance;

  KarmaTransactionModel({
    required this.id,
    required this.points,
    required this.eventType,
    this.referenceType,
    this.referenceId,
    this.reason,
    required this.createdAt,
    required this.previousBalance,
    required this.resultingBalance,
  });

  factory KarmaTransactionModel.fromJson(Map<String, dynamic> json) {
    return KarmaTransactionModel(
      id: json['id'] as String,
      points: (json['points'] as num).toInt(),
      eventType: json['eventType'] as String? ?? 'AWARD',
      referenceType: json['referenceType'] as String?,
      referenceId: json['referenceId'] as String?,
      reason: json['reason'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      previousBalance: (json['previousBalance'] as num?)?.toInt() ?? 0,
      resultingBalance: (json['resultingBalance'] as num?)?.toInt() ?? 0,
    );
  }

  bool get isPositive => points > 0;
}

class KarmaSummaryModel {
  final String userId;
  final int balance;
  final String tier;
  final int totalTransactions;
  final List<KarmaTransactionModel> recentTransactions;

  KarmaSummaryModel({
    required this.userId,
    required this.balance,
    required this.tier,
    required this.totalTransactions,
    required this.recentTransactions,
  });

  factory KarmaSummaryModel.fromJson(Map<String, dynamic> json) {
    final recentList = (json['recentTransactions'] as List<dynamic>?) ?? [];
    return KarmaSummaryModel(
      userId: json['userId'] as String? ?? '',
      balance: (json['balance'] as num?)?.toInt() ?? 0,
      tier: json['tier'] as String? ?? 'Community Member',
      totalTransactions: (json['totalTransactions'] as num?)?.toInt() ?? 0,
      recentTransactions: recentList
          .map((item) =>
              KarmaTransactionModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}
