import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';

typedef NetraChipType = NetraChipVariant;

enum NetraChipVariant {
  neutral,
  eligible,
  deferral,
  review,
  info,
  outline,
  statusSuccess,
}

class NetraChip extends StatelessWidget {
  final String label;
  final IconData? icon;
  final NetraChipVariant variant;
  final VoidCallback? onTap;

  const NetraChip({
    super.key,
    required this.label,
    this.icon,
    NetraChipVariant? variant,
    NetraChipVariant? type,
    this.onTap,
  }) : variant = type ?? variant ?? NetraChipVariant.neutral;

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color border;
    Color text;

    switch (variant) {
      case NetraChipVariant.eligible:
      case NetraChipVariant.statusSuccess:
        bg = NetraColors.eligibleGreenBg;
        border = NetraColors.eligibleGreenBorder;
        text = NetraColors.eligibleGreen;
        break;
      case NetraChipVariant.deferral:
        bg = NetraColors.deferralAmberBg;
        border = NetraColors.deferralAmberBorder;
        text = NetraColors.deferralAmber;
        break;
      case NetraChipVariant.review:
        bg = NetraColors.medicalReviewOrangeBg;
        border = NetraColors.medicalReviewOrangeBorder;
        text = NetraColors.medicalReviewOrange;
        break;
      case NetraChipVariant.info:
        bg = NetraColors.insufficientBlueBg;
        border = NetraColors.insufficientBlueBorder;
        text = NetraColors.insufficientBlue;
        break;
      case NetraChipVariant.outline:
        bg = Colors.transparent;
        border = NetraColors.borderGray;
        text = NetraColors.textPrimary;
        break;
      case NetraChipVariant.neutral:
        bg = NetraColors.backgroundGray;
        border = NetraColors.borderGray;
        text = NetraColors.textSecondary;
        break;
    }

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(NetraSpacing.radiusFull),
      child: Container(
        padding: const EdgeInsets.symmetric(
            horizontal: NetraSpacing.md, vertical: 6),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: BorderRadius.circular(NetraSpacing.radiusFull),
          border: Border.all(color: border, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 14, color: text),
              NetraSpacing.gapW4,
            ],
            Text(
              label,
              style: NetraTypography.labelSmall.copyWith(
                color: text,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
