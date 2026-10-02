import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/core/network/network_exception.dart';
import 'package:netra_app/features/auth/models/auth_models.dart';
import 'package:netra_app/features/auth/screens/verify_email_screen.dart';
import 'package:netra_app/features/auth/services/auth_api_service.dart';
import 'package:netra_app/features/auth/services/secure_token_storage.dart';
import 'package:netra_app/features/auth/state/auth_controller.dart';
import 'package:netra_app/features/auth/state/auth_scope.dart';
import 'package:netra_app/features/events/models/event_registration.dart';
import 'package:netra_app/features/events/widgets/participant_registration_dialog.dart';

class MockAuthApiService extends AuthApiService {
  bool verifyCalled = false;
  bool resendCalled = false;
  String? lastVerifiedCode;

  @override
  Future<AuthResponseBundle> verifyEmail({
    required String email,
    required String code,
  }) async {
    verifyCalled = true;
    lastVerifiedCode = code;
    if (code == '123456') {
      return AuthResponseBundle(
        user: const User(
          id: 'user-1',
          fullName: 'Test Donor',
          email: 'test@donor.org',
          phone: '+919876543210',
          roles: ['ROLE_DONOR'],
          status: 'ACTIVE',
        ),
        tokens: const AuthTokens(
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
          expiresIn: 3600,
        ),
      );
    }
    throw const ValidationException('Invalid or expired verification code.');
  }

  @override
  Future<void> resendVerification({required String email}) async {
    resendCalled = true;
  }

  @override
  Future<AuthResponseBundle> login(LoginRequest request) async {
    if (request.email == 'unverified@test.org') {
      throw const AccountNotVerifiedException(
        'Account created. Verify your email to continue.',
        email: 'unverified@test.org',
      );
    }
    return super.login(request);
  }
}

class MockTokenStorage implements SecureTokenStorage {
  String? accessToken;
  String? refreshToken;
  User? user;

  @override
  Future<void> clearTokens() async {
    accessToken = null;
    refreshToken = null;
    user = null;
  }

  @override
  Future<bool> hasValidSession() async => accessToken != null;

  @override
  Future<String?> getAccessToken() async => accessToken;

  @override
  Future<String?> getRefreshToken() async => refreshToken;

  @override
  Future<User?> getUser() async => user;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @override
  Future<void> saveUser(User user) async {
    this.user = user;
  }
}

