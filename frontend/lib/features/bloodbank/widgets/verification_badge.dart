import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';
import '../models/blood_bank.dart';

class BloodBankVerificationBadge extends StatelessWidget {
  final BloodBankVerificationStatus status;

  const BloodBankVerificationBadge({
    super.key,
    required this.status,
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    IconData icon;

    switch (status) {
      case BloodBankVerificationStatus.verified:
        bg = NetraColors.eligibleGreenBg;
        fg = NetraColors.eligibleGreen;
        icon = Icons.verified_rounded;
        break;
      case BloodBankVerificationStatus.pending:
        bg = NetraColors.warningOrangeBg;
        fg = NetraColors.warningOrange;
        icon = Icons.hourglass_top_rounded;
        break;
      case BloodBankVerificationStatus.suspended:
        bg = NetraColors.ineligibleRedBg;
        fg = NetraColors.ineligibleRed;
        icon = Icons.block_rounded;
        break;
      case BloodBankVerificationStatus.rejected:
        bg = NetraColors.ineligibleRedBg;
        fg = NetraColors.ineligibleRed;
        icon = Icons.cancel_outlined;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusXs),
        border: Border.all(color: fg.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: fg),
          NetraSpacing.gapW4,
          Text(
            status.displayName,
            style: NetraTypography.labelSmall.copyWith(
              color: fg,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
