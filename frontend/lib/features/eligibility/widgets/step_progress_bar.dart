import 'package:flutter/material.dart';
import '../../../core/theme/netra_colors.dart';
import '../../../core/theme/netra_spacing.dart';
import '../../../core/theme/netra_typography.dart';

class StepProgressBar extends StatelessWidget {
  final int currentStep;
  final int totalSteps;
  final VoidCallback? onBack;

  const StepProgressBar({
    super.key,
    required this.currentStep,
    required this.totalSteps,
    this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    final double progress = (currentStep / totalSteps).clamp(0.0, 1.0);

    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: NetraSpacing.lg, vertical: NetraSpacing.md),
      decoration: const BoxDecoration(
        color: NetraColors.surfaceWhite,
        border: Border(
            bottom: BorderSide(color: NetraColors.borderGray, width: 0.5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              if (onBack != null)
                IconButton(
                  icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 18),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  tooltip: 'Previous Step',
                  onPressed: onBack,
                )
              else
                const SizedBox(width: 18),
              Text(
                "Step $currentStep of $totalSteps",
                style: NetraTypography.labelLarge.copyWith(
                  color: NetraColors.primaryRed,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(width: 18),
            ],
          ),
          NetraSpacing.gapH8,
          ClipRRect(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusSm),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 6,
              backgroundColor: NetraColors.borderGray.withValues(alpha: 0.4),
              valueColor:
                  const AlwaysStoppedAnimation<Color>(NetraColors.primaryRed),
            ),
          ),
        ],
      ),
    );
  }
}
