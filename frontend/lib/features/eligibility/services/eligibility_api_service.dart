import 'package:http/http.dart' as http;
import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/eligibility_models.dart';

class EligibilityApiException extends NetworkException {
  EligibilityApiException(super.message, [int? statusCode])
      : super(statusCode: statusCode);
}

class EligibilitySessionInfo {
  final String sessionId;
  final String ruleVersion;
  final String? capabilityToken;
  final DateTime? expiresAt;

  EligibilitySessionInfo({
    required this.sessionId,
    required this.ruleVersion,
    this.capabilityToken,
    this.expiresAt,
  });
}

class EligibilityApiService {
  final String baseUrl;
  final ApiClient _apiClient;
  String? _cachedCapabilityToken;

  EligibilityApiService({
    String? baseUrl,
    http.Client? client,
    ApiClient? apiClient,
  })  : baseUrl = baseUrl ?? ApiConfig.endpoint('/eligibility'),
        _apiClient = apiClient ??
            ApiClient(
              baseUrl: baseUrl ?? ApiConfig.endpoint('/eligibility'),
              client: client,
            );

  String? get currentCapabilityToken => _cachedCapabilityToken;
  set currentCapabilityToken(String? token) => _cachedCapabilityToken = token;

  Map<String, String> _buildHeaders([String? capabilityToken]) {
    final headers = <String, String>{};
    final effectiveToken = capabilityToken ?? _cachedCapabilityToken;
    if (effectiveToken != null && effectiveToken.isNotEmpty) {
      headers['X-Capability-Token'] = effectiveToken;
    }
    return headers;
  }

  Future<String> fetchRuleVersion() async {
    try {
      final data = await _apiClient.get('/rules/version');
      if (data is Map && data['activeVersion'] != null) {
        return data['activeVersion'].toString();
      }
      return 'INDIA-NBTC-2026-01';
    } on NetworkException {
      rethrow;
    } catch (e) {
      throw EligibilityApiException('Failed to load rules version: $e');
    }
  }

  Future<String> createSession() async {
    final sessionInfo = await createSessionDetailed();
    return sessionInfo.sessionId;
  }

  Future<EligibilitySessionInfo> createSessionDetailed() async {
    try {
      final data = await _apiClient.post(
        '/sessions',
        body: {
          'clientTimestamp': DateTime.now().toUtc().toIso8601String(),
        },
      );

      if (data is Map<String, dynamic>) {
        final sessionId = data['sessionId'] as String;
        final ruleVersion =
            data['ruleVersion'] as String? ?? 'INDIA-NBTC-2026-01';
        final capabilityToken = data['capabilityToken'] as String?;
        _cachedCapabilityToken = capabilityToken;

        DateTime? expiresAt;
        if (data['expiresAt'] != null) {
          expiresAt = DateTime.tryParse(data['expiresAt'].toString());
        }

        return EligibilitySessionInfo(
          sessionId: sessionId,
          ruleVersion: ruleVersion,
          capabilityToken: capabilityToken,
          expiresAt: expiresAt,
        );
      }
      throw EligibilityApiException(
          'Failed to create screening session: unexpected response format');
    } on NetworkException {
      rethrow;
    } catch (e) {
      throw EligibilityApiException('Failed to create screening session: $e');
    }
  }

  Future<void> submitAnswers(String sessionId, Map<String, String> answers,
      {String? capabilityToken}) async {
    try {
      final list = answers.entries
          .map((e) => {'questionKey': e.key, 'value': e.value})
          .toList();

      await _apiClient.post(
        '/sessions/$sessionId/answers',
        headers: _buildHeaders(capabilityToken),
        body: {'answers': list},
      );
    } on NetworkException {
      rethrow;
    } catch (e) {
      throw EligibilityApiException('Failed to save screening responses: $e');
    }
  }

  Future<EligibilityResult> checkEligibility(String sessionId,
      {String? capabilityToken}) async {
    try {
      // Critical Zero-Trust Guard: Request body contains NO client-calculated result parameter
      final data = await _apiClient.post(
        '/sessions/$sessionId/check',
        headers: _buildHeaders(capabilityToken),
        body: {'clientReviewConfirmed': true},
      );

      if (data is Map<String, dynamic>) {
        return EligibilityResult.fromJson(data);
      }
      throw EligibilityApiException(
          'Failed to evaluate donation eligibility: unexpected response format');
    } on NetworkException {
      rethrow;
    } catch (e) {
      throw EligibilityApiException(
          'Failed to evaluate donation eligibility: $e');
    }
  }

  Future<EligibilityResult> getSessionResult(String sessionId,
      {String? capabilityToken}) async {
    try {
      final data = await _apiClient.get(
        '/sessions/$sessionId/result',
        headers: _buildHeaders(capabilityToken),
      );

      if (data is Map<String, dynamic>) {
        return EligibilityResult.fromJson(data);
      }
      throw EligibilityApiException(
          'Failed to retrieve evaluation result: unexpected response format');
    } on NetworkException {
      rethrow;
    } catch (e) {
      throw EligibilityApiException('Failed to retrieve evaluation result: $e');
    }
  }
}
