import '../../../core/network/api_client.dart';
import '../../../core/network/network_exception.dart';
import '../models/donation_event.dart';
import '../models/event_registration.dart';

class DonationEventApiService {
  final ApiClient _client;

  DonationEventApiService({ApiClient? client, String? baseUrl})
      : _client = client ??
            ApiClient(
                baseUrl:
                    baseUrl ?? 'http://localhost:8080/api/v1/donation-events');

  Future<List<DonationEventSummary>> discoverEvents({
    String? city,
    String? bloodBankId,
    String? status,
    bool upcomingOnly = false,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        'upcomingOnly': upcomingOnly,
      };
      if (city != null && city.trim().isNotEmpty) {
        queryParams['city'] = city.trim();
      }
      if (bloodBankId != null && bloodBankId.trim().isNotEmpty) {
        queryParams['bloodBankId'] = bloodBankId.trim();
      }
      if (status != null && status.trim().isNotEmpty) {
        queryParams['status'] = status.trim();
      }

      final response = await _client.get('', queryParameters: queryParams);

      if (response is Map<String, dynamic> && response.containsKey('content')) {
        final list = response['content'] as List<dynamic>;
        return list
            .map(
                (e) => DonationEventSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      } else if (response is List<dynamic>) {
        return response
            .map(
                (e) => DonationEventSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to discover donation camps. Please try again.');
    }
  }

  Future<List<DonationEventSummary>> getNearbyEvents({
    required double latitude,
    required double longitude,
    double radiusKm = 10.0,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'latitude': latitude,
        'longitude': longitude,
        'radiusKm': radiusKm,
      };

      final response =
          await _client.get('/nearby', queryParameters: queryParams);

      if (response is List<dynamic>) {
        return response
            .map(
                (e) => DonationEventSummary.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to find nearby camps. Please try again.');
    }
  }

  Future<DonationEventDetail> getEventDetails(String id) async {
    try {
      final response = await _client.get('/$id');
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to load camp details.');
    }
  }

  Future<DonationEventDetail> createEvent(Map<String, dynamic> payload) async {
    try {
      final response = await _client.post('', body: payload);
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to create donation camp.');
    }
  }

  Future<DonationEventDetail> updateEvent(
      String id, Map<String, dynamic> payload) async {
    try {
      final response = await _client.put('/$id', body: payload);
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to update donation camp.');
    }
  }

  Future<DonationEventDetail> submitEvent(String id) async {
    try {
      final response = await _client.post('/$id/submit');
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException(
          'Failed to submit donation camp for approval.');
    }
  }

  Future<DonationEventDetail> approveEvent(String id, String status,
      {String? rejectionReason}) async {
    try {
      final response = await _client.patch(
        '/$id/approval',
        body: {
          'status': status,
          if (rejectionReason != null) 'rejectionReason': rejectionReason,
        },
      );
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to review donation camp.');
    }
  }

  Future<DonationEventDetail> cancelEvent(String id, {String? reason}) async {
    try {
      final response = await _client.post(
        '/$id/cancel',
        body: {
          if (reason != null) 'reason': reason,
        },
      );
      return DonationEventDetail.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to cancel donation camp.');
    }
  }

  Future<EventRegistration> registerForEvent(String eventId) async {
    try {
      final response = await _client.post('/$eventId/register');
      return EventRegistration.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Registration failed. Please try again.');
    }
  }

  Future<EventRegistration> cancelRegistration(String eventId) async {
    try {
      final response = await _client.delete('/$eventId/registration');
      return EventRegistration.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to cancel registration.');
    }
  }

  Future<EventRegistration?> getMyRegistration(String eventId) async {
    try {
      final response = await _client.get('/$eventId/registration');
      return EventRegistration.fromJson(response as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<EventRegistration>> getMyRegistrations() async {
    try {
      final response = await _client.get('/my-registrations');
      if (response is List<dynamic>) {
        return response
            .map((e) => EventRegistration.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to load registrations.');
    }
  }

  Future<List<EventAttendee>> getEventAttendees(String eventId) async {
    try {
      final response = await _client.get('/$eventId/registrations');
      if (response is List<dynamic>) {
        return response
            .map((e) => EventAttendee.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      if (e is NetworkException) rethrow;
      throw const ValidationException('Failed to load camp attendee list.');
    }
  }
}
