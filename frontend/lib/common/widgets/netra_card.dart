import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';

class NetraCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;
  final Color? backgroundColor;
  final Color? borderColor;
  final double borderWidth;
  final double borderRadius;
  final bool isSelected;
  final double elevation;

  const NetraCard({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor,
    this.borderColor,
    this.borderWidth = 1.0,
    this.borderRadius = NetraSpacing.radiusLg,
    this.isSelected = false,
    this.elevation = 0,
  });

  const NetraCard.elevated({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor = NetraColors.surfaceWhite,
    this.borderColor,
    this.borderWidth = 0,
    this.borderRadius = NetraSpacing.radiusLg,
    this.isSelected = false,
    this.elevation = 2.0,
  });

  const NetraCard.outlined({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor = NetraColors.surfaceWhite,
    this.borderColor = NetraColors.borderGray,
    this.borderWidth = 1.0,
    this.borderRadius = NetraSpacing.radiusLg,
    this.isSelected = false,
    this.elevation = 0,
  });

  const NetraCard.selectable({
    super.key,
    required this.child,
    required this.isSelected,
    required this.onTap,
    this.padding = NetraSpacing.cardPadding,
    this.backgroundColor,
    this.borderColor,
    this.borderWidth = 1.5,
    this.borderRadius = NetraSpacing.radiusLg,
    this.elevation = 0,
  });

  @override
  Widget build(BuildContext context) {
    final effectiveBgColor = backgroundColor ??
        (isSelected ? NetraColors.backgroundRed : NetraColors.surfaceWhite);

    final effectiveBorderColor = borderColor ??
        (isSelected ? NetraColors.primaryRed : NetraColors.borderGray);

    final effectiveBorderWidth = isSelected ? 2.0 : borderWidth;

    Widget cardWidget = Material(
      color: effectiveBgColor,
      elevation: elevation,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(borderRadius),
        side: BorderSide(
          color: effectiveBorderColor,
          width: effectiveBorderWidth,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(borderRadius),
        child: Padding(
          padding: padding,
          child: child,
        ),
      ),
    );

    return cardWidget;
  }
}
