import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../models/user_profile.dart';

class ProfileApiService {
  final ApiClient _client;

  ProfileApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(
                baseUrl: baseUrl ?? 'http://localhost:8080/api/v1/profile');

  Future<UserProfile> getMyProfile(String accessToken) async {
    try {
      final response = await _client.get(
        '/me',
        headers: {'Authorization': 'Bearer $accessToken'},
      );
      return UserProfile.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load profile. Please try again.');
    }
  }

  Future<UserProfile> updateMyProfile(
    String accessToken,
    UpdateUserProfileRequest request,
  ) async {
    try {
      final response = await _client.put(
        '/me',
        headers: {'Authorization': 'Bearer $accessToken'},
        body: request.toJson(),
      );
      return UserProfile.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to update profile. Please verify your details.');
    }
  }
}
