import 'package:flutter/foundation.dart';
import '../models/blood_request.dart';
import '../services/blood_request_api_service.dart';

class BloodRequestController extends ChangeNotifier {
  final BloodRequestApiService _apiService;

  BloodRequestController({BloodRequestApiService? apiService})
      : _apiService = apiService ?? BloodRequestApiService();

  List<BloodRequestSummary> _discoverableRequests = [];
  List<BloodRequestSummary> get discoverableRequests => _discoverableRequests;

  List<BloodRequestSummary> _myRequests = [];
  List<BloodRequestSummary> get myRequests => _myRequests;

  List<BloodRequestSummary> _nearbyRequests = [];
  List<BloodRequestSummary> get nearbyRequests => _nearbyRequests;

  BloodRequestDetail? _selectedRequest;
  BloodRequestDetail? get selectedRequest => _selectedRequest;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  bool _isSubmitting = false;
  bool get isSubmitting => _isSubmitting;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  String? _filterBloodGroup;
  String? get filterBloodGroup => _filterBloodGroup;

  String? _filterCity;
  String? get filterCity => _filterCity;

  String? _filterUrgency;
  String? get filterUrgency => _filterUrgency;

  Future<void> loadDiscoverableRequests({bool refresh = false}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _discoverableRequests = await _apiService.discoverRequests(
        bloodGroup: _filterBloodGroup,
        city: _filterCity,
        urgency: _filterUrgency,
      );
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadMyRequests({bool refresh = false}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _myRequests = await _apiService.getMyRequests();
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadNearbyRequests({
    required double latitude,
    required double longitude,
    double radiusKm = 10.0,
    String? bloodGroup,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _nearbyRequests = await _apiService.findNearbyRequests(
        latitude: latitude,
        longitude: longitude,
        radiusKm: radiusKm,
        bloodGroup: bloodGroup ?? _filterBloodGroup,
      );
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<BloodRequestDetail?> loadRequestDetails(String id) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _selectedRequest = await _apiService.getRequestDetails(id);
      return _selectedRequest;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      return null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<BloodRequestDetail?> createRequest(Map<String, dynamic> payload) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final created = await _apiService.createRequest(payload);
      _selectedRequest = created;
      await loadDiscoverableRequests();
      await loadMyRequests();
      return created;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      return null;
    } finally {
      _isSubmitting = false;
      notifyListeners();
    }
  }

  Future<BloodRequestDetail?> updateRequest(String id, Map<String, dynamic> payload) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final updated = await _apiService.updateRequest(id, payload);
      _selectedRequest = updated;
      await loadDiscoverableRequests();
      await loadMyRequests();
      return updated;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      return null;
    } finally {
      _isSubmitting = false;
      notifyListeners();
    }
  }

  Future<bool> cancelRequest(String id, {String? reason}) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final cancelled = await _apiService.cancelRequest(id, reason: reason);
      _selectedRequest = cancelled;
      await loadDiscoverableRequests();
      await loadMyRequests();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      return false;
    } finally {
      _isSubmitting = false;
      notifyListeners();
    }
  }

  void setFilters({String? bloodGroup, String? city, String? urgency}) {
    _filterBloodGroup = bloodGroup;
    _filterCity = city;
    _filterUrgency = urgency;
    loadDiscoverableRequests();
  }

  void clearFilters() {
    _filterBloodGroup = null;
    _filterCity = null;
    _filterUrgency = null;
    loadDiscoverableRequests();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
