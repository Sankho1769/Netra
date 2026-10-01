import 'package:flutter/material.dart';
import 'netra_colors.dart';
import 'netra_spacing.dart';

/// Claymorphism and Glassmorphism design tokens for NETRA.
/// Combines soft, tactile 3D clay physical surfaces with frosted-glass translucent overlays.
abstract final class ClayGlassTheme {
  // --- Clay Surfaces & Shadows ---
  static const Color claySurfaceLight = Color(0xFFFFFFFF);
  static const Color claySurfaceSubtle = Color(0xFFF8FAFC);
  static const Color claySurfaceWarm = Color(0xFFFFF7F7);
  static const Color claySurfaceGreen = Color(0xFFF0FDF4);
  static const Color claySurfaceAmber = Color(0xFFFFFBEB);
  static const Color claySurfaceCrimson = NetraColors.primaryRed;

  /// Soft, tactile dual shadow for clay cards
  static List<BoxShadow> clayShadow({
    double depth = 6.0,
    Color shadowColor = const Color(0xFF0F172A),
    double opacity = 0.07,
  }) {
    return [
      // Deep diffused drop shadow
      BoxShadow(
        color: shadowColor.withValues(alpha: opacity),
        offset: Offset(0, depth * 1.5),
        blurRadius: depth * 3,
        spreadRadius: 0,
      ),
      // Ambient soft glow
      BoxShadow(
        color: shadowColor.withValues(alpha: opacity * 0.5),
        offset: Offset(0, depth * 0.5),
        blurRadius: depth,
        spreadRadius: -1,
      ),
      // Top-left specular highlight for 3D inflated clay effect
      BoxShadow(
        color: Colors.white.withValues(alpha: 0.9),
        offset: Offset(-depth * 0.5, -depth * 0.5),
        blurRadius: depth,
        spreadRadius: 1,
      ),
    ];
  }

  /// Crimson clay shadow for primary call-to-actions
  static List<BoxShadow> crimsonClayShadow({
    double depth = 8.0,
  }) {
    return [
      BoxShadow(
        color: const Color(0xFFB71C1C).withValues(alpha: 0.35),
        offset: Offset(0, depth * 1.2),
        blurRadius: depth * 2.5,
        spreadRadius: 0,
      ),
      BoxShadow(
        color: const Color(0xFFE53935).withValues(alpha: 0.20),
        offset: Offset(0, depth * 0.4),
        blurRadius: depth,
        spreadRadius: -1,
      ),
      BoxShadow(
        color: Colors.white.withValues(alpha: 0.4),
        offset: const Offset(-2, -2),
        blurRadius: 4,
        spreadRadius: 0,
      ),
    ];
  }

  // --- Glass Surfaces & Shadows ---
  static const Color glassSurfaceLight = Color(0xCCFFFFFF); // 80% opacity
  static const Color glassSurfaceDark = Color(0xB31E293B);
  static const Color glassBorderLight = Color(0x99FFFFFF);
  static const Color glassBorderSubtle = Color(0x33CBD5E1);

  static List<BoxShadow> glassShadow({
    double blur = 16.0,
    double opacity = 0.06,
  }) {
    return [
      BoxShadow(
        color: Colors.black.withValues(alpha: opacity),
        offset: const Offset(0, 8),
        blurRadius: blur,
        spreadRadius: 0,
      ),
    ];
  }

  /// Reusable clay card box decoration
  static BoxDecoration clayCardDecoration({
    Color color = claySurfaceLight,
    double borderRadius = NetraSpacing.radiusLg,
    double depth = 6.0,
    Border? border,
  }) {
    return BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(borderRadius),
      border: border ?? Border.all(color: const Color(0xFFF1F5F9), width: 1.0),
      boxShadow: clayShadow(depth: depth),
    );
  }

  /// Reusable glass container box decoration
  static BoxDecoration glassCardDecoration({
    Color color = glassSurfaceLight,
    double borderRadius = NetraSpacing.radiusLg,
    Color borderColor = glassBorderLight,
    double borderWidth = 1.2,
  }) {
    return BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(borderRadius),
      border: Border.all(color: borderColor, width: borderWidth),
      boxShadow: glassShadow(),
    );
  }
}
