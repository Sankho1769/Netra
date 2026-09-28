import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netra_app/features/about/models/community_impact_model.dart';
import 'package:netra_app/features/about/models/community_timeseries_model.dart';
import 'package:netra_app/features/about/screens/about_screen.dart';
import 'package:netra_app/features/about/services/community_metrics_api_service.dart';
import 'package:netra_app/features/blood_request/models/blood_request.dart';
import 'package:netra_app/features/emergency/screens/emergency_request_created_screen.dart';

class MockTimeSeriesMetricsService extends Fake
    implements CommunityMetricsApiService {
  final CommunityImpactModel impactModel;
  final Map<int, CommunityTimeSeriesModel> timeSeriesByDays;

  MockTimeSeriesMetricsService({
    required this.impactModel,
    required this.timeSeriesByDays,
  });

  @override
  Future<CommunityImpactModel> getCommunityImpact() async => impactModel;

  @override
  Future<CommunityTimeSeriesModel> getCommunityTimeSeries(
      {int days = 30}) async {
    return timeSeriesByDays[days] ??
        CommunityTimeSeriesModel(
          days: days,
          dataPoints: const [],
          totalRequestsReceived: 0,
          totalRequestsFulfilled: 0,
          totalDonations: 0,
          totalUnitsCollected: 0,
          totalEmergencyRequests: 0,
          totalEmergencyFulfilled: 0,
          activeDonors: 0,
          verifiedCenters: 0,
          hasData: false,
        );
  }
}

