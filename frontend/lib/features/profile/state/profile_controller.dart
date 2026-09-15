import 'package:flutter/material.dart';
import '../models/user_profile.dart';
import '../services/profile_api_service.dart';

class ProfileController extends ChangeNotifier {
  final ProfileApiService _apiService;

  UserProfile? _profile;
  bool _isLoading = false;
  String? _errorMessage;

  ProfileController({ProfileApiService? apiService})
      : _apiService = apiService ?? ProfileApiService();

  UserProfile? get profile => _profile;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  Future<void> loadProfile(String accessToken) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _profile = await _apiService.getMyProfile(accessToken);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> updateProfile(String accessToken, UpdateUserProfileRequest request) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _profile = await _apiService.updateMyProfile(accessToken, request);
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
