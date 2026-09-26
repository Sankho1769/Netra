import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'network_exception.dart';

/// Production-ready, secure HTTP client wrapper for NETRA.
/// Enforces timeouts, centralized error translation, token refresh, and retry management.
class ApiClient {
  static Future<String?> Function()? defaultTokenProvider;
  static Future<bool> Function()? defaultTokenRefreshHandler;

  final String baseUrl;
  final http.Client _client;
  final Duration timeout;
  final Future<bool> Function()? onTokenRefresh;
  final Future<String?> Function()? tokenProvider;

  ApiClient({
    required this.baseUrl,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
    Future<bool> Function()? onTokenRefresh,
    Future<String?> Function()? tokenProvider,
  })  : _client = client ?? http.Client(),
        onTokenRefresh = onTokenRefresh ?? defaultTokenRefreshHandler,
        tokenProvider = tokenProvider ?? defaultTokenProvider;

  static void configureAuth({
    required Future<String?> Function() tokenProvider,
    required Future<bool> Function() onTokenRefresh,
  }) {
    defaultTokenProvider = tokenProvider;
    defaultTokenRefreshHandler = onTokenRefresh;
  }

  static void resetAuthConfiguration() {
    defaultTokenProvider = null;
    defaultTokenRefreshHandler = null;
  }

  bool _isPublicAuthPath(String path) {
    final clean = path.toLowerCase();
    return clean.endsWith('/login') ||
        clean.endsWith('/register') ||
        clean.endsWith('/refresh');
  }

  /// Execute HTTP request with optional token refresh retry on 401.
  Future<dynamic> _executeWithRetry(
    Future<http.Response> Function(Map<String, String> headers) requestFn,
    Map<String, String>? initialHeaders, {
    bool requiresAuth = true,
  }) async {
    final headers = _buildHeaders(initialHeaders);
    if (requiresAuth && !headers.containsKey('Authorization') && tokenProvider != null) {
      final token = await tokenProvider!();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }
    }
    try {
      final response = await requestFn(headers).timeout(timeout);
      if (requiresAuth && response.statusCode == 401 && onTokenRefresh != null) {
        final refreshed = await onTokenRefresh!();
        if (refreshed) {
          final retryHeaders = Map<String, String>.from(headers);
          if (tokenProvider != null) {
            final newToken = await tokenProvider!();
            if (newToken != null && newToken.isNotEmpty) {
              retryHeaders['Authorization'] = 'Bearer $newToken';
            }
          }
          final retryResponse = await requestFn(retryHeaders).timeout(timeout);
          return _processResponse(retryResponse);
        }
      }
      return _processResponse(response);
    } on SocketException {
      throw const ConnectionException();
    } on TimeoutException {
      throw const ConnectionException(
          'Network request timed out. Please check your connection.');
    } on http.ClientException catch (e) {
      throw ConnectionException(e.message);
    }
  }

  /// Perform a GET request.
  Future<dynamic> get(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
  }) {
    final uri = _buildUri(path, queryParameters);
    final effectiveRequiresAuth = requiresAuth && !_isPublicAuthPath(path);
    return _executeWithRetry(
      (h) => _client.get(uri, headers: h),
      headers,
      requiresAuth: effectiveRequiresAuth,
    );
  }

  /// Perform a POST request.
  Future<dynamic> post(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
  }) {
    final uri = _buildUri(path, queryParameters);
    final encoded = body != null ? jsonEncode(body) : null;
    final effectiveRequiresAuth = requiresAuth && !_isPublicAuthPath(path);
    return _executeWithRetry(
      (h) => _client.post(uri, headers: h, body: encoded),
      headers,
      requiresAuth: effectiveRequiresAuth,
    );
  }

  /// Perform a PUT request.
  Future<dynamic> put(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
  }) {
    final uri = _buildUri(path, queryParameters);
    final encoded = body != null ? jsonEncode(body) : null;
    final effectiveRequiresAuth = requiresAuth && !_isPublicAuthPath(path);
    return _executeWithRetry(
      (h) => _client.put(uri, headers: h, body: encoded),
      headers,
      requiresAuth: effectiveRequiresAuth,
    );
  }

  /// Perform a PATCH request.
  Future<dynamic> patch(
    String path, {
    Map<String, String>? headers,
    Object? body,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
  }) {
    final uri = _buildUri(path, queryParameters);
    final encoded = body != null ? jsonEncode(body) : null;
    final effectiveRequiresAuth = requiresAuth && !_isPublicAuthPath(path);
    return _executeWithRetry(
      (h) => _client.patch(uri, headers: h, body: encoded),
      headers,
      requiresAuth: effectiveRequiresAuth,
    );
  }

  /// Perform a DELETE request.
  Future<dynamic> delete(
    String path, {
    Map<String, String>? headers,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
  }) {
    final uri = _buildUri(path, queryParameters);
    final effectiveRequiresAuth = requiresAuth && !_isPublicAuthPath(path);
    return _executeWithRetry(
      (h) => _client.delete(uri, headers: h),
      headers,
      requiresAuth: effectiveRequiresAuth,
    );
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

    final correlationId = response.headers['x-correlation-id'] ??
        response.headers['X-Correlation-ID'] ??
        (responseBody is Map
            ? responseBody['correlationId']?.toString()
            : null);

    switch (statusCode) {
      case 400:
        throw ValidationException(
          message,
          statusCode: statusCode,
          details: responseBody,
          correlationId: correlationId,
        );
      case 401:
        throw UnauthorizedException(message, correlationId);
      case 403:
        throw ForbiddenException(message, correlationId);
      case 404:
        throw NotFoundException(message, correlationId);
      case 409:
        throw ConflictException(message, correlationId);
      case 410:
        throw SessionExpiredException(message, correlationId);
      case 429:
        throw RateLimitException(message, correlationId);
      default:
        if (statusCode >= 500) {
          throw ServerException(message, correlationId);
        }
        throw ValidationException(
          message,
          statusCode: statusCode,
          details: responseBody,
          correlationId: correlationId,
        );
    }
  }

  void close() {
    _client.close();
  }
}
