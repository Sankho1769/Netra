import 'package:flutter/material.dart';

/// NETRA Design System Color Palette
/// Designed for clinical clarity, accessibility (WCAG AA/AAA compliant contrast),
/// and responsive readability across mobile, tablet, and desktop viewports.
abstract final class NetraColors {
  // --- Brand Crimson ---
  static const Color primaryRed = Color(0xFFD32F2F);
  static const Color darkRed = Color(0xFFB71C1C);
  static const Color lightRed = Color(0xFFFFCDD2);
  static const Color backgroundRed = Color(0xFFFFEBEE);
  static const Color accentRed = Color(0xFFE53935);

  // --- Neutrals (Light Mode) ---
  static const Color surfaceWhite = Color(0xFFFFFFFF);
  static const Color surfaceElevated = Color(0xFFFAFAFA);
  static const Color backgroundGray = Color(0xFFF8F9FA);
  static const Color borderGray = Color(0xFFE0E0E0);
  static const Color borderSubtle = Color(0xFFEEEEEE);
  static const Color dividerColor = Color(0xFFE0E0E0);

  // --- Text Hierarchy ---
  static const Color textPrimary = Color(0xFF212121);
  static const Color textSecondary = Color(0xFF616161);
  static const Color textMuted = Color(0xFF9E9E9E);
  static const Color textDisabled = Color(0xFFBDBDBD);
  static const Color textOnPrimary = Color(0xFFFFFFFF);

  // --- Clinical Status Colors (Always paired with clear text & icons) ---
  // Eligible / Safe
  static const Color eligibleGreen = Color(0xFF2E7D32);
  static const Color eligibleGreenBg = Color(0xFFE8F5E9);
  static const Color eligibleGreenBorder = Color(0xFFA5D6A7);

  // Temporary Deferral / Waiting Period
  static const Color deferralAmber = Color(0xFFED6C02);
  static const Color deferralAmberBg = Color(0xFFFFF4E5);
  static const Color deferralAmberBorder = Color(0xFFFFCC80);

  // Medical Review Required / Clinical Assessment
  static const Color medicalReviewOrange = Color(0xFFE65100);
  static const Color medicalReviewOrangeBg = Color(0xFFFFF3E0);
  static const Color medicalReviewOrangeBorder = Color(0xFFFFB74D);

  // Insufficient Information / Missing Data
  static const Color insufficientBlue = Color(0xFF1976D2);
  static const Color insufficientBlueBg = Color(0xFFE3F2FD);
  static const Color insufficientBlueBorder = Color(0xFF90CAF9);

  // Interactive / State Colors
  static const Color focusRing = Color(0xFF1976D2);
  static const Color errorRed = Color(0xFFC62828);
  static const Color errorRedBg = Color(0xFFFFEBEE);
  static const Color infoBlue = Color(0xFF0288D1);
  static const Color infoBlueBg = Color(0xFFE1F5FE);

  // Semantic Aliases
  static const Color backgroundGreen = eligibleGreenBg;
  static const Color successGreen = eligibleGreen;
  static const Color ineligibleRed = errorRed;
  static const Color ineligibleRedBg = errorRedBg;
  static const Color warningOrange = medicalReviewOrange;
  static const Color warningOrangeBg = medicalReviewOrangeBg;
}
