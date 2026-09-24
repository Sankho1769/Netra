import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:netra_app/core/network/api_client.dart';
import 'package:netra_app/core/network/network_exception.dart';

class MockHttpClient extends http.BaseClient {
  final Future<http.Response> Function(http.BaseRequest request) handler;
  MockHttpClient(this.handler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final response = await handler(request);
    return http.StreamedResponse(
      Stream.value(response.bodyBytes),
      response.statusCode,
      headers: response.headers,
      request: request,
    );
  }
}

void main() {
  group('Observability & Correlation ID Tests in Flutter', () {
    test('NetworkException includes correlation ID in toString() when present',
        () {
      const exWithId = ValidationException(
        'Invalid blood request format',
        correlationId: 'req-corr-12345',
      );
      expect(
        exWithId.toString(),
        equals('Invalid blood request format (Request ID: req-corr-12345)'),
      );
      expect(exWithId.correlationId, equals('req-corr-12345'));

      const exWithoutId = ValidationException('Invalid blood request format');
      expect(exWithoutId.toString(), equals('Invalid blood request format'));
      expect(exWithoutId.correlationId, isNull);
    });

    test('ApiClient extracts correlation ID from response header (lowercase)',
        () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
          jsonEncode({'message': 'Rate limit exceeded'}),
          429,
          headers: {'x-correlation-id': 'trace-abc-789'},
        );
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
      );

      expect(
        () => apiClient.get('/test'),
        throwsA(
          predicate((e) =>
              e is RateLimitException &&
              e.correlationId == 'trace-abc-789' &&
              e.toString() ==
                  'Rate limit exceeded (Request ID: trace-abc-789)'),
        ),
      );
    });

    test('ApiClient extracts correlation ID from response header (capitalized)',
        () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
          jsonEncode({'message': 'Blood request not found'}),
          404,
          headers: {'X-Correlation-ID': 'trace-cap-456'},
        );
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
      );

      expect(
        () => apiClient.get('/blood-requests/unknown-id'),
        throwsA(
          predicate((e) =>
              e is NotFoundException &&
              e.correlationId == 'trace-cap-456' &&
              e.toString() ==
                  'Blood request not found (Request ID: trace-cap-456)'),
        ),
      );
    });

    test(
        'ApiClient extracts correlation ID from response body JSON if header missing',
        () async {
      final mockClient = MockHttpClient((req) async {
        return http.Response(
          jsonEncode(
              {'message': 'Internal error', 'correlationId': 'json-corr-999'}),
          500,
          headers: {},
        );
      });

      final apiClient = ApiClient(
        baseUrl: 'http://api.netra.local',
        client: mockClient,
      );

      expect(
        () => apiClient.post('/blood-requests', body: {}),
        throwsA(
          predicate((e) =>
              e is ServerException &&
              e.correlationId == 'json-corr-999' &&
              e.toString() == 'Internal error (Request ID: json-corr-999)'),
        ),
      );
    });

    test('All network exception types propagate correlation ID properly', () {
      const corr = 'test-corr-id';

      expect(
          const ConnectionException('msg', corr).correlationId, equals(corr));
      expect(
          const ValidationException('msg', correlationId: corr).correlationId,
          equals(corr));
      expect(
          const UnauthorizedException('msg', corr).correlationId, equals(corr));
      expect(const NotFoundException('msg', corr).correlationId, equals(corr));
      expect(const ConflictException('msg', corr).correlationId, equals(corr));
      expect(const SessionExpiredException('msg', corr).correlationId,
          equals(corr));
      expect(const RateLimitException('msg', corr).correlationId, equals(corr));
      expect(const ServerException('msg', corr).correlationId, equals(corr));
    });
  });
}
