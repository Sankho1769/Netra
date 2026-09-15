import 'package:flutter/material.dart';
import '../models/donor_profile.dart';
import '../services/donor_api_service.dart';

class DonorController extends ChangeNotifier {
  final DonorApiService _apiService;

  DonorProfile? _donorProfile;
  bool _isLoading = false;
  bool _isInitialized = false;
  String? _errorMessage;

  DonorController({DonorApiService? apiService})
      : _apiService = apiService ?? DonorApiService();

  DonorProfile? get donorProfile => _donorProfile;
  bool get isLoading => _isLoading;
  bool get isInitialized => _isInitialized;
  bool get hasProfile => _donorProfile != null;
  String? get errorMessage => _errorMessage;

  Future<void> loadDonorProfile(String accessToken) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _donorProfile = await _apiService.getDonorProfile(accessToken);
      _isInitialized = true;
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> createDonorProfile(
    String accessToken,
    CreateDonorProfileRequest request,
  ) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _donorProfile = await _apiService.createDonorProfile(accessToken, request);
      _isInitialized = true;
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateDonorProfile(
    String accessToken,
    UpdateDonorProfileRequest request,
  ) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _donorProfile = await _apiService.updateDonorProfile(accessToken, request);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
