import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';
import 'netra_button.dart';

class NetraErrorView extends StatelessWidget {
  final String title;
  final String message;
  final String? retryButtonText;
  final VoidCallback? onRetry;
  final IconData icon;

  const NetraErrorView({
    super.key,
    this.title = 'Something went wrong',
    required this.message,
    this.retryButtonText = 'Try Again',
    this.onRetry,
    this.icon = Icons.error_outline_rounded,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: NetraSpacing.screenMobile,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              padding: NetraSpacing.paddingLg,
              decoration: const BoxDecoration(
                color: NetraColors.errorRedBg,
                shape: BoxShape.circle,
              ),
              child: Icon(
                icon,
                size: 48,
                color: NetraColors.errorRed,
              ),
            ),
            NetraSpacing.gapH20,
            Text(
              title,
              textAlign: TextAlign.center,
              style: NetraTypography.headlineSmall,
            ),
            NetraSpacing.gapH8,
            Text(
              message,
              textAlign: TextAlign.center,
              style: NetraTypography.bodyMedium,
            ),
            if (onRetry != null) ...[
              NetraSpacing.gapH24,
              NetraButton.secondary(
                text: retryButtonText ?? 'Try Again',
                onPressed: onRetry,
                icon: Icons.refresh_rounded,
                isFullWidth: false,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
