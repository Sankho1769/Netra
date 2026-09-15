enum InventoryFreshness {
  fresh,
  recent,
  stale,
  unknown;

  static InventoryFreshness fromString(String? value) {
    switch (value?.toUpperCase()) {
      case 'FRESH':
        return InventoryFreshness.fresh;
      case 'RECENT':
        return InventoryFreshness.recent;
      case 'STALE':
        return InventoryFreshness.stale;
      case 'UNKNOWN':
      default:
        return InventoryFreshness.unknown;
    }
  }

  String get label {
    switch (this) {
      case InventoryFreshness.fresh:
        return 'Fresh';
      case InventoryFreshness.recent:
        return 'Recent';
      case InventoryFreshness.stale:
        return 'Stale Stock';
      case InventoryFreshness.unknown:
        return 'Unknown';
    }
  }

  String get description {
    switch (this) {
      case InventoryFreshness.fresh:
        return 'Updated within 2 hours';
      case InventoryFreshness.recent:
        return 'Updated 2 to 6 hours ago';
      case InventoryFreshness.stale:
        return 'Updated more than 6 hours ago';
      case InventoryFreshness.unknown:
        return 'No recent update reported';
    }
  }
}

class BloodInventoryItem {
  final String id;
  final String bloodBankId;
  final String bloodGroup;
  final int unitsAvailable;
  final InventoryFreshness freshness;
  final DateTime? lastUpdatedAt;

  BloodInventoryItem({
    required this.id,
    required this.bloodBankId,
    required this.bloodGroup,
    required this.unitsAvailable,
    required this.freshness,
    this.lastUpdatedAt,
  });

  factory BloodInventoryItem.fromJson(Map<String, dynamic> json) {
    return BloodInventoryItem(
      id: json['id'] as String,
      bloodBankId: json['bloodBankId'] as String,
      bloodGroup: json['bloodGroup'] as String,
      unitsAvailable: (json['unitsAvailable'] as num?)?.toInt() ?? 0,
      freshness: InventoryFreshness.fromString(json['freshness'] as String?),
      lastUpdatedAt: json['lastUpdatedAt'] != null
          ? DateTime.tryParse(json['lastUpdatedAt'] as String)
          : null,
    );
  }

  bool get isAvailable => unitsAvailable > 0;

  String get formattedRelativeTime {
    if (lastUpdatedAt == null) return 'No timestamp';
    final diff = DateTime.now().difference(lastUpdatedAt!);
    if (diff.inMinutes < 1) {
      return 'Just now';
    } else if (diff.inMinutes < 60) {
      return '${diff.inMinutes}m ago';
    } else if (diff.inHours < 24) {
      return '${diff.inHours}h ago';
    } else {
      return '${diff.inDays}d ago';
    }
  }
}
