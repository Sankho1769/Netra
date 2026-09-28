import '../../../core/network/api_client.dart';
import '../../../core/network/api_config.dart';
import '../../../core/network/network_exception.dart';
import '../models/community_impact_model.dart';
import '../models/community_timeseries_model.dart';

class CommunityMetricsApiService {
  final ApiClient _client;

  CommunityMetricsApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(baseUrl: baseUrl ?? ApiConfig.endpoint('/metrics'));

  Future<CommunityImpactModel> getCommunityImpact() async {
    try {
      final response = await _client.get('/community-impact');
      return CommunityImpactModel.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load community impact metrics. Please try again.');
    }
  }

  Future<CommunityTimeSeriesModel> getCommunityTimeSeries({int days = 30}) async {
    try {
      final response =
          await _client.get('/community-impact/timeseries?days=$days');
      return CommunityTimeSeriesModel.fromJson(
          response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to load community impact timeseries. Please try again.');
    }
  }
}
