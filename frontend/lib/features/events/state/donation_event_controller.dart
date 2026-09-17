import 'package:flutter/foundation.dart';
import '../../../core/network/network_exception.dart';
import '../models/donation_event.dart';
import '../models/event_registration.dart';
import '../services/donation_event_api_service.dart';

class DonationEventController extends ChangeNotifier {
  final DonationEventApiService _apiService;

  List<DonationEventSummary> _events = [];
  List<EventRegistration> _myRegistrations = [];
  DonationEventDetail? _currentEvent;
  EventRegistration? _currentRegistration;
  bool _isLoading = false;
  bool _isActionLoading = false;
  String? _errorMessage;
  String? _actionSuccessMessage;
  String? _selectedCity;
  bool _upcomingOnly = false;

  DonationEventController({DonationEventApiService? apiService})
      : _apiService = apiService ?? DonationEventApiService();

  List<DonationEventSummary> get events => _events;
  List<EventRegistration> get myRegistrations => _myRegistrations;
  DonationEventDetail? get currentEvent => _currentEvent;
  EventRegistration? get currentRegistration => _currentRegistration;
  bool get isLoading => _isLoading;
  bool get isActionLoading => _isActionLoading;
  String? get errorMessage => _errorMessage;
  String? get actionSuccessMessage => _actionSuccessMessage;
  String? get selectedCity => _selectedCity;
  bool get upcomingOnly => _upcomingOnly;

  void setCity(String? city) {
    _selectedCity = city;
    notifyListeners();
    loadEvents();
  }

  void setUpcomingOnly(bool value) {
    _upcomingOnly = value;
    notifyListeners();
    loadEvents();
  }

  Future<void> loadEvents() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _events = await _apiService.discoverEvents(
        city: _selectedCity,
        upcomingOnly: _upcomingOnly,
      );
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Unable to load donation camps. Please check your connection.';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadNearbyEvents({required double latitude, required double longitude, double radiusKm = 10.0}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _events = await _apiService.getNearbyEvents(
        latitude: latitude,
        longitude: longitude,
        radiusKm: radiusKm,
      );
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Unable to find nearby camps.';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadEventDetails(String eventId) async {
    _isLoading = true;
    _errorMessage = null;
    _currentRegistration = null;
    notifyListeners();

    try {
      _currentEvent = await _apiService.getEventDetails(eventId);
      _currentRegistration = await _apiService.getMyRegistration(eventId);
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Failed to load camp details.';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> registerForEvent(String eventId) async {
    _isActionLoading = true;
    _errorMessage = null;
    _actionSuccessMessage = null;
    notifyListeners();

    try {
      final reg = await _apiService.registerForEvent(eventId);
      _currentRegistration = reg;
      _actionSuccessMessage = "You're registered! Please remember to bring government ID on the day of the camp.";
      // Refresh current event details for updated slot count
      if (_currentEvent != null && _currentEvent!.id == eventId) {
        _currentEvent = await _apiService.getEventDetails(eventId);
      }
      return true;
    } catch (e) {
      if (e is ConflictException) {
        if (e.message.contains('ALREADY_REGISTERED') || e.message.toLowerCase().contains('already registered')) {
          _errorMessage = 'You are already registered for this donation camp.';
        } else if (e.message.contains('EVENT_FULL') || e.message.toLowerCase().contains('capacity')) {
          _errorMessage = 'This donation camp has reached maximum capacity. Please check back later or view nearby camps.';
        } else {
          _errorMessage = e.message;
        }
      } else if (e is NetworkException) {
        _errorMessage = e.message;
      } else {
        _errorMessage = 'Registration could not be completed. Please try again.';
      }
      return false;
    } finally {
      _isActionLoading = false;
      notifyListeners();
    }
  }

  Future<bool> cancelRegistration(String eventId) async {
    _isActionLoading = true;
    _errorMessage = null;
    _actionSuccessMessage = null;
    notifyListeners();

    try {
      final reg = await _apiService.cancelRegistration(eventId);
      _currentRegistration = reg;
      _actionSuccessMessage = 'Your registration for this donation camp has been cancelled.';
      if (_currentEvent != null && _currentEvent!.id == eventId) {
        _currentEvent = await _apiService.getEventDetails(eventId);
      }
      return true;
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Could not cancel registration.';
      return false;
    } finally {
      _isActionLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadMyRegistrations() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _myRegistrations = await _apiService.getMyRegistrations();
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Unable to load registrations.';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<DonationEventDetail?> createEvent(Map<String, dynamic> payload) async {
    _isActionLoading = true;
    _errorMessage = null;
    _actionSuccessMessage = null;
    notifyListeners();

    try {
      final created = await _apiService.createEvent(payload);
      _actionSuccessMessage = 'Donation camp created successfully as draft.';
      return created;
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Failed to create donation camp.';
      return null;
    } finally {
      _isActionLoading = false;
      notifyListeners();
    }
  }

  Future<DonationEventDetail?> submitEvent(String eventId) async {
    _isActionLoading = true;
    _errorMessage = null;
    _actionSuccessMessage = null;
    notifyListeners();

    try {
      final updated = await _apiService.submitEvent(eventId);
      _actionSuccessMessage = 'Donation camp submitted for admin review.';
      return updated;
    } catch (e) {
      _errorMessage = e is NetworkException ? e.message : 'Failed to submit donation camp.';
      return null;
    } finally {
      _isActionLoading = false;
      notifyListeners();
    }
  }

  void clearMessages() {
    _errorMessage = null;
    _actionSuccessMessage = null;
    notifyListeners();
  }
}
