import 'package:flutter/material.dart';
import 'netra_colors.dart';

/// NETRA Design System Typography
/// Scaled hierarchy optimized for clinical readability, high contrast,
/// and responsive scaling across device sizes.
abstract final class NetraTypography {
  // Display Styles (for large landing hero text)
  static const TextStyle displayLarge = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.5,
    height: 1.25,
    color: NetraColors.textPrimary,
  );

  static const TextStyle displayMedium = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.4,
    height: 1.28,
    color: NetraColors.textPrimary,
  );

  static const TextStyle displaySmall = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.3,
    height: 1.3,
    color: NetraColors.textPrimary,
  );

  // Headline Styles (for screen titles and modal headers)
  static const TextStyle headlineLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.3,
    height: 1.33,
    color: NetraColors.textPrimary,
  );

  static const TextStyle headlineMedium = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.2,
    height: 1.35,
    color: NetraColors.textPrimary,
  );

  static const TextStyle headlineSmall = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.15,
    height: 1.4,
    color: NetraColors.textPrimary,
  );

  // Title Styles (for card headers and section labels)
  static const TextStyle titleLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.1,
    height: 1.45,
    color: NetraColors.textPrimary,
  );

  static const TextStyle titleMedium = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.1,
    height: 1.45,
    color: NetraColors.textPrimary,
  );

  static const TextStyle titleSmall = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.1,
    height: 1.45,
    color: NetraColors.textPrimary,
  );

  // Body Styles (for question descriptions, help text, paragraphs)
  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w400,
    letterSpacing: 0.2,
    height: 1.5,
    color: NetraColors.textPrimary,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    letterSpacing: 0.2,
    height: 1.5,
    color: NetraColors.textSecondary,
  );

  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
    letterSpacing: 0.2,
    height: 1.5,
    color: NetraColors.textMuted,
  );

  // Label & Button Styles
  static const TextStyle labelLarge = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.3,
    height: 1.35,
    color: NetraColors.textPrimary,
  );

  static const TextStyle labelMedium = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.3,
    height: 1.35,
    color: NetraColors.textSecondary,
  );

  static const TextStyle labelSmall = TextStyle(
    fontSize: 11,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.4,
    height: 1.35,
    color: NetraColors.textMuted,
  );

  // Clinical / Alert text helpers
  static const TextStyle disclaimerText = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w500,
    letterSpacing: 0.1,
    height: 1.45,
    color: NetraColors.textSecondary,
  );
}
