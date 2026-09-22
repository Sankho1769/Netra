import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_inventory.dart';
import 'freshness_indicator.dart';

class BloodAvailabilityCard extends StatelessWidget {
  final BloodInventoryItem item;

  const BloodAvailabilityCard({
    super.key,
    required this.item,
  });

  @override
  Widget build(BuildContext context) {
    final hasStock = item.unitsAvailable > 0;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: NetraColors.surfaceWhite,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        border: Border.all(
          color: hasStock ? NetraColors.borderSubtle : NetraColors.borderGray,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: hasStock
                      ? NetraColors.backgroundRed
                      : NetraColors.backgroundGray,
                  borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
                ),
                child: Text(
                  item.bloodGroup,
                  style: NetraTypography.titleMedium.copyWith(
                    color: hasStock
                        ? NetraColors.primaryRed
                        : NetraColors.textSecondary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              FreshnessIndicator(
                freshness: item.freshness,
                relativeTime: item.formattedRelativeTime,
              ),
            ],
          ),
          NetraSpacing.gapH8,
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '${item.unitsAvailable}',
                style: NetraTypography.headlineMedium.copyWith(
                  fontWeight: FontWeight.w700,
                  color: hasStock
                      ? NetraColors.textPrimary
                      : NetraColors.textMuted,
                ),
              ),
              NetraSpacing.gapW4,
              Text(
                item.unitsAvailable == 1 ? 'unit available' : 'units available',
                style: NetraTypography.bodySmall.copyWith(
                  color: NetraColors.textSecondary,
                ),
              ),
            ],
          ),
          if (!hasStock)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                'Currently Out of Stock',
                style: NetraTypography.labelSmall.copyWith(
                  color: NetraColors.ineligibleRed,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
