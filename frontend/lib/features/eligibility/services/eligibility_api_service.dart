import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
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
  final http.Client _client;
  String? _cachedCapabilityToken;

  EligibilityApiService({
    String? baseUrl,
    http.Client? client,
  })  : baseUrl = baseUrl ?? 'http://localhost:8080/api/v1/eligibility',
        _client = client ?? http.Client();

  String? get currentCapabilityToken => _cachedCapabilityToken;
  set currentCapabilityToken(String? token) => _cachedCapabilityToken = token;

  Map<String, String> _buildHeaders([String? capabilityToken]) {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    final effectiveToken = capabilityToken ?? _cachedCapabilityToken;
    if (effectiveToken != null && effectiveToken.isNotEmpty) {
      headers['X-Capability-Token'] = effectiveToken;
    }
    return headers;
  }

  Future<String> fetchRuleVersion() async {
    try {
      final response = await _client.get(Uri.parse('$baseUrl/rules/version'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['activeVersion'] ?? 'INDIA-NBTC-2026-01';
      }
      throw EligibilityApiException('Failed to load rules version', response.statusCode);
    } on SocketException {
      throw EligibilityApiException('Unable to reach NETRA rules engine. Please check your internet connection.');
    }
  }

  Future<String> createSession() async {
    final sessionInfo = await createSessionDetailed();
    return sessionInfo.sessionId;
  }

  Future<EligibilitySessionInfo> createSessionDetailed() async {
    try {
      final response = await _client.post(
        Uri.parse('$baseUrl/sessions'),
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        body: jsonEncode({
          'clientTimestamp': DateTime.now().toUtc().toIso8601String(),
        }),
      );

      if (response.statusCode == 201) {
        final data = jsonDecode(response.body);
        final sessionId = data['sessionId'] as String;
        final ruleVersion = data['ruleVersion'] as String? ?? 'INDIA-NBTC-2026-01';
        final capabilityToken = data['capabilityToken'] as String?;
        _cachedCapabilityToken = capabilityToken;

        DateTime? expiresAt;
        if (data['expiresAt'] != null) {
          expiresAt = DateTime.tryParse(data['expiresAt']);
        }

        return EligibilitySessionInfo(
          sessionId: sessionId,
          ruleVersion: ruleVersion,
          capabilityToken: capabilityToken,
          expiresAt: expiresAt,
        );
      }
      throw EligibilityApiException('Failed to create screening session', response.statusCode);
    } on SocketException {
      throw EligibilityApiException('Unable to reach NETRA service. Pre-screening requires active clinical connection.');
    }
  }

  Future<void> submitAnswers(String sessionId, Map<String, String> answers, {String? capabilityToken}) async {
    try {
      final list = answers.entries
          .map((e) => {'questionKey': e.key, 'value': e.value})
          .toList();

      final response = await _client.post(
        Uri.parse('$baseUrl/sessions/$sessionId/answers'),
        headers: _buildHeaders(capabilityToken),
        body: jsonEncode({'answers': list}),
      );

      if (response.statusCode != 200) {
        throw EligibilityApiException('Failed to save screening responses', response.statusCode);
      }
    } on SocketException {
      throw EligibilityApiException('Network error while saving answers.');
    }
  }

  Future<EligibilityResult> checkEligibility(String sessionId, {String? capabilityToken}) async {
    try {
      // Critical Zero-Trust Guard: Request body contains NO result parameter
      final response = await _client.post(
        Uri.parse('$baseUrl/sessions/$sessionId/check'),
        headers: _buildHeaders(capabilityToken),
        body: jsonEncode({'clientReviewConfirmed': true}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return EligibilityResult.fromJson(data);
      }
      throw EligibilityApiException('Failed to evaluate donation eligibility', response.statusCode);
    } on SocketException {
      throw EligibilityApiException('Unable to verify eligibility offline. Donor safety requires verified server evaluation.');
    }
  }

  Future<EligibilityResult> getSessionResult(String sessionId, {String? capabilityToken}) async {
    try {
      final response = await _client.get(
        Uri.parse('$baseUrl/sessions/$sessionId/result'),
        headers: _buildHeaders(capabilityToken),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return EligibilityResult.fromJson(data);
      }
      throw EligibilityApiException('Failed to retrieve evaluation result', response.statusCode);
    } on SocketException {
      throw EligibilityApiException('Unable to load screening result. Please check connection.');
    }
  }
}
