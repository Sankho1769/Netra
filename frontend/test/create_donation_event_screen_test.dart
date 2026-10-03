import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/auth/models/auth_models.dart';
import 'package:netra_app/features/auth/state/auth_controller.dart';
import 'package:netra_app/features/auth/state/auth_scope.dart';
import 'package:netra_app/features/events/screens/create_donation_event_screen.dart';

class FakeAuthController extends AuthController {
  final User? _user;
  FakeAuthController(this._user) : super(configureGlobalAuth: false);

  @override
  User? get currentUser => _user;
}

void main() {
  const donorUser = User(
    id: 'user-donor-1',
    fullName: 'Rahul Sharma',
    email: 'rahul@example.com',
    roles: ['ROLE_DONOR'],
    status: 'ACTIVE',
  );

  const adminUser = User(
    id: 'user-admin-1',
    fullName: 'Admin User',
    email: 'admin@netra.org',
    roles: ['ROLE_ADMIN'],
    status: 'ACTIVE',
  );

  const bloodBankStaff = User(
    id: 'user-bb-1',
    fullName: 'Bank Staff',
    email: 'staff@bloodbank.org',
    roles: ['ROLE_BLOODBANK'],
    status: 'ACTIVE',
  );

  group('CreateDonationEventScreen Authorization UX Gating Tests', () {
    testWidgets('unauthorized donor sees clear guidance screen, not creation form', (tester) async {
      final authController = FakeAuthController(donorUser);

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: authController,
            child: const CreateDonationEventScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Should show unauthorized guidance header & notice
      expect(find.text('Create Donation Camp'), findsWidgets);
      expect(
        find.text('Only authorized blood-bank organizations and approved administrators can create donation camps.'),
        findsOneWidget,
      );
      expect(find.text('Find Active Camps'), findsOneWidget);
      expect(find.text('Contact Support'), findsOneWidget);

      // Creation form fields and submit button must NOT be exposed
      expect(find.text('Organize a Blood Donation Camp'), findsNothing);
      expect(find.text('Create Camp'), findsNothing);
      expect(find.byType(TextFormField), findsNothing);
    });

    testWidgets('unauthorized donor tapping Contact Support opens support dialog', (tester) async {
      final authController = FakeAuthController(donorUser);

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: authController,
            child: const CreateDonationEventScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      final contactSupportFinder = find.text('Contact Support');
      await tester.ensureVisible(contactSupportFinder);
      await tester.tap(contactSupportFinder);
      await tester.pumpAndSettle();

      expect(find.text('Organizer Support'), findsOneWidget);
      expect(find.textContaining('support@netra.org'), findsOneWidget);
      expect(find.textContaining('1800-NETRA-HELP'), findsOneWidget);
    });
  });

  group('CreateDonationEventScreen Production Clean State Tests (No Dummy Data)', () {
    testWidgets('fresh form opens with 100% empty fields and no default 50 capacity', (tester) async {
      final authController = FakeAuthController(adminUser);

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: authController,
            child: const CreateDonationEventScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Authorized view should be shown
      expect(find.text('Organize a Blood Donation Camp'), findsOneWidget);
      expect(find.textContaining('Authorized Host: Admin User'), findsOneWidget);

      // Find all TextFormField widgets
      final textFields = tester.widgetList<TextFormField>(find.byType(TextFormField)).toList();

      // Check controllers / initial values of form fields
      for (final field in textFields) {
        final text = field.controller?.text ?? '';
        // No field should contain default '50', 'Community Blood Donation Drive', etc.
        expect(text, isNot('50'), reason: 'Donor capacity must not default to 50');
        expect(text, isNot('Community Blood Donation Drive'), reason: 'Camp title must not be prefilled');
        expect(text, isNot(contains('Auditorium')), reason: 'Venue must not be invented');
      }

      // Check explicit date picker placeholders (all dates start null)
      expect(find.text('Select camp start'), findsOneWidget);
      expect(find.text('Select camp end'), findsOneWidget);
      expect(find.text('Select registration open'), findsOneWidget);
      expect(find.text('Select registration close'), findsOneWidget);

      // Location status banner must indicate no location attached
      expect(find.textContaining('No GPS coordinates attached'), findsOneWidget);

      // Review card starts with clean empty indicators
      expect(find.text('Camp Summary Review'), findsOneWidget);
      expect(find.text('Not Attached'), findsOneWidget);
      expect(find.text('Not selected'), findsNWidgets(2)); // Schedule & Registration
    });

    testWidgets('authorized blood bank staff sees authorized host badge and can enter data', (tester) async {
      final authController = FakeAuthController(bloodBankStaff);

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: authController,
            child: const CreateDonationEventScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.textContaining('Authorized Host: Bank Staff (ROLE_BLOODBANK)'), findsOneWidget);

      // Enter camp title
      final titleFinder = find.widgetWithText(TextFormField, 'Enter camp name (e.g. Annual Community Blood Drive)');
      expect(titleFinder, findsOneWidget);
      await tester.enterText(titleFinder, 'Red Cross Blood Drive 2026');
      await tester.pumpAndSettle();

      // Enter capacity
      final capacityFinder = find.widgetWithText(TextFormField, 'Enter maximum donor capacity (e.g. 50, 100)');
      expect(capacityFinder, findsOneWidget);
      await tester.enterText(capacityFinder, '75');
      await tester.pumpAndSettle();

      // Verify review card updates with user-entered values
      expect(find.text('Red Cross Blood Drive 2026'), findsWidgets);
      final slotsFinder = find.text('75 slots');
      await tester.ensureVisible(slotsFinder);
      expect(slotsFinder, findsOneWidget);
    });

    testWidgets('tapping Create Camp with empty fields triggers validation error', (tester) async {
      final authController = FakeAuthController(adminUser);

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: authController,
            child: const CreateDonationEventScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Scroll to and tap Create Camp button
      final createButton = find.text('Create Camp');
      await tester.ensureVisible(createButton);
      await tester.tap(createButton);
      await tester.pumpAndSettle();

      // Should show validation SnackBar
      expect(find.byType(SnackBar), findsOneWidget);
    });
  });
}