void main() {
  group('AuthController Email Verification & Unverified State Tests', () {
    late MockAuthApiService mockApi;
    late MockTokenStorage mockStorage;
    late AuthController controller;

    setUp(() {
      mockApi = MockAuthApiService();
      mockStorage = MockTokenStorage();
      controller = AuthController(
        apiService: mockApi,
        tokenStorage: mockStorage,
        configureGlobalAuth: false,
      );
    });

    test('Login with unverified email sets isUnverified=true and records unverifiedEmail', () async {
      final success = await controller.login('unverified@test.org', 'Password123!');
      expect(success, isFalse);
      expect(controller.isUnverified, isTrue);
      expect(controller.unverifiedEmail, 'unverified@test.org');
      expect(controller.errorMessage, contains('Verify your email'));
    });

    test('verifyEmail with correct code saves tokens and sets authenticated state', () async {
      final success = await controller.verifyEmail(
        email: 'test@donor.org',
        code: '123456',
      );
      expect(success, isTrue);
      expect(controller.isAuthenticated, isTrue);
      expect(controller.isUnverified, isFalse);
      expect(controller.currentUser?.fullName, 'Test Donor');
      expect(mockStorage.accessToken, 'test-access-token');
    });

    test('verifyEmail with wrong code fails and retains unauthenticated state', () async {
      final success = await controller.verifyEmail(
        email: 'test@donor.org',
        code: '000000',
      );
      expect(success, isFalse);
      expect(controller.isAuthenticated, isFalse);
      expect(controller.errorMessage, contains('Invalid or expired'));
    });

    test('resendVerification calls API service and returns true', () async {
      final success = await controller.resendVerification(email: 'test@donor.org');
      expect(success, isTrue);
      expect(mockApi.resendCalled, isTrue);
    });
  });

  group('ParticipantRegistrationData Serialization Tests', () {
    test('Correctly serializes participant form data to backend JSON format', () {
      final data = ParticipantRegistrationData(
        fullName: 'Rahul Sharma',
        dateOfBirth: DateTime(2000, 5, 15),
        phone: '+919876543210',
        email: 'rahul.sharma@example.com',
        bloodGroup: 'O+',
        address: 'Flat 402, Block B, Salt Lake',
        city: 'Kolkata',
        emergencyContactName: 'Anita Sharma',
        emergencyContactPhone: '+919876543211',
        consentConfirmed: true,
      );

      final json = data.toJson();
      expect(json['fullName'], 'Rahul Sharma');
      expect(json['dateOfBirth'], '2000-05-15');
      expect(json['phone'], '+919876543210');
      expect(json['email'], 'rahul.sharma@example.com');
      expect(json['bloodGroup'], 'O+');
      expect(json['address'], 'Flat 402, Block B, Salt Lake');
      expect(json['city'], 'Kolkata');
      expect(json['emergencyContactName'], 'Anita Sharma');
      expect(json['emergencyContactPhone'], '+919876543211');
      expect(json['consentConfirmed'], isTrue);
    });
  });

  group('VerifyEmailScreen Widget Tests', () {
    late MockAuthApiService mockApi;
    late MockTokenStorage mockStorage;
    late AuthController controller;

    setUp(() {
      mockApi = MockAuthApiService();
      mockStorage = MockTokenStorage();
      controller = AuthController(
        apiService: mockApi,
        tokenStorage: mockStorage,
        configureGlobalAuth: false,
      );
    });

    testWidgets('Renders email, code input field, and verification button', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: controller,
            child: const VerifyEmailScreen(email: 'donor@example.org'),
          ),
        ),
      );

      expect(find.text('Verify Your Account'), findsOneWidget);
      expect(find.text('donor@example.org'), findsOneWidget);
      expect(find.text('Enter 6-Digit Code'), findsOneWidget);
      expect(find.text('Verify Email'), findsOneWidget);
      expect(find.byType(TextFormField), findsOneWidget);
    });

    testWidgets('Entering 6-digit code triggers verifyEmail', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: AuthScope(
            controller: controller,
            child: const VerifyEmailScreen(email: 'donor@example.org'),
          ),
        ),
      );

      await tester.enterText(find.byType(TextFormField), '123456');
      await tester.tap(find.text('Verify Email'));
      await tester.pumpAndSettle();

      expect(mockApi.verifyCalled, isTrue);
      expect(mockApi.lastVerifiedCode, '123456');
      expect(find.text('Account Verified'), findsOneWidget);
    });
  });

  group('ParticipantRegistrationDialog Widget Tests', () {
    testWidgets('Renders all participant fields, declarations, and consent', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  showDialog(
                    context: context,
                    builder: (_) => const ParticipantRegistrationDialog(
                      eventTitle: 'Mega Blood Donation Drive',
                      venueName: 'Apollo Blood Centre',
                      initialFullName: 'Test Donor',
                      initialEmail: 'test@donor.org',
                      initialPhone: '9876543210',
                    ),
                  );
                },
                child: const Text('Open Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Dialog'));
      await tester.pumpAndSettle();

      expect(find.text('Participant Registration'), findsOneWidget);
      expect(find.text('Mega Blood Donation Drive'), findsOneWidget);
      expect(find.text('Full Legal Name *'), findsOneWidget);
      expect(find.text('Test Donor'), findsOneWidget);
      expect(find.text('Date of Birth *'), findsOneWidget);
      expect(find.text('Blood Group *'), findsOneWidget);
      expect(find.textContaining('Emergency Contact'), findsWidgets);
      expect(find.textContaining('Pre-Screening Self-Declaration'), findsOneWidget);
      expect(find.text('Confirm & Register'), findsOneWidget);
    });
  });
}
