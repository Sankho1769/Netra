import 'package:flutter/material.dart';
import '../../../common/widgets/common_widgets.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_bank.dart';
import 'verification_badge.dart';

class BloodBankCard extends StatelessWidget {
  final BloodBankSummary bank;
  final VoidCallback onTap;

  const BloodBankCard({
    super.key,
    required this.bank,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isOpen = bank.operatingStatus == BloodBankOperatingStatus.open;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
      child: NetraCard.outlined(
        padding: NetraSpacing.cardPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: NetraSpacing.paddingSm,
                  decoration: BoxDecoration(
                    color: NetraColors.backgroundRed,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
                  ),
                  child: const Icon(
                    Icons.local_hospital_rounded,
                    color: NetraColors.primaryRed,
                    size: 22,
                  ),
                ),
                NetraSpacing.gapW12,
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        bank.name,
                        style: NetraTypography.titleMedium,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      NetraSpacing.gapH4,
                      Text(
                        '${bank.address}, ${bank.city}',
                        style: NetraTypography.bodySmall.copyWith(color: NetraColors.textSecondary),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            NetraSpacing.gapH12,
            Row(
              children: [
                if (bank.distanceKm != null) ...[
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: NetraColors.backgroundGray,
                      borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
                      border: Border.all(color: NetraColors.borderSubtle),
                    ),
                    child: Text(
                      bank.formattedDistance,
                      style: NetraTypography.labelSmall.copyWith(color: NetraColors.textPrimary),
                    ),
                  ),
                  NetraSpacing.gapW8,
                ],
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: isOpen ? NetraColors.eligibleGreenBg : NetraColors.ineligibleRedBg,
                    borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
                  ),
                  child: Text(
                    bank.operatingStatus.displayName,
                    style: NetraTypography.labelSmall.copyWith(
                      color: isOpen ? NetraColors.eligibleGreen : NetraColors.ineligibleRed,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const Spacer(),
                BloodBankVerificationBadge(status: bank.verificationStatus),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
