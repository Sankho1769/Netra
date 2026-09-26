/// Authentication & User Identity models for NETRA.
/// Zero clinical or medical data is captured at this level.

enum AuthStatus {
  unknown,
  authenticating,
  authenticated,
  unauthenticated,
  error,
}

class User {
  final String id;
  final String fullName;
  final String email;
  final String? phone;
  final List<String> roles;
  final String status;

  const User({
    required this.id,
    required this.fullName,
    required this.email,
    this.phone,
    required this.roles,
    required this.status,
  });

  bool get isActive => status.toUpperCase() == 'ACTIVE';
  bool get isDonor => roles.contains('ROLE_DONOR');
  bool get isAdmin => roles.contains('ROLE_ADMIN');
  bool get isOrganization => roles.contains('ROLE_ORGANIZATION');
  bool get isBloodBank => roles.contains('ROLE_BLOODBANK');

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      fullName: json['fullName'] as String? ?? '',
      email: json['email'] as String? ?? '',
      phone: json['phone'] as String?,
      roles: (json['roles'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          ['ROLE_DONOR'],
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'fullName': fullName,
        'email': email,
        if (phone != null) 'phone': phone,
        'roles': roles,
        'status': status,
      };
}

class AuthTokens {
  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresIn;

  const AuthTokens({
    required this.accessToken,
    required this.refreshToken,
    this.tokenType = 'Bearer',
    required this.expiresIn,
  });

  factory AuthTokens.fromJson(Map<String, dynamic> json) {
    return AuthTokens(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      tokenType: json['tokenType'] as String? ?? 'Bearer',
      expiresIn: (json['expiresIn'] as num?)?.toInt() ?? 900,
    );
  }

  Map<String, dynamic> toJson() => {
        'accessToken': accessToken,
        'refreshToken': refreshToken,
        'tokenType': tokenType,
        'expiresIn': expiresIn,
      };
}

class LoginRequest {
  final String email;
  final String password;
  final String? deviceId;

  const LoginRequest({
    required this.email,
    required this.password,
    this.deviceId,
  });

  Map<String, dynamic> toJson() => {
        'email': email.trim().toLowerCase(),
        'password': password,
        if (deviceId != null) 'deviceId': deviceId,
      };
}

class RegisterRequest {
  final String fullName;
  final String email;
  final String phone;
  final String password;

  const RegisterRequest({
    required this.fullName,
    required this.email,
    required this.phone,
    required this.password,
  });

  Map<String, dynamic> toJson() => {
        'fullName': fullName.trim(),
        'email': email.trim().toLowerCase(),
        'phone': phone.trim(),
        'password': password,
      };
}
