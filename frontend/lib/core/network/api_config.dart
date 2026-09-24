import 'package:flutter/foundation.dart';

/// Centralized API and environment configuration for the NETRA Flutter client.
/// Supports build-time configuration via --dart-define=NETRA_API_URL=...
class ApiConfig {
  static const String _defaultDevUrl = 'http://localhost:8080/api/v1';

  /// Returns the configured base API URL.
  /// In release builds, throws a StateError if NETRA_API_URL is unconfigured or points to insecure localhost.
  static String get baseUrl {
    const envUrl = String.fromEnvironment('NETRA_API_URL');
    if (envUrl.isNotEmpty) {
      final sanitized = envUrl.endsWith('/')
          ? envUrl.substring(0, envUrl.length - 1)
          : envUrl;
      if (kReleaseMode &&
          (sanitized.contains('localhost') ||
              sanitized.contains('127.0.0.1'))) {
        throw StateError(
          'FATAL: Insecure localhost URL is prohibited in production release builds. '
          'Configure a secure HTTPS endpoint via --dart-define=NETRA_API_URL=https://api.netra.org/api/v1',
        );
      }
      return sanitized;
    }

    if (kReleaseMode) {
      throw StateError(
        'FATAL: NETRA_API_URL environment variable is required in release mode. '
        'Compile with --dart-define=NETRA_API_URL=https://api.netra.org/api/v1',
      );
    }

    return _defaultDevUrl;
  }

  /// Builds a full API endpoint URL from a subpath.
  static String endpoint(String path) {
    final cleanPath = path.startsWith('/') ? path : '/$path';
    return '$baseUrl$cleanPath';
  }
}
