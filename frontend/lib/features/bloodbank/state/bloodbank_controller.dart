import 'package:flutter/foundation.dart';
import '../models/blood_bank.dart';
import '../services/bloodbank_api_service.dart';

class BloodBankController extends ChangeNotifier {
  final BloodBankApiService _apiService;

  List<BloodBankSummary> _bloodBanks = [];
  BloodBankDetail? _selectedBloodBank;
  bool _isLoading = false;
  String? _errorMessage;
  String? _currentCity;
  String? _selectedBloodGroup;

  BloodBankController({BloodBankApiService? apiService})
      : _apiService = apiService ?? BloodBankApiService();

  List<BloodBankSummary> get bloodBanks => _bloodBanks;
  BloodBankDetail? get selectedBloodBank => _selectedBloodBank;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get currentCity => _currentCity;
  String? get selectedBloodGroup => _selectedBloodGroup;

  Future<void> loadNearbyBloodBanks({
    required double latitude,
    required double longitude,
    double radiusKm = 10.0,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _bloodBanks = await _apiService.getNearbyBloodBanks(
        latitude: latitude,
        longitude: longitude,
        radiusKm: radiusKm,
      );
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> searchByCity(String city, {String? bloodGroup}) async {
    _isLoading = true;
    _errorMessage = null;
    _currentCity = city;
    if (bloodGroup != null) {
      _selectedBloodGroup = bloodGroup;
    }
    notifyListeners();

    try {
      _bloodBanks = await _apiService.discoverBloodBanks(
        city: city,
        bloodGroup: _selectedBloodGroup,
      );
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadBloodBankDetails(String id,
      {double? userLat, double? userLon}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _selectedBloodBank = await _apiService.getBloodBankDetails(
        id,
        userLat: userLat,
        userLon: userLon,
      );
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void setBloodGroupFilter(String? bloodGroup) {
    if (_selectedBloodGroup == bloodGroup) return;
    _selectedBloodGroup = bloodGroup;
    if (_currentCity != null && _currentCity!.isNotEmpty) {
      searchByCity(_currentCity!, bloodGroup: _selectedBloodGroup);
    } else {
      notifyListeners();
    }
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
