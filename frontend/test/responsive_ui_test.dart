import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/core/responsive/responsive.dart';
import 'package:netra_app/core/theme/netra_colors.dart';
import 'package:netra_app/core/theme/netra_spacing.dart';
import 'package:netra_app/core/theme/netra_typography.dart';
import 'package:netra_app/common/widgets/common_widgets.dart';

void main() {
  group('Responsive Breakpoint Tests (Section 2 & 48)', () {
    test('Correctly maps screen widths to ScreenType', () {
      // 1. Compact Mobile (< 360px)
      expect(ResponsiveBreakpoints.getScreenType(320.0), ScreenType.compact);
      expect(ResponsiveBreakpoints.getScreenType(359.0), ScreenType.compact);

      // 2. Standard Mobile (360px - 599px)
      expect(ResponsiveBreakpoints.getScreenType(360.0), ScreenType.mobile);
      expect(ResponsiveBreakpoints.getScreenType(375.0), ScreenType.mobile);
      expect(ResponsiveBreakpoints.getScreenType(414.0), ScreenType.mobile);
      expect(ResponsiveBreakpoints.getScreenType(430.0), ScreenType.mobile);
      expect(ResponsiveBreakpoints.getScreenType(599.0), ScreenType.mobile);

      // 3. Tablet (600px - 1023px)
      expect(ResponsiveBreakpoints.getScreenType(600.0), ScreenType.tablet);
      expect(ResponsiveBreakpoints.getScreenType(768.0), ScreenType.tablet);
      expect(ResponsiveBreakpoints.getScreenType(834.0), ScreenType.tablet);
      expect(ResponsiveBreakpoints.getScreenType(1023.0), ScreenType.tablet);

      // 4. Desktop (1024px - 1439px)
      expect(ResponsiveBreakpoints.getScreenType(1024.0), ScreenType.desktop);
      expect(ResponsiveBreakpoints.getScreenType(1280.0), ScreenType.desktop);
      expect(ResponsiveBreakpoints.getScreenType(1366.0), ScreenType.desktop);
      expect(ResponsiveBreakpoints.getScreenType(1439.0), ScreenType.desktop);

      // 5. Wide Desktop (>= 1440px)
      expect(
          ResponsiveBreakpoints.getScreenType(1440.0), ScreenType.wideDesktop);
      expect(
          ResponsiveBreakpoints.getScreenType(1920.0), ScreenType.wideDesktop);
      expect(
          ResponsiveBreakpoints.getScreenType(2560.0), ScreenType.wideDesktop);
    });

    test('Responsive container max width constants conform to ergonomics', () {
      expect(ResponsiveBreakpoints.maxContentWidthNarrow, 560.0);
      expect(ResponsiveBreakpoints.maxContentWidthReading, 680.0);
      expect(ResponsiveBreakpoints.maxContentWidthStandard, 880.0);
      expect(ResponsiveBreakpoints.maxContentWidthWide, 1140.0);
      expect(ResponsiveBreakpoints.maxContentWidthCanvas, 1400.0);
    });
  });

  group('Design System Tokens Test', () {
    test('Color contrast and status palette verification', () {
      expect(NetraColors.primaryRed, const Color(0xFFD32F2F));
      expect(NetraColors.darkRed, const Color(0xFFB71C1C));
      expect(NetraColors.eligibleGreen, const Color(0xFF2E7D32));
      expect(NetraColors.deferralAmber, const Color(0xFFED6C02));
      expect(NetraColors.medicalReviewOrange, const Color(0xFFE65100));
      expect(NetraColors.insufficientBlue, const Color(0xFF1976D2));
    });

    test('Spacing grid conforms to 4px/8px modular units', () {
      expect(NetraSpacing.xs, 4.0);
      expect(NetraSpacing.sm, 8.0);
      expect(NetraSpacing.md, 12.0);
      expect(NetraSpacing.lg, 16.0);
      expect(NetraSpacing.xl, 24.0);
      expect(NetraSpacing.xxl, 32.0);
      expect(NetraSpacing.xxxl, 48.0);
    });

    test('Typography styles define appropriate font weights', () {
      expect(NetraTypography.displayLarge.fontWeight, FontWeight.w700);
      expect(NetraTypography.headlineMedium.fontWeight, FontWeight.w600);
      expect(NetraTypography.bodyLarge.fontWeight, FontWeight.w400);
      expect(NetraTypography.labelLarge.fontWeight, FontWeight.w600);
    });
  });

  group('Common Widget Rendering Tests across Viewports', () {
    testWidgets('ResponsiveContainer bounds content width correctly',
        (WidgetTester tester) async {
      // Simulate Desktop viewport (1280x800)
      tester.view.physicalSize = const Size(1280, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ResponsiveContainer.reading(
              child: const Text('Responsive Reading Content'),
            ),
          ),
        ),
      );

      final constrainedBoxFinder = find.byType(ConstrainedBox);
      expect(constrainedBoxFinder, findsWidgets);

      final ConstrainedBox constrainedBox =
          tester.widgetList<ConstrainedBox>(constrainedBoxFinder).firstWhere(
                (box) =>
                    box.constraints.maxWidth ==
                    ResponsiveBreakpoints.maxContentWidthReading,
              );
      expect(constrainedBox.constraints.maxWidth, 680.0);
    });

    testWidgets('NetraButton renders with primary variant and reacts to tap',
        (WidgetTester tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: NetraButton(
              text: 'Save and Continue',
              onPressed: () => tapped = true,
            ),
          ),
        ),
      );

      expect(find.text('Save and Continue'), findsOneWidget);
      await tester.tap(find.byType(NetraButton));
      expect(tapped, true);
    });

    testWidgets('NetraDisclaimerBanner renders mandatory medical guidance text',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: NetraDisclaimerBanner(),
          ),
        ),
      );

      expect(find.byIcon(Icons.info_outline_rounded), findsOneWidget);
      expect(find.textContaining('Pre-screening result only'), findsOneWidget);
    });
  });
}
