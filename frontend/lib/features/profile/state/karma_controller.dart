import 'package:flutter/foundation.dart';
import '../models/karma_models.dart';
import '../services/karma_api_service.dart';

class KarmaController extends ChangeNotifier {
  final KarmaApiService _apiService;

  KarmaSummaryModel? _summary;
  List<KarmaTransactionModel> _transactions = [];
  bool _isLoading = false;
  String? _errorMessage;

  KarmaController({KarmaApiService? apiService})
      : _apiService = apiService ?? KarmaApiService();

  KarmaSummaryModel? get summary => _summary;
  List<KarmaTransactionModel> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> loadKarma(String accessToken) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _summary = await _apiService.getMyKarmaSummary(accessToken);
      _transactions = await _apiService.getMyTransactions(accessToken);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
