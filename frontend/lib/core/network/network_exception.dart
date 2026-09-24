/// Standard network exceptions for NETRA.
/// Clinical safety and security require deterministic failure handling.

abstract class NetworkException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic details;
  final String? correlationId;

  const NetworkException(
    this.message, {
    this.statusCode,
    this.details,
    this.correlationId,
  });

  @override
  String toString() {
    if (correlationId != null && correlationId!.isNotEmpty) {
      return '$message (Request ID: $correlationId)';
    }
    return message;
  }
}

/// Thrown when device has no network connection or the server is unreachable.
class ConnectionException extends NetworkException {
  const ConnectionException([
    String message =
        'Unable to reach NETRA server. Please verify your internet connection.',
    String? correlationId,
  ]) : super(message, correlationId: correlationId);
}

/// Thrown on HTTP 400 Bad Request or malformed inputs.
class ValidationException extends NetworkException {
  const ValidationException(
    String message, {
    int? statusCode,
    dynamic details,
    String? correlationId,
  }) : super(
          message,
          statusCode: statusCode ?? 400,
          details: details,
          correlationId: correlationId,
        );
}

/// Thrown on HTTP 401 Unauthorized or 403 Forbidden.
class UnauthorizedException extends NetworkException {
  const UnauthorizedException([
    String message = 'Session unauthorized or capability token is invalid.',
    String? correlationId,
  ]) : super(message, statusCode: 403, correlationId: correlationId);
}

/// Thrown on HTTP 404 Not Found.
class NotFoundException extends NetworkException {
  const NotFoundException([
    String message = 'Requested resource not found.',
    String? correlationId,
  ]) : super(message, statusCode: 404, correlationId: correlationId);
}

/// Thrown on HTTP 409 Conflict (e.g. duplicate registration or concurrent modification).
class ConflictException extends NetworkException {
  const ConflictException([
    String message = 'Resource conflict or duplicate operation detected.',
    String? correlationId,
  ]) : super(message, statusCode: 409, correlationId: correlationId);
}

/// Thrown on HTTP 410 Gone (e.g. expired session).
class SessionExpiredException extends NetworkException {
  const SessionExpiredException([
    String message =
        'Screening session has expired. Please start a fresh assessment.',
    String? correlationId,
  ]) : super(message, statusCode: 410, correlationId: correlationId);
}

/// Thrown on HTTP 429 Too Many Requests.
class RateLimitException extends NetworkException {
  const RateLimitException([
    String message = 'Too many requests. Please pause before trying again.',
    String? correlationId,
  ]) : super(message, statusCode: 429, correlationId: correlationId);
}

/// Thrown on HTTP 500+ Internal Server Error.
class ServerException extends NetworkException {
  const ServerException([
    String message = 'Server error occurred. Please try again later.',
    String? correlationId,
  ]) : super(message, statusCode: 500, correlationId: correlationId);
}
