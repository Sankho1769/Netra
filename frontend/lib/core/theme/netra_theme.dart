import 'package:flutter/material.dart';
import 'netra_colors.dart';
import 'netra_typography.dart';
import 'netra_spacing.dart';

export 'netra_colors.dart';
export 'netra_typography.dart';
export 'netra_spacing.dart';

class NetraTheme {
  // Brand Colors (aliases for backward compatibility)
  static const Color primaryRed = NetraColors.primaryRed;
  static const Color darkRed = NetraColors.darkRed;
  static const Color lightRed = NetraColors.lightRed;
  static const Color backgroundRed = NetraColors.backgroundRed;

  // Neutral Colors
  static const Color surfaceWhite = NetraColors.surfaceWhite;
  static const Color backgroundGray = NetraColors.backgroundGray;
  static const Color borderGray = NetraColors.borderGray;
  static const Color textPrimary = NetraColors.textPrimary;
  static const Color textSecondary = NetraColors.textSecondary;
  static const Color textMuted = NetraColors.textMuted;

  // Clinical Status Colors
  static const Color eligibleGreen = NetraColors.eligibleGreen;
  static const Color eligibleGreenBg = NetraColors.eligibleGreenBg;
  static const Color deferralAmber = NetraColors.deferralAmber;
  static const Color deferralAmberBg = NetraColors.deferralAmberBg;
  static const Color medicalReviewOrange = NetraColors.medicalReviewOrange;
  static const Color medicalReviewOrangeBg = NetraColors.medicalReviewOrangeBg;
  static const Color insufficientBlue = NetraColors.insufficientBlue;
  static const Color insufficientBlueBg = NetraColors.insufficientBlueBg;

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: NetraColors.primaryRed,
        primary: NetraColors.primaryRed,
        surface: NetraColors.surfaceWhite,
        brightness: Brightness.light,
      ),
      scaffoldBackgroundColor: NetraColors.backgroundGray,
      appBarTheme: const AppBarTheme(
        backgroundColor: NetraColors.surfaceWhite,
        foregroundColor: NetraColors.textPrimary,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: NetraTypography.headlineSmall,
      ),
      cardTheme: CardTheme(
        color: NetraColors.surfaceWhite,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(NetraSpacing.radiusLg),
          side: const BorderSide(color: NetraColors.borderGray, width: 1),
        ),
        margin: const EdgeInsets.symmetric(vertical: NetraSpacing.sm),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: NetraColors.primaryRed,
          foregroundColor: NetraColors.surfaceWhite,
          elevation: 0,
          minimumSize: const Size(double.infinity, 52),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          ),
          textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.surfaceWhite),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: NetraColors.primaryRed,
          side: const BorderSide(color: NetraColors.primaryRed, width: 1.5),
          minimumSize: const Size(double.infinity, 50),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          ),
          textStyle: NetraTypography.labelLarge.copyWith(color: NetraColors.primaryRed),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: NetraColors.surfaceWhite,
        contentPadding: const EdgeInsets.symmetric(horizontal: NetraSpacing.lg, vertical: NetraSpacing.md),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          borderSide: const BorderSide(color: NetraColors.borderGray),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          borderSide: const BorderSide(color: NetraColors.borderGray),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          borderSide: const BorderSide(color: NetraColors.primaryRed, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(NetraSpacing.radiusMd),
          borderSide: const BorderSide(color: NetraColors.darkRed, width: 1.5),
        ),
        labelStyle: NetraTypography.bodyMedium,
        hintStyle: NetraTypography.bodyMedium.copyWith(color: NetraColors.textMuted),
      ),
    );
  }
}
