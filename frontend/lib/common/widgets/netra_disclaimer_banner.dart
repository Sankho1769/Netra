import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';

class NetraDisclaimerBanner extends StatelessWidget {
  final String? customText;
  final bool compact;
  final IconData icon;

  const NetraDisclaimerBanner({
    super.key,
    this.customText,
    this.compact = false,
    this.icon = Icons.info_outline_rounded,
  });

  @override
  Widget build(BuildContext context) {
    final text = customText ??
        "Pre-screening result only. Final eligibility is determined by the blood bank/qualified medical staff after physical examination and required tests.";

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(compact ? NetraSpacing.md : NetraSpacing.lg),
      decoration: BoxDecoration(
        color: NetraColors.backgroundRed,
        borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
        border: Border.all(color: NetraColors.lightRed, width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            icon,
            color: NetraColors.primaryRed,
            size: compact ? 18 : 20,
          ),
          NetraSpacing.gapW12,
          Expanded(
            child: Text(
              text,
              style: compact
                  ? NetraTypography.bodySmall.copyWith(
                      color: NetraColors.textPrimary,
                      fontWeight: FontWeight.w500,
                    )
                  : NetraTypography.disclaimerText,
            ),
          ),
        ],
      ),
    );
  }
}
