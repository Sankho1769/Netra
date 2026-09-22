import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_inventory.dart';

class FreshnessIndicator extends StatelessWidget {
  final InventoryFreshness freshness;
  final String? relativeTime;

  const FreshnessIndicator({
    super.key,
    required this.freshness,
    this.relativeTime,
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (freshness) {
      case InventoryFreshness.fresh:
        bg = NetraColors.eligibleGreenBg;
        fg = NetraColors.eligibleGreen;
        icon = Icons.bolt_rounded;
        break;
      case InventoryFreshness.recent:
        bg = const Color(0xFFE3F2FD);
        fg = const Color(0xFF1976D2);
        icon = Icons.access_time_filled_rounded;
        break;
      case InventoryFreshness.stale:
        bg = NetraColors.warningOrangeBg;
        fg = NetraColors.warningOrange;
        icon = Icons.warning_amber_rounded;
        break;
      case InventoryFreshness.unknown:
        bg = NetraColors.backgroundGray;
        fg = NetraColors.textMuted;
        icon = Icons.help_outline_rounded;
        break;
    }

    return Tooltip(
      message: freshness.description,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 12, color: fg),
            NetraSpacing.gapW4,
            Text(
              relativeTime != null
                  ? '${freshness.label} ($relativeTime)'
                  : freshness.label,
              style: NetraTypography.labelSmall.copyWith(
                color: fg,
                fontSize: 10,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
