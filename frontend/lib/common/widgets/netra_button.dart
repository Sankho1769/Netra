import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/netra_typography.dart';

enum NetraButtonVariant { primary, secondary, outlined, text, danger }

class NetraButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final NetraButtonVariant variant;
  final IconData? icon;
  final bool isLoading;
  final bool isFullWidth;
  final double height;

  const NetraButton({
    super.key,
    required this.text,
    required this.onPressed,
    this.variant = NetraButtonVariant.primary,
    this.icon,
    this.isLoading = false,
    this.isFullWidth = true,
    this.height = 52.0,
  });

  const NetraButton.outlined({
    super.key,
    required this.text,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.isFullWidth = true,
    this.height = 50.0,
  }) : variant = NetraButtonVariant.outlined;

  const NetraButton.secondary({
    super.key,
    required this.text,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.isFullWidth = true,
    this.height = 50.0,
  }) : variant = NetraButtonVariant.secondary;

  const NetraButton.text({
    super.key,
    required this.text,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.isFullWidth = false,
    this.height = 44.0,
  }) : variant = NetraButtonVariant.text;

  const NetraButton.danger({
    super.key,
    required this.text,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.isFullWidth = true,
    this.height = 52.0,
  }) : variant = NetraButtonVariant.danger;

  @override
  Widget build(BuildContext context) {
    final effectiveOnPressed = isLoading ? null : onPressed;

    Widget childContent;
    if (isLoading) {
      childContent = SizedBox(
        width: 24,
        height: 24,
        child: CircularProgressIndicator(
          strokeWidth: 2.5,
          valueColor: AlwaysStoppedAnimation<Color>(
            variant == NetraButtonVariant.primary || variant == NetraButtonVariant.danger
                ? NetraColors.surfaceWhite
                : NetraColors.primaryRed,
          ),
        ),
      );
    } else {
      childContent = Row(
        mainAxisSize: isFullWidth ? MainAxisSize.max : MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 20),
            NetraSpacing.gapW8,
          ],
          Text(text),
        ],
      );
    }

    Widget button;
    switch (variant) {
      case NetraButtonVariant.primary:
        button = ElevatedButton(
          onPressed: effectiveOnPressed,
          style: ElevatedButton.styleFrom(
            backgroundColor: NetraColors.primaryRed,
            foregroundColor: NetraColors.surfaceWhite,
            disabledBackgroundColor: NetraColors.borderGray,
            disabledForegroundColor: NetraColors.textDisabled,
            elevation: 0,
            minimumSize: Size(isFullWidth ? double.infinity : 120, height),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            ),
            textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.surfaceWhite),
          ),
          child: childContent,
        );
        break;

      case NetraButtonVariant.secondary:
        button = ElevatedButton(
          onPressed: effectiveOnPressed,
          style: ElevatedButton.styleFrom(
            backgroundColor: NetraColors.backgroundRed,
            foregroundColor: NetraColors.primaryRed,
            disabledBackgroundColor: NetraColors.borderSubtle,
            disabledForegroundColor: NetraColors.textDisabled,
            elevation: 0,
            minimumSize: Size(isFullWidth ? double.infinity : 120, height),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            ),
            textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.primaryRed),
          ),
          child: childContent,
        );
        break;

      case NetraButtonVariant.outlined:
        button = OutlinedButton(
          onPressed: effectiveOnPressed,
          style: OutlinedButton.styleFrom(
            foregroundColor: NetraColors.primaryRed,
            disabledForegroundColor: NetraColors.textDisabled,
            side: BorderSide(
              color: effectiveOnPressed == null ? NetraColors.borderGray : NetraColors.primaryRed,
              width: 1.5,
            ),
            minimumSize: Size(isFullWidth ? double.infinity : 120, height),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            ),
            textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.primaryRed),
          ),
          child: childContent,
        );
        break;

      case NetraButtonVariant.text:
        button = TextButton(
          onPressed: effectiveOnPressed,
          style: TextButton.styleFrom(
            foregroundColor: NetraColors.primaryRed,
            disabledForegroundColor: NetraColors.textDisabled,
            minimumSize: Size(isFullWidth ? double.infinity : 80, height),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            ),
            textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.primaryRed),
          ),
          child: childContent,
        );
        break;

      case NetraButtonVariant.danger:
        button = ElevatedButton(
          onPressed: effectiveOnPressed,
          style: ElevatedButton.styleFrom(
            backgroundColor: NetraColors.errorRed,
            foregroundColor: NetraColors.surfaceWhite,
            disabledBackgroundColor: NetraColors.borderGray,
            disabledForegroundColor: NetraColors.textDisabled,
            elevation: 0,
            minimumSize: Size(isFullWidth ? double.infinity : 120, height),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
            ),
            textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.surfaceWhite),
          ),
          child: childContent,
        );
        break;
    }

    return isFullWidth ? SizedBox(width: double.infinity, child: button) : button;
  }
}
