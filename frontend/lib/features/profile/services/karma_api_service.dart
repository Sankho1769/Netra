import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/karma_models.dart';

class KarmaApiService {
  final ApiClient _client;

  KarmaApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(baseUrl: baseUrl ?? ApiConfig.endpoint('/karma'));

  Future<KarmaSummaryModel> getMyKarmaSummary(String accessToken) async {
    try {
      final response = await _client.get(
        '/me',
        headers: {'Authorization': 'Bearer $accessToken'},
      );
      return KarmaSummaryModel.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load karma score. Please try again.');
    }
  }

  Future<List<KarmaTransactionModel>> getMyTransactions(
    String accessToken, {
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _client.get(
        '/transactions?page=$page&size=$size',
        headers: {'Authorization': 'Bearer $accessToken'},
      );
      if (response is Map<String, dynamic> && response.containsKey('content')) {
        final content = response['content'] as List<dynamic>;
        return content
            .map((item) =>
                KarmaTransactionModel.fromJson(item as Map<String, dynamic>))
            .toList();
      } else if (response is List<dynamic>) {
        return response
            .map((item) =>
                KarmaTransactionModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load karma transactions. Please try again.');
    }
  }
}
