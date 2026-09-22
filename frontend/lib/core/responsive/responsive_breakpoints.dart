import 'package:flutter/material.dart';

/// Screen categories for responsive NETRA layouts.
enum ScreenType {
  compact, // < 360px (very small phones)
  mobile, // 360px - 599px (standard phones)
  tablet, // 600px - 1023px (tablets, foldables unfolded)
  desktop, // 1024px - 1439px (laptops, small monitors)
  wideDesktop, // >= 1440px (large desktops, ultrawide)
}

/// Breakpoint width constants in logical pixels.
abstract final class ResponsiveBreakpoints {
  static const double compact = 360.0;
  static const double mobile = 600.0;
  static const double tablet = 1024.0;
  static const double desktop = 1440.0;

  // Max width constraints for readability & ergonomics
  static const double maxContentWidthNarrow = 560.0;
  static const double maxContentWidthReading = 680.0;
  static const double maxContentWidthStandard = 880.0;
  static const double maxContentWidthWide = 1140.0;
  static const double maxContentWidthCanvas = 1400.0;

  /// Determine ScreenType from a given width
  static ScreenType getScreenType(double width) {
    if (width < compact) return ScreenType.compact;
    if (width < mobile) return ScreenType.mobile;
    if (width < tablet) return ScreenType.tablet;
    if (width < desktop) return ScreenType.desktop;
    return ScreenType.wideDesktop;
  }
}

/// Convenience BuildContext extensions for responsive queries
extension ResponsiveContext on BuildContext {
  double get screenWidth => MediaQuery.of(this).size.width;
  double get screenHeight => MediaQuery.of(this).size.height;

  ScreenType get screenType => ResponsiveBreakpoints.getScreenType(screenWidth);

  bool get isCompact => screenWidth < ResponsiveBreakpoints.compact;
  bool get isMobile => screenWidth < ResponsiveBreakpoints.mobile;
  bool get isTablet =>
      screenWidth >= ResponsiveBreakpoints.mobile &&
      screenWidth < ResponsiveBreakpoints.tablet;
  bool get isDesktop =>
      screenWidth >= ResponsiveBreakpoints.tablet &&
      screenWidth < ResponsiveBreakpoints.desktop;
  bool get isWideDesktop => screenWidth >= ResponsiveBreakpoints.desktop;
  bool get isDesktopOrWide => screenWidth >= ResponsiveBreakpoints.tablet;

  /// Returns a responsive value based on current screen type
  T responsiveValue<T>({
    required T mobile,
    T? tablet,
    T? desktop,
    T? wideDesktop,
    T? compact,
  }) {
    final type = screenType;
    switch (type) {
      case ScreenType.compact:
        return compact ?? mobile;
      case ScreenType.mobile:
        return mobile;
      case ScreenType.tablet:
        return tablet ?? mobile;
      case ScreenType.desktop:
        return desktop ?? tablet ?? mobile;
      case ScreenType.wideDesktop:
        return wideDesktop ?? desktop ?? tablet ?? mobile;
    }
  }

  /// Responsive screen gutter padding
  EdgeInsets get screenGutter {
    if (isCompact) {
      return const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0);
    }
    if (isMobile) {
      return const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0);
    }
    if (isTablet) {
      return const EdgeInsets.symmetric(horizontal: 24.0, vertical: 20.0);
    }
    return const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0);
  }
}
