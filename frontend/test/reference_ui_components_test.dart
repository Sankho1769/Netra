import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/common/widgets/netra_bottom_nav_bar.dart';
import 'package:netra_app/common/widgets/blood_action_sheet.dart';
import 'package:netra_app/features/about/screens/about_screen.dart';

void main() {
  group('NetraBottomNavBar Component Tests', () {
    testWidgets('renders all 5 navigation items and triggers callbacks',
        (tester) async {
      int selectedIdx = 0;
      bool centerTapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            bottomNavigationBar: NetraBottomNavBar(
              selectedIndex: selectedIdx,
              onItemSelected: (idx) => selectedIdx = idx,
              onCenterActionTap: () => centerTapped = true,
            ),
          ),
        ),
      );

      // Verify labels exist
      expect(find.text('Home'), findsOneWidget);
      expect(find.text('Events'), findsOneWidget);
      expect(find.text('About'), findsOneWidget);
      expect(find.text('Profile'), findsOneWidget);

      // Verify Center Blood Drop button exists
      expect(find.byIcon(Icons.water_drop_rounded), findsOneWidget);

      // Tap Events (index 1)
      await tester.tap(find.text('Events'));
      await tester.pump();
      expect(selectedIdx, equals(1));

      // Tap About (index 3)
      await tester.tap(find.text('About'));
      await tester.pump();
      expect(selectedIdx, equals(3));

      // Tap Center button
      await tester.tap(find.byIcon(Icons.water_drop_rounded));
      await tester.pump();
      expect(centerTapped, isTrue);
    });
  });

  group('BloodActionSheet Modal Tests', () {
    testWidgets('renders action options correctly', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: BloodActionSheet(),
          ),
        ),
      );

      // Verify header and action cards
      expect(find.text('What do you need?'), findsOneWidget);
      expect(find.text('Choose an option to get started'), findsOneWidget);
      expect(find.text('GIVE BLOOD'), findsOneWidget);
      expect(find.text('NEED BLOOD'), findsOneWidget);
      expect(find.text('Find Authorized Blood Centres'), findsOneWidget);
      expect(find.text('DONATE NOW'), findsOneWidget);
      expect(find.text('REQUEST BLOOD'), findsOneWidget);
    });
  });

  group('AboutScreen Guidelines & Compatibility Tests', () {
    testWidgets('renders guidelines, goal progress, and dynamic matrix',
        (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: AboutScreen(isEmbedded: true),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Verify header & goal banner
      expect(find.text('Guidelines & Blood Facts'), findsOneWidget);
      expect(find.text('5,000 Units'), findsOneWidget);
      expect(find.text('4,250'), findsOneWidget);
      expect(find.text('85% Achieved'), findsOneWidget);

      // Verify Must Do & Must Avoid cards
      expect(find.text('What You MUST Do'), findsOneWidget);
      expect(find.text('What You MUST AVOID'), findsOneWidget);

      // Verify Compatibility Matrix & chips
      expect(find.text('Blood Compatibility Matrix'), findsOneWidget);
      expect(find.text('O+'), findsWidgets);
      expect(find.text('O-'), findsWidgets);
      expect(find.text('AB+'), findsWidgets);

      // Ensure ChoiceChip is visible in scrollable container
      await tester.ensureVisible(find.widgetWithText(ChoiceChip, 'O-'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(ChoiceChip, 'O-'));
      await tester.pumpAndSettle();
      expect(find.text('Universal Red Cell Donor'), findsOneWidget);

      // Tap AB+ chip
      await tester.ensureVisible(find.widgetWithText(ChoiceChip, 'AB+'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(ChoiceChip, 'AB+'));
      await tester.pumpAndSettle();
      expect(find.text('Universal Recipient'), findsOneWidget);

      // Verify NBTC standards section
      expect(find.text('NBTC National Donor Criteria'), findsOneWidget);
    });
  });
}
