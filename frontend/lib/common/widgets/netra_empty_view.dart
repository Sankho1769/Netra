import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';
import 'netra_button.dart';

class NetraEmptyView extends StatelessWidget {
  final String title;
  final String message;
  final IconData icon;
  final String? actionText;
  final VoidCallback? onAction;

  const NetraEmptyView({
    super.key,
    required this.title,
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.actionText,
    this.onAction,
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
              padding: NetraSpacing.paddingXl,
              decoration: const BoxDecoration(
                color: NetraColors.backgroundGray,
                shape: BoxShape.circle,
              ),
              child: Icon(
                icon,
                size: 48,
                color: NetraColors.textMuted,
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
            if (actionText != null && onAction != null) ...[
              NetraSpacing.gapH24,
              NetraButton.secondary(
                text: actionText!,
                onPressed: onAction,
                isFullWidth: false,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
