import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../models/donor_profile.dart';

class DonorApiService {
  final ApiClient _client;

  DonorApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(
                baseUrl:
                    baseUrl ?? 'http://localhost:8080/api/v1/donor/profile');

  Future<DonorProfile?> getDonorProfile(String accessToken) async {
    try {
      final response = await _client.get(
        '',
        headers: {'Authorization': 'Bearer $accessToken'},
      );
      return DonorProfile.fromJson(response as Map<String, dynamic>);
    } on NotFoundException {
      // 404 indicates user has not created a donor profile yet
      return null;
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load donor profile. Please try again.');
    }
  }

  Future<DonorProfile> createDonorProfile(
    String accessToken,
    CreateDonorProfileRequest request,
  ) async {
    try {
      final response = await _client.post(
        '',
        headers: {'Authorization': 'Bearer $accessToken'},
        body: request.toJson(),
      );
      return DonorProfile.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to create donor profile. Please check your inputs.');
    }
  }

  Future<DonorProfile> updateDonorProfile(
    String accessToken,
    UpdateDonorProfileRequest request,
  ) async {
    try {
      final response = await _client.put(
        '',
        headers: {'Authorization': 'Bearer $accessToken'},
        body: request.toJson(),
      );
      return DonorProfile.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to update donor profile. Please try again.');
    }
  }
}
