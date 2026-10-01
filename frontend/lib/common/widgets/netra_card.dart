import 'dart:ui';
import 'package:flutter/material.dart';
import '../../core/theme/netra_colors.dart';
import '../../core/theme/netra_spacing.dart';
import '../../core/theme/clay_glass_theme.dart';

enum _CardStyle { standard, elevated, outlined, selectable, clay, glass }

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
  final _CardStyle _style;
  final double clayDepth;

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
    this.clayDepth = 6.0,
  }) : _style = _CardStyle.standard;

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
    this.clayDepth = 6.0,
  }) : _style = _CardStyle.elevated;

  const NetraCard.outlined({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor = NetraColors.surfaceWhite,
    this.borderColor = const Color(0xFFF1F5F9),
    this.borderWidth = 1.0,
    this.borderRadius = NetraSpacing.radiusLg,
    this.isSelected = false,
    this.elevation = 0,
    this.clayDepth = 5.0,
  }) : _style = _CardStyle.outlined;

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
    this.clayDepth = 6.0,
  }) : _style = _CardStyle.selectable;

  /// Tactile 3D Claymorphic card with inflated dual shadows
  const NetraCard.clay({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor = ClayGlassTheme.claySurfaceLight,
    this.borderColor = const Color(0xFFF1F5F9),
    this.borderWidth = 1.0,
    this.borderRadius = 22.0,
    this.isSelected = false,
    this.clayDepth = 6.0,
  })  : elevation = 0,
        _style = _CardStyle.clay;

  /// Translucent Glassmorphic card with backdrop blur
  const NetraCard.glass({
    super.key,
    required this.child,
    this.padding = NetraSpacing.cardPadding,
    this.onTap,
    this.backgroundColor = ClayGlassTheme.glassSurfaceLight,
    this.borderColor = ClayGlassTheme.glassBorderLight,
    this.borderWidth = 1.2,
    this.borderRadius = 22.0,
    this.isSelected = false,
    this.clayDepth = 0,
  })  : elevation = 0,
        _style = _CardStyle.glass;

  @override
  Widget build(BuildContext context) {
    if (_style == _CardStyle.glass) {
      return Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(borderRadius),
          boxShadow: ClayGlassTheme.glassShadow(),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(borderRadius),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
            child: Material(
              color: backgroundColor ?? ClayGlassTheme.glassSurfaceLight,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(borderRadius),
                side: BorderSide(
                  color: borderColor ?? ClayGlassTheme.glassBorderLight,
                  width: borderWidth,
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
            ),
          ),
        ),
      );
    }

    if (_style == _CardStyle.clay) {
      final effectiveBgColor = backgroundColor ??
          (isSelected ? NetraColors.backgroundRed : ClayGlassTheme.claySurfaceLight);
      final effectiveBorderColor = borderColor ??
          (isSelected ? NetraColors.primaryRed : const Color(0xFFF1F5F9));

      return Container(
        decoration: BoxDecoration(
          color: effectiveBgColor,
          borderRadius: BorderRadius.circular(borderRadius),
          border: Border.all(
            color: effectiveBorderColor,
            width: isSelected ? 2.0 : borderWidth,
          ),
          boxShadow: ClayGlassTheme.clayShadow(depth: clayDepth),
        ),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(borderRadius),
            child: Padding(
              padding: padding,
              child: child,
            ),
          ),
        ),
      );
    }

    final effectiveBgColor = backgroundColor ??
        (isSelected ? NetraColors.backgroundRed : NetraColors.surfaceWhite);

    final effectiveBorderColor = borderColor ??
        (isSelected ? NetraColors.primaryRed : NetraColors.borderGray);

    final effectiveBorderWidth = isSelected ? 2.0 : borderWidth;

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(borderRadius),
        boxShadow: elevation > 0 || _style == _CardStyle.outlined
            ? ClayGlassTheme.clayShadow(depth: elevation > 0 ? elevation * 2.5 : 4.0, opacity: 0.05)
            : null,
      ),
      child: Material(
        color: effectiveBgColor,
        elevation: 0,
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
      ),
    );
  }
}