void main() {
  group('Community TimeSeries Model Tests', () {
    test('Correctly deserializes JSON with data points and totals', () {
      final json = {
        'days': 30,
        'startDate': '2026-08-29',
        'endDate': '2026-09-28',
        'dataPoints': [
          {
            'date': '2026-09-27',
            'bloodRequestsReceived': 5,
            'bloodRequestsFulfilled': 4,
            'donationsRecorded': 6,
            'unitsCollected': 6,
            'emergencyRequests': 2,
            'emergencyRequestsFulfilled': 2,
          }
        ],
        'totalRequestsReceived': 25,
        'totalRequestsFulfilled': 22,
        'totalDonations': 30,
        'totalUnitsCollected': 30,
        'totalEmergencyRequests': 8,
        'totalEmergencyFulfilled': 8,
        'activeDonors': 45,
        'verifiedCenters': 12,
        'hasData': true,
      };

      final model = CommunityTimeSeriesModel.fromJson(json);
      expect(model.days, equals(30));
      expect(model.startDate, equals('2026-08-29'));
      expect(model.hasData, isTrue);
      expect(model.totalRequestsReceived, equals(25));
      expect(model.totalRequestsFulfilled, equals(22));
      expect(model.totalDonations, equals(30));
      expect(model.totalUnitsCollected, equals(30));
      expect(model.totalEmergencyRequests, equals(8));
      expect(model.totalEmergencyFulfilled, equals(8));
      expect(model.dataPoints.length, equals(1));
      expect(model.dataPoints.first.bloodRequestsReceived, equals(5));
    });

    test('Correctly deserializes empty state model', () {
      final json = {
        'days': 7,
        'dataPoints': [],
        'totalRequestsReceived': 0,
        'totalRequestsFulfilled': 0,
        'totalDonations': 0,
        'totalUnitsCollected': 0,
        'totalEmergencyRequests': 0,
        'totalEmergencyFulfilled': 0,
        'activeDonors': 0,
        'verifiedCenters': 0,
        'hasData': false,
        'emptyStateMessage': 'No verified activity recorded in the last 7 days.',
      };

      final model = CommunityTimeSeriesModel.fromJson(json);
      expect(model.days, equals(7));
      expect(model.hasData, isFalse);
      expect(model.emptyStateMessage,
          contains('No verified activity recorded in the last 7 days.'));
    });
  });

  group('AboutScreen TimeSeries & Days Toggle Tests', () {
    testWidgets('renders Activity Trends section and switches days',
        (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final service = MockTimeSeriesMetricsService(
        impactModel: const CommunityImpactModel(
          totalVerifiedDonations: 10,
          totalUnitsCollected: 10,
          activeDonorsCount: 8,
          fulfilledRequestsCount: 9,
          hasData: true,
        ),
        timeSeriesByDays: {
          7: const CommunityTimeSeriesModel(
            days: 7,
            dataPoints: [],
            totalRequestsReceived: 2,
            totalRequestsFulfilled: 2,
            totalDonations: 3,
            totalUnitsCollected: 3,
            totalEmergencyRequests: 1,
            totalEmergencyFulfilled: 1,
            activeDonors: 3,
            verifiedCenters: 1,
            hasData: true,
          ),
          30: const CommunityTimeSeriesModel(
            days: 30,
            dataPoints: [],
            totalRequestsReceived: 10,
            totalRequestsFulfilled: 9,
            totalDonations: 10,
            totalUnitsCollected: 10,
            totalEmergencyRequests: 4,
            totalEmergencyFulfilled: 4,
            activeDonors: 8,
            verifiedCenters: 5,
            hasData: true,
          ),
          90: const CommunityTimeSeriesModel(
            days: 90,
            dataPoints: [],
            totalRequestsReceived: 0,
            totalRequestsFulfilled: 0,
            totalDonations: 0,
            totalUnitsCollected: 0,
            totalEmergencyRequests: 0,
            totalEmergencyFulfilled: 0,
            activeDonors: 0,
            verifiedCenters: 0,
            hasData: false,
            emptyStateMessage: 'No records found for 90 days.',
          ),
        },
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AboutScreen(
              isEmbedded: true,
              metricsApiService: service,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Verify Activity Trends header and 30D default
      expect(find.text('ACTIVITY TRENDS'), findsOneWidget);
      expect(find.text('30D'), findsOneWidget);
      expect(find.text('7D'), findsOneWidget);
      expect(find.text('90D'), findsOneWidget);

      // Verify 30D metrics are shown
      expect(find.text('9 fulfilled'), findsOneWidget); // total requests fulfilled subtitle
      expect(find.text('4 fulfilled'), findsOneWidget); // emergency requests subtitle

      // Tap 7D chip
      await tester.tap(find.text('7D'));
      await tester.pumpAndSettle();
      expect(find.text('3 units'), findsOneWidget);

      // Tap 90D chip (empty state)
      await tester.tap(find.text('90D'));
      await tester.pumpAndSettle();
      expect(
          find.text('No verified community activity in the last 90 days.'),
          findsOneWidget);
    });
  });

  group('EmergencyRequestCreatedScreen Navigation Tests', () {
    testWidgets('Pop button pops until first route', (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final dummyReq = BloodRequestDetail(
        id: 'req-123',
        requesterUserId: 'usr-1',
        bloodGroup: 'O+',
        unitsRequired: 2,
        urgency: BloodRequestUrgency.critical,
        status: BloodRequestStatus.open,
        hospitalName: 'Apollo Hospital',
        hospitalAddress: '58 Canal Circular Road',
        city: 'Kolkata',
        state: 'West Bengal',
        postalCode: '700054',
        latitude: 22.5726,
        longitude: 88.3639,
        requiredBy: DateTime.now().add(const Duration(hours: 4)),
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) =>
                          EmergencyRequestCreatedScreen(request: dummyReq),
                    ),
                  );
                },
                child: const Text('Open Created Screen'),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Open screen
      await tester.tap(find.text('Open Created Screen'));
      await tester.pumpAndSettle();

      expect(find.text('Emergency Request Submitted'), findsOneWidget);
      expect(find.byTooltip('Back'), findsOneWidget);

      // Tap AppBar Back button
      await tester.tap(find.byTooltip('Back'));
      await tester.pumpAndSettle();

      // Verify popped back to root
      expect(find.text('Open Created Screen'), findsOneWidget);
      expect(find.text('Emergency Request Submitted'), findsNothing);
    });
  });
}
