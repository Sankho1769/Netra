import 'dart:math';
import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../../auth/services/secure_token_storage.dart';
import '../../blood_request/models/blood_request.dart';
import '../../bloodbank/models/blood_bank.dart';
import '../../bloodbank/services/bloodbank_api_service.dart';
import '../../blood_request/services/blood_request_api_service.dart';

class EmergencyApiService {
  final ApiClient _client;
  final SecureTokenStorage _tokenStorage;
  final BloodBankApiService _bloodBankApiService;
  final BloodRequestApiService _bloodRequestApiService;

  EmergencyApiService({
    ApiClient? client,
    SecureTokenStorage? tokenStorage,
    BloodBankApiService? bloodBankApiService,
    BloodRequestApiService? bloodRequestApiService,
    String? baseUrl,
  })  : _client = client ??
            ApiClient(baseUrl: baseUrl ?? 'http://localhost:8080/api/v1/emergency'),
        _tokenStorage = tokenStorage ?? PlatformSecureTokenStorage(),
        _bloodBankApiService = bloodBankApiService ?? BloodBankApiService(),
        _bloodRequestApiService = bloodRequestApiService ?? BloodRequestApiService();

  static String generateIdempotencyKey() {
    final random = Random.secure();
    final values = List<int>.generate(16, (i) => random.nextInt(256));
    // Set UUID v4 variant & version bits
    values[6] = (values[6] & 0x0f) | 0x40;
    values[8] = (values[8] & 0x3f) | 0x80;
    final hex = values.map((b) => b.toRadixString(16).padLeft(2, '0')).toList();
    return '${hex[0]}${hex[1]}${hex[2]}${hex[3]}-${hex[4]}${hex[5]}-${hex[6]}${hex[7]}-${hex[8]}${hex[9]}-${hex[10]}${hex[11]}${hex[12]}${hex[13]}${hex[14]}${hex[15]}';
  }

  Future<BloodRequestDetail> createEmergencyRequest(
    Map<String, dynamic> payload, {
    required String idempotencyKey,
  }) async {
    try {
      final token = await _tokenStorage.getAccessToken();
      final headers = <String, String>{
        'Idempotency-Key': idempotencyKey,
      };
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }

      final response = await _client.post(
        '/blood-requests',
        headers: headers,
        body: payload,
      );

      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to submit emergency blood request.');
    }
  }

  Future<BloodRequestDetail> cancelEmergencyRequest(
    String id, {
    String? reason,
  }) async {
    try {
      final token = await _tokenStorage.getAccessToken();
      final headers = <String, String>{};
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }

      final body = <String, dynamic>{};
      if (reason != null && reason.trim().isNotEmpty) {
        body['reason'] = reason.trim();
      }

      final response = await _client.post(
        '/blood-requests/$id/cancel',
        headers: headers,
        body: body,
      );

      return BloodRequestDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to cancel emergency request.');
    }
  }

  Future<List<BloodBankSummary>> getNearbyBloodBanks({
    required double latitude,
    required double longitude,
    double radiusKm = 15.0,
    String? bloodGroup,
  }) {
    return _bloodBankApiService.getNearbyBloodBanks(
      latitude: latitude,
      longitude: longitude,
      radiusKm: radiusKm,
      bloodGroup: bloodGroup,
    );
  }

  Future<List<BloodRequestSummary>> getNearbyBloodRequests({
    required double latitude,
    required double longitude,
    double radiusKm = 15.0,
    String? bloodGroup,
  }) {
    return _bloodRequestApiService.findNearbyRequests(
      latitude: latitude,
      longitude: longitude,
      radiusKm: radiusKm,
      bloodGroup: bloodGroup,
    );
  }
}
