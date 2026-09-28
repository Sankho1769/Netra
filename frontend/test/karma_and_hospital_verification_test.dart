import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/about/models/community_impact_model.dart';
import 'package:netra_app/features/blood_request/models/verified_hospital_model.dart';
import 'package:netra_app/features/blood_request/widgets/hospital_search_field.dart';
import 'package:netra_app/features/events/models/donation_event.dart';
import 'package:netra_app/features/events/services/donation_event_api_service.dart';
import 'package:netra_app/features/home/home_screen.dart';
import 'package:netra_app/features/profile/models/karma_models.dart';

class FakeDonationEventApiService extends Fake
    implements DonationEventApiService {
  final List<DonationEventSummary> events;

  FakeDonationEventApiService(this.events);

  @override
  Future<List<DonationEventSummary>> discoverEvents({
    String? city,
    String? bloodBankId,
    String? status,
    bool upcomingOnly = false,
    int page = 0,
    int size = 20,
  }) async {
    return events;
  }
}

void main() {
  group('Karma Models Tests', () {
    test('KarmaSummaryModel deserializes correctly', () {
      final json = {
        'userId': 'user-1',
        'balance': 250,
        'tier': 'GOLD',
        'totalTransactions': 5,
        'recentTransactions': [
          {
            'id': 'tx-1',
            'points': 50,
            'eventType': 'VERIFIED_DONATION_COMPLETED',
            'reason': 'Awarded for verified donation',
            'createdAt': '2026-09-27T10:00:00Z',
          }
        ]
      };

      final summary = KarmaSummaryModel.fromJson(json);
      expect(summary.userId, equals('user-1'));
      expect(summary.balance, equals(250));
      expect(summary.tier, equals('GOLD'));
      expect(summary.totalTransactions, equals(5));
      expect(summary.recentTransactions.length, equals(1));
      expect(summary.recentTransactions.first.points, equals(50));
      expect(summary.recentTransactions.first.isPositive, isTrue);
    });

    test('KarmaTransactionModel handles debit / penalty points', () {
      final json = {
        'id': 'tx-2',
        'points': -50,
        'eventType': 'FAKE_REQUEST_CONFIRMED',
        'reason': 'Fraud penalty applied',
        'createdAt': '2026-09-27T11:00:00Z',
      };

      final tx = KarmaTransactionModel.fromJson(json);
      expect(tx.points, equals(-50));
      expect(tx.isPositive, isFalse);
      expect(tx.eventType, equals('FAKE_REQUEST_CONFIRMED'));
    });
  });

  group('VerifiedHospitalModel Tests', () {
    test('VerifiedHospitalModel deserializes correctly and provides alias getters', () {
      final json = {
        'id': 'hosp-123',
        'name': 'KEM Hospital Mumbai',
        'address': 'Acharya Donde Marg, Parel',
        'city': 'Mumbai',
        'state': 'Maharashtra',
        'postalCode': '400012',
        'placeId': 'place-kem-123',
        'hasBloodBank': true,
        'verificationStatus': 'VERIFIED',
        'latitude': 18.9986,
        'longitude': 72.8428,
        'phone': '+912224107000',
      };

      final model = VerifiedHospitalModel.fromJson(json);
      expect(model.name, equals('KEM Hospital Mumbai'));
      expect(model.hospitalName, equals('KEM Hospital Mumbai'));
      expect(model.hospitalAddress, equals('Acharya Donde Marg, Parel'));
      expect(model.city, equals('Mumbai'));
      expect(model.state, equals('Maharashtra'));
      expect(model.placeId, equals('place-kem-123'));
      expect(model.isVerified, isTrue);
      expect(model.hasBloodBank, isTrue);
      expect(model.latitude, equals(18.9986));
      expect(model.longitude, equals(72.8428));
    });
  });

  group('CommunityImpactModel Tests', () {
    test('CommunityImpactModel handles empty and populated states', () {
      final empty = CommunityImpactModel.fromJson({
        'totalVerifiedDonations': 0,
        'totalUnitsCollected': 0,
        'activeDonorsCount': 0,
        'fulfilledRequestsCount': 0,
        'hasData': false,
        'notice': 'Community impact data will appear here once verified donations are recorded.',
      });

      expect(empty.hasData, isFalse);
      expect(empty.totalUnitsCollected, equals(0));
      expect(empty.notice, contains('once verified donations are recorded'));

      final populated = CommunityImpactModel.fromJson({
        'totalVerifiedDonations': 350,
        'totalUnitsCollected': 420,
        'activeDonorsCount': 280,
        'fulfilledRequestsCount': 310,
        'hasData': true,
      });

      expect(populated.hasData, isTrue);
      expect(populated.totalUnitsCollected, equals(420));
      expect(populated.totalVerifiedDonations, equals(350));
    });
  });

  group('HospitalSearchField Widget Tests', () {
    testWidgets('renders search field and accepts text input', (tester) async {
      VerifiedHospitalModel? selected;
      final controller = TextEditingController();

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Padding(
              padding: const EdgeInsets.all(16.0),
              child: HospitalSearchField(
                controller: controller,
                onHospitalSelected: (h) => selected = h,
              ),
            ),
          ),
        ),
      );

      expect(find.byType(HospitalSearchField), findsOneWidget);
      expect(find.byType(TextFormField), findsOneWidget);
      expect(find.text('Hospital or Medical Institution *'), findsOneWidget);

      await tester.enterText(find.byType(TextFormField), 'Lilavati');
      expect(controller.text, equals('Lilavati'));
      expect(selected, isNull);
    });
  });

  group('HomeScreen Dynamic Drive Card Tests', () {
    testWidgets('renders dynamic upcoming drive when available', (tester) async {
      final mockEvent = DonationEventSummary(
        id: 'event-101',
        bloodBankId: 'bank-1',
        bloodBankName: 'Rotary Blood Centre',
        title: 'Bandra Community Blood Drive',
        description: 'Annual drive',
        eventType: DonationEventType.bloodDonationCamp,
        status: DonationEventStatus.published,
        venueName: 'Bandra Community Hall',
        address: 'Hill Road, Bandra West',
        city: 'Mumbai',
        state: 'Maharashtra',
        postalCode: '400050',
        startAt: DateTime(2026, 10, 15, 9, 0),
        endAt: DateTime(2026, 10, 15, 17, 0),
        registrationOpenAt: DateTime(2026, 10, 1),
        registrationCloseAt: DateTime(2026, 10, 14),
        donorCapacity: 100,
        currentRegistrationCount: 25,
        remainingCapacity: 75,
        isRegistrationOpen: true,
      );

      await tester.pumpWidget(
        MaterialApp(
          home: HomeScreen(
            eventApiService: FakeDonationEventApiService([mockEvent]),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('FEATURED DRIVE'), findsOneWidget);
      expect(find.text('Bandra Community Blood Drive'), findsOneWidget);
      expect(find.text('Bandra Community Hall, Hill Road, Bandra West'), findsOneWidget);
      expect(find.text('View Event & Register'), findsOneWidget);
    });

    testWidgets('renders clean empty state notice when no drives scheduled', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: HomeScreen(
            eventApiService: FakeDonationEventApiService([]),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('COMMUNITY DRIVES'), findsOneWidget);
      expect(find.text('No Upcoming Drives Scheduled'), findsOneWidget);
      expect(
        find.text('Verified community blood donation drives and institutional camps will appear here when scheduled.'),
        findsOneWidget,
      );
      expect(find.text('Explore Events Directory'), findsOneWidget);
    });
  });
}
