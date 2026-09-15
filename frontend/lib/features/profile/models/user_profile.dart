class UserProfile {
  final String id;
  final String fullName;
  final String email;
  final String? phone;
  final List<String> roles;
  final String status;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const UserProfile({
    required this.id,
    required this.fullName,
    required this.email,
    this.phone,
    required this.roles,
    required this.status,
    this.createdAt,
    this.updatedAt,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] as String,
      fullName: json['fullName'] as String? ?? '',
      email: json['email'] as String? ?? '',
      phone: json['phone'] as String?,
      roles: (json['roles'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      status: json['status'] as String? ?? 'ACTIVE',
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fullName': fullName,
      'email': email,
      'phone': phone,
      'roles': roles,
      'status': status,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }
}

class UpdateUserProfileRequest {
  final String fullName;
  final String? phone;

  const UpdateUserProfileRequest({
    required this.fullName,
    this.phone,
  });

  Map<String, dynamic> toJson() {
    return {
      'fullName': fullName,
      if (phone != null) 'phone': phone,
    };
  }
}
