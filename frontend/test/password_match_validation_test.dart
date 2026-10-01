import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/auth/screens/register_screen.dart';
import 'package:netra_app/features/auth/state/auth_controller.dart';
import 'package:netra_app/features/auth/state/auth_scope.dart';
import 'package:netra_app/features/auth/services/auth_api_service.dart';
import 'package:netra_app/features/auth/services/secure_token_storage.dart';
import 'package:netra_app/features/auth/models/auth_models.dart';

class _FakeTokenStorage implements SecureTokenStorage {
  String? accessToken;
  String? refreshToken;

  @override
  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @override
  Future<String?> getAccessToken() async => accessToken;

  @override
  Future<String?> getRefreshToken() async => refreshToken;

  User? user;

  @override
  Future<void> saveUser(User user) async {
    this.user = user;
  }

  @override
  Future<User?> getUser() async => user;

  @override
  Future<void> clearTokens() async {
    accessToken = null;
    refreshToken = null;
    user = null;
  }

  @override
  Future<bool> hasValidSession() async => refreshToken != null;
}

class _FakeAuthApiService extends AuthApiService {
  @override
  Future<AuthResponseBundle> register(RegisterRequest request) async {
    return AuthResponseBundle(
      tokens: AuthTokens(accessToken: 'access-123', refreshToken: 'refresh-123', expiresIn: 3600),
      user: User(
        id: 'usr-1',
        fullName: request.fullName,
        email: request.email,
        phone: request.phone,
        roles: ['ROLE_DONOR'],
        status: 'ACTIVE',
      ),
    );
  }
}

void main() {
  group('Password and Confirm Password Validation Regression Tests', () {
    late AuthController authController;

    setUp(() {
      authController = AuthController(
        apiService: _FakeAuthApiService(),
        tokenStorage: _FakeTokenStorage(),
        configureGlobalAuth: false,
      );
    });

    Widget createTestWidget() {
      return MaterialApp(
        home: AuthScope(
          controller: authController,
          child: const RegisterScreen(),
        ),
      );
    }

    testWidgets('Confirm password dynamically re-evaluates and clears error when matching',
        (tester) async {
      await tester.pumpWidget(createTestWidget());
      await tester.pumpAndSettle();

      // Find password and confirm password fields
      final textFormFieldFinder = find.byType(TextFormField);
      expect(textFormFieldFinder, findsWidgets);

      // In RegisterScreen:
      // Index 0: Full Name
      // Index 1: Email Address
      // Index 2: Mobile Number
      // Index 3: Password
      // Index 4: Confirm Password
      final passwordField = textFormFieldFinder.at(3);
      final confirmField = textFormFieldFinder.at(4);

      // 1. Enter password: "ValidPassword1"
      await tester.enterText(passwordField, 'ValidPassword1');
      await tester.pumpAndSettle();

      // Password checklist requirements should be visible
      expect(find.text("8+ characters"), findsOneWidget);
      expect(find.text("At least one uppercase letter"), findsOneWidget);
      expect(find.text("At least one lowercase letter"), findsOneWidget);
      expect(find.text("At least one number (0-9)"), findsOneWidget);

      // 2. Enter non-matching confirm password: "MismatchPassword2"
      await tester.enterText(confirmField, 'MismatchPassword2');
      await tester.pumpAndSettle();

      // Mismatch error must be shown dynamically
      expect(find.text("Passwords do not match"), findsOneWidget);

      // 3. Update Confirm Password to match: "ValidPassword1"
      await tester.enterText(confirmField, 'ValidPassword1');
      await tester.pumpAndSettle();

      // Error MUST dynamically disappear without needing submit button tap
      expect(find.text("Passwords do not match"), findsNothing);

      // 4. Change Password field to "AnotherPassword9": mismatch should immediately appear
      await tester.enterText(passwordField, 'AnotherPassword9');
      await tester.pumpAndSettle();
      expect(find.text("Passwords do not match"), findsOneWidget);

      // 5. Change Password back to "ValidPassword1": mismatch must immediately disappear
      await tester.enterText(passwordField, 'ValidPassword1');
      await tester.pumpAndSettle();
      expect(find.text("Passwords do not match"), findsNothing);
    });

    testWidgets('Password strength checklist dynamically reflects criteria',
        (tester) async {
      await tester.pumpWidget(createTestWidget());
      await tester.pumpAndSettle();

      final passwordFinder = find.byType(TextFormField).at(3);

      // Initially empty
      await tester.enterText(passwordFinder, 'abc');
      await tester.pumpAndSettle();

      // Only lowercase is satisfied
      final checkIcons = find.byIcon(Icons.check_circle_rounded);
      expect(checkIcons, findsNWidgets(1)); // Lowercase only

      // Add uppercase
      await tester.enterText(passwordFinder, 'abcABC');
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.check_circle_rounded), findsNWidgets(2)); // Lowercase + Uppercase

      // Add number
      await tester.enterText(passwordFinder, 'abcABC1');
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.check_circle_rounded), findsNWidgets(3)); // + Number

      // Add 8th char to satisfy length
      await tester.enterText(passwordFinder, 'abcABC12');
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.check_circle_rounded), findsNWidgets(4)); // All 4 satisfied
    });
  });
}
