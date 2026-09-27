import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/blood_request/models/blood_request.dart';

void main() {
  group('BloodRequestDetail Model Serialization & Security Invariants', () {
    test('BloodRequestDetail.fromJson parses full owner payload accurately', () {
      final ownerJson = {
        'id': '8dae1a48-5188-41e6-a63e-711125f850a6',
        'bloodGroup': 'O_POSITIVE',
        'unitsRequired': 2,
        'urgency': 'CRITICAL',
        'hospitalName': 'KEM Hospital, Parel',
        'city': 'Mumbai',
        'state': 'Maharashtra',
        'status': 'OPEN',
        'requiredBy': '2026-09-27T19:40:00.000Z',
        'createdAt': '2026-09-27T13:40:00.000Z',
        'updatedAt': '2026-09-27T13:41:00.000Z',
        'requesterUserId': 'user-123',
        'isOwner': true,
        'canManage': true,
        'hospitalAddress': 'Acharya Donde Marg, Parel',
        'postalCode': '400012',
        'latitude': 18.9986,
        'longitude': 72.8426,
        'description': 'Urgent requirement for emergency surgery.',
      };

      final detail = BloodRequestDetail.fromJson(ownerJson);
      expect(detail.id, equals('8dae1a48-5188-41e6-a63e-711125f850a6'));
      expect(detail.bloodGroup, equals('O_POSITIVE'));
      expect(detail.unitsRequired, equals(2));
      expect(detail.urgency, equals(BloodRequestUrgency.critical));
      expect(detail.hospitalName, equals('KEM Hospital, Parel'));
      expect(detail.isOwner, isTrue);
      expect(detail.canManage, isTrue);
      expect(detail.hospitalAddress, equals('Acharya Donde Marg, Parel'));
      expect(detail.postalCode, equals('400012'));
      expect(detail.latitude, equals(18.9986));
      expect(detail.longitude, equals(72.8426));
      expect(detail.description, equals('Urgent requirement for emergency surgery.'));
      expect(detail.updatedAt, equals(DateTime.parse('2026-09-27T13:41:00.000Z')));
    });

    test('BloodRequestDetail.fromJson parses public/non-owner payload with stripped privacy fields', () {
      // Backend strips updatedAt, hospitalAddress, postalCode, latitude, longitude, description, requesterUserId
      // on public GET /api/v1/blood-requests/{id} to prevent unauthorized metadata leakage
      final publicJson = {
        'id': '8dae1a48-5188-41e6-a63e-711125f850a6',
        'bloodGroup': 'O_POSITIVE',
        'unitsRequired': 2,
        'urgency': 'CRITICAL',
        'hospitalName': 'KEM Hospital, Parel',
        'city': 'Mumbai',
        'state': 'Maharashtra',
        'status': 'OPEN',
        'requiredBy': '2026-09-27T19:40:00.000Z',
        'createdAt': '2026-09-27T13:40:00.000Z',
        'isOwner': false,
        'canManage': false,
        // updatedAt, hospitalAddress, postalCode, latitude, longitude, description are omitted/null
      };

      final detail = BloodRequestDetail.fromJson(publicJson);
      expect(detail.id, equals('8dae1a48-5188-41e6-a63e-711125f850a6'));
      expect(detail.bloodGroup, equals('O_POSITIVE'));
      expect(detail.unitsRequired, equals(2));
      expect(detail.urgency, equals(BloodRequestUrgency.critical));
      expect(detail.isOwner, isFalse);
      expect(detail.canManage, isFalse);
      expect(detail.hospitalAddress, equals(''));
      expect(detail.postalCode, equals(''));
      expect(detail.latitude, isNull);
      expect(detail.longitude, isNull);
      expect(detail.description, isNull);
      expect(detail.updatedAt, equals(DateTime.parse('2026-09-27T13:40:00.000Z')));
    });
  });
}
