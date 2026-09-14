import 'package:flutter/material.dart';

/// NETRA Design System Spacing Tokens & Insets
/// Standard 4px/8px modular grid for consistent layout pacing.
abstract final class NetraSpacing {
  // Base raw units
  static const double xxs = 2.0;
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 12.0;
  static const double lg = 16.0;
  static const double xl = 24.0;
  static const double xxl = 32.0;
  static const double xxxl = 48.0;
  static const double giant = 64.0;

  // Border Radii
  static const double radiusXs = 6.0;
  static const double radiusSm = 8.0;
  static const double radiusMd = 12.0;
  static const double radiusLg = 16.0;
  static const double radiusXl = 20.0;
  static const double radiusFull = 999.0;

  // Common Padding Presets
  static const EdgeInsets paddingZero = EdgeInsets.zero;
  static const EdgeInsets paddingXs = EdgeInsets.all(xs);
  static const EdgeInsets paddingSm = EdgeInsets.all(sm);
  static const EdgeInsets paddingMd = EdgeInsets.all(md);
  static const EdgeInsets paddingLg = EdgeInsets.all(lg);
  static const EdgeInsets paddingXl = EdgeInsets.all(xl);
  static const EdgeInsets paddingXxl = EdgeInsets.all(xxl);

  // Horizontal Padding Presets
  static const EdgeInsets paddingHorizontalSm = EdgeInsets.symmetric(horizontal: sm);
  static const EdgeInsets paddingHorizontalMd = EdgeInsets.symmetric(horizontal: md);
  static const EdgeInsets paddingHorizontalLg = EdgeInsets.symmetric(horizontal: lg);
  static const EdgeInsets paddingHorizontalXl = EdgeInsets.symmetric(horizontal: xl);

  // Vertical Padding Presets
  static const EdgeInsets paddingVerticalSm = EdgeInsets.symmetric(vertical: sm);
  static const EdgeInsets paddingVerticalMd = EdgeInsets.symmetric(vertical: md);
  static const EdgeInsets paddingVerticalLg = EdgeInsets.symmetric(vertical: lg);
  static const EdgeInsets paddingVerticalXl = EdgeInsets.symmetric(vertical: xl);

  // Card / Dialog Padding
  static const EdgeInsets cardPadding = EdgeInsets.all(16.0);
  static const EdgeInsets cardPaddingDense = EdgeInsets.all(12.0);
  static const EdgeInsets cardPaddingSpacious = EdgeInsets.all(24.0);

  // Screen Gutters
  static const EdgeInsets screenMobile = EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0);
  static const EdgeInsets screenTablet = EdgeInsets.symmetric(horizontal: 24.0, vertical: 20.0);
  static const EdgeInsets screenDesktop = EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0);

  // Vertical Gaps (SizedBox)
  static const Widget gapH4 = SizedBox(height: xs);
  static const Widget gapH8 = SizedBox(height: sm);
  static const Widget gapH12 = SizedBox(height: md);
  static const Widget gapH16 = SizedBox(height: lg);
  static const Widget gapH20 = SizedBox(height: 20.0);
  static const Widget gapH24 = SizedBox(height: xl);
  static const Widget gapH32 = SizedBox(height: xxl);
  static const Widget gapH48 = SizedBox(height: xxxl);

  // Horizontal Gaps (SizedBox)
  static const Widget gapW4 = SizedBox(width: xs);
  static const Widget gapW8 = SizedBox(width: sm);
  static const Widget gapW12 = SizedBox(width: md);
  static const Widget gapW16 = SizedBox(width: lg);
  static const Widget gapW24 = SizedBox(width: xl);
  static const Widget gapW32 = SizedBox(width: xxl);
}
