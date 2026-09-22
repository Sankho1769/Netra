import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';

class NetraLoadingIndicator extends StatelessWidget {
  final String? message;
  final double size;

  const NetraLoadingIndicator({
    super.key,
    this.message,
    this.size = 40.0,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          SizedBox(
            width: size,
            height: size,
            child: const CircularProgressIndicator(
              strokeWidth: 3.0,
              valueColor: AlwaysStoppedAnimation<Color>(NetraColors.primaryRed),
            ),
          ),
          if (message != null) ...[
            NetraSpacing.gapH16,
            Text(
              message!,
              textAlign: TextAlign.center,
              style: NetraTypography.bodyMedium
                  .copyWith(color: NetraColors.textSecondary),
            ),
          ],
        ],
      ),
    );
  }
}
