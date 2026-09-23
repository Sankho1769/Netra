import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'network_exception.dart';

/// Production-ready, secure HTTP client wrapper for NETRA.
/// Enforces timeouts, centralized error translation, and token management.
class ApiClient {
  final String baseUrl;
  final http.Client _client;
  final Duration timeout;

  ApiClient({
    required this.baseUrl,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
  }) : _client = client ?? http.Client();

  /// Perform a GET request.
  Future<dynamic> get(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    try {
      final response = await _client
          .get(uri, headers: _buildHeaders(headers))
          .timeout(timeout);
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  /// Perform a POST request.
  Future<dynamic> post(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    try {
      final response = await _client
          .post(
            uri,
            headers: _buildHeaders(headers),
            body: body != null ? jsonEncode(body) : null,
          )
          .timeout(timeout);
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  /// Perform a PUT request.
  Future<dynamic> put(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    try {
      final response = await _client
          .put(
            uri,
            headers: _buildHeaders(headers),
            body: body != null ? jsonEncode(body) : null,
          )
          .timeout(timeout);
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  /// Perform a PATCH request.
  Future<dynamic> patch(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    try {
      final response = await _client
          .patch(
            uri,
            headers: _buildHeaders(headers),
            body: body != null ? jsonEncode(body) : null,
          )
          .timeout(timeout);
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  /// Perform a DELETE request.
  Future<dynamic> delete(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    try {
      final response = await _client
          .delete(uri, headers: _buildHeaders(headers))
          .timeout(timeout);
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  Uri _buildUri(String path, Map<String, dynamic>? queryParameters) {
    final cleanBase = baseUrl.endsWith('/')
        ? baseUrl.substring(0, baseUrl.length - 1)
        : baseUrl;
    final cleanPath = path.startsWith('/') ? path : '/$path';
    final urlString = '$cleanBase$cleanPath';
    final uri = Uri.parse(urlString);

    if (queryParameters != null && queryParameters.isNotEmpty) {
      final mappedParams =
          queryParameters.map((k, v) => MapEntry(k, v.toString()));
      return uri.replace(queryParameters: mappedParams);
    }
    return uri;
  }

  Map<String, String> _buildHeaders(Map<String, String>? customHeaders) {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (customHeaders != null) {
      headers.addAll(customHeaders);
    }
    return headers;
  }

  dynamic _processResponse(http.Response response) {
    final statusCode = response.statusCode;

    dynamic responseBody;
    if (response.body.isNotEmpty) {
      try {
        responseBody = jsonDecode(response.body);
      } catch (_) {
        responseBody = response.body;
      }
    }

    if (statusCode >= 200 && statusCode < 300) {
      return responseBody;
    }

    String message = 'Request failed with status $statusCode';
    if (responseBody is Map && responseBody.containsKey('message')) {
      message = responseBody['message'].toString();
    }

    switch (statusCode) {
      case 400:
        throw ValidationException(message,
            statusCode: statusCode, details: responseBody);
      case 401:
      case 403:
        throw UnauthorizedException(message);
      case 404:
        throw NotFoundException(message);
      case 409:
        throw ConflictException(message);
      case 410:
        throw SessionExpiredException(message);
      case 429:
        throw RateLimitException(message);
      default:
        if (statusCode >= 500) {
          throw ServerException(message);
        }
        throw ValidationException(message,
            statusCode: statusCode, details: responseBody);
    }
  }

  void close() {
    _client.close();
  }
}
