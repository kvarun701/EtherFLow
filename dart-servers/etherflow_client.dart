/// etherflow_client.dart
/// EtherFlow HTTP Client for Dart — standalone (not Flutter)
///
/// A strongly-typed, async/await HTTP client that mirrors the EtherFlow
/// Java/Kotlin builder API. Uses the `http` package for transport.
///
/// Usage:
///   final client = EtherFlowClient.builder()
///     .baseUrl('https://api.example.com')
///     .retry(3)
///     .build();
///
///   final user = await client.get<User>('/users/1', decoder: User.fromJson);
///   final created = await client.post<User>('/users', body: {'name': 'Alice'}, decoder: User.fromJson);

library etherflow_client;

import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

/// Represents an error from the EtherFlow Dart client.
class EtherFlowException implements Exception {
  final String message;
  final int? statusCode;
  final String? responseBody;

  const EtherFlowException(this.message, {this.statusCode, this.responseBody});

  @override
  String toString() => '[EtherFlow] $message${statusCode != null ? " (HTTP $statusCode)" : ""}';
}

// ─────────────────────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────────────────────

/// Configuration for the EtherFlow Dart client.
class EtherFlowConfig {
  final String baseUrl;
  final Duration timeout;
  final int maxRetries;
  final Duration retryDelay;
  final Map<String, String> defaultHeaders;

  const EtherFlowConfig({
    this.baseUrl = '',
    this.timeout = const Duration(seconds: 10),
    this.maxRetries = 3,
    this.retryDelay = const Duration(milliseconds: 200),
    this.defaultHeaders = const {},
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Safe Result
// ─────────────────────────────────────────────────────────────────────────────

/// A discriminated union result that never throws — mirrors EtherFlow Java's `toResult()`.
sealed class EtherFlowResult<T> {}

final class EtherFlowSuccess<T> extends EtherFlowResult<T> {
  final T data;
  const EtherFlowSuccess(this.data);
}

final class EtherFlowFailure<T> extends EtherFlowResult<T> {
  final EtherFlowException error;
  const EtherFlowFailure(this.error);
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

/// EtherFlow HTTP client for Dart.
/// Mirrors the Java/Kotlin EtherFlowClient builder API with native Dart idioms.
class EtherFlowClient {
  final EtherFlowConfig _config;
  final http.Client _httpClient;

  EtherFlowClient._(this._config, this._httpClient);

  factory EtherFlowClient(EtherFlowConfig config) =>
      EtherFlowClient._(config, http.Client());

  // ── Factory / Builder ────────────────────────────────────────────────────

  static EtherFlowClientBuilder builder() => EtherFlowClientBuilder._();

  static EtherFlowClient create(String baseUrl) =>
      EtherFlowClient(EtherFlowConfig(baseUrl: baseUrl));

  void close() => _httpClient.close();

  // ── Public API ───────────────────────────────────────────────────────────

  /// GET request — decoder maps the JSON Map to your model class.
  Future<T> get<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('GET', path, headers: headers);
    return decoder(json as Map<String, dynamic>);
  }

  /// GET a list of objects.
  Future<List<T>> getList<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('GET', path, headers: headers);
    return (json as List).map((e) => decoder(e as Map<String, dynamic>)).toList();
  }

  /// POST with JSON body.
  Future<T> post<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, dynamic> body = const {},
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('POST', path, body: body, headers: headers);
    return decoder(json as Map<String, dynamic>);
  }

  /// PUT with JSON body.
  Future<T> put<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, dynamic> body = const {},
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('PUT', path, body: body, headers: headers);
    return decoder(json as Map<String, dynamic>);
  }

  /// PATCH with JSON body.
  Future<T> patch<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, dynamic> body = const {},
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('PATCH', path, body: body, headers: headers);
    return decoder(json as Map<String, dynamic>);
  }

  /// DELETE request.
  Future<T> delete<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
    Map<String, String> headers = const {},
  }) async {
    final json = await _executeJson('DELETE', path, headers: headers);
    return decoder(json as Map<String, dynamic>);
  }

  /// Raw GET — returns decoded JSON as dynamic.
  Future<dynamic> getRaw(String path, {Map<String, String> headers = const {}}) =>
      _executeJson('GET', path, headers: headers);

  /// Safe GET — never throws, returns [EtherFlowResult].
  Future<EtherFlowResult<T>> getResult<T>(
    String path, {
    required T Function(Map<String, dynamic>) decoder,
  }) async {
    try {
      final value = await get<T>(path, decoder: decoder);
      return EtherFlowSuccess(value);
    } on EtherFlowException catch (e) {
      return EtherFlowFailure(e);
    } catch (e) {
      return EtherFlowFailure(EtherFlowException(e.toString()));
    }
  }

  /// Health check — calls `/health` on the base URL.
  Future<Map<String, dynamic>> checkHealth() async {
    try {
      return await getRaw('/health') as Map<String, dynamic>;
    } catch (_) {
      return {'status': 'DOWN', 'error': 'Health check failed'};
    }
  }

  // ── Internal ─────────────────────────────────────────────────────────────

  Uri _buildUri(String path) {
    if (path.startsWith('http')) return Uri.parse(path);
    final base = _config.baseUrl.endsWith('/')
        ? _config.baseUrl.substring(0, _config.baseUrl.length - 1)
        : _config.baseUrl;
    return Uri.parse('$base$path');
  }

  Map<String, String> _buildHeaders(Map<String, String> extra) => {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'User-Agent': 'EtherFlow-Dart-Client/1.0',
        ..._config.defaultHeaders,
        ...extra,
      };

  Future<dynamic> _executeJson(
    String method,
    String path, {
    Map<String, dynamic>? body,
    Map<String, String> headers = const {},
  }) async {
    final uri = _buildUri(path);
    final allHeaders = _buildHeaders(headers);
    final bodyBytes = body != null ? utf8.encode(jsonEncode(body)) : null;

    Exception lastError = EtherFlowException('Unknown error');
    var delay = _config.retryDelay;

    for (int attempt = 0; attempt <= _config.maxRetries; attempt++) {
      try {
        final request = http.Request(method, uri);
        request.headers.addAll(allHeaders);
        if (bodyBytes != null) request.bodyBytes = bodyBytes;

        final streamedResponse = await _httpClient
            .send(request)
            .timeout(_config.timeout);
        final response = await http.Response.fromStream(streamedResponse);

        if (response.statusCode >= 200 && response.statusCode < 300) {
          return jsonDecode(response.body);
        }

        final err = EtherFlowException(
          'HTTP ${response.statusCode}: ${response.reasonPhrase}',
          statusCode: response.statusCode,
          responseBody: response.body,
        );

        // Don't retry 4xx
        if (response.statusCode >= 400 && response.statusCode < 500) throw err;
        lastError = err;

      } catch (e) {
        lastError = e is Exception ? e : EtherFlowException(e.toString());
        if (e is EtherFlowException && e.statusCode != null &&
            e.statusCode! >= 400 && e.statusCode! < 500) rethrow;
      }

      if (attempt < _config.maxRetries) {
        print('[EtherFlow.Dart] Retry ${attempt + 1}/${_config.maxRetries} after ${delay.inMilliseconds}ms');
        await Future.delayed(delay);
        delay = delay * 2;
      }
    }
    throw EtherFlowException('[EtherFlow] Max retries exceeded. Last: $lastError');
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

class EtherFlowClientBuilder {
  String _baseUrl = '';
  Duration _timeout = const Duration(seconds: 10);
  int _maxRetries = 3;
  Duration _retryDelay = const Duration(milliseconds: 200);
  final Map<String, String> _headers = {};

  EtherFlowClientBuilder._();

  EtherFlowClientBuilder baseUrl(String url) {
    _baseUrl = url;
    return this;
  }

  EtherFlowClientBuilder timeout(Duration duration) {
    _timeout = duration;
    return this;
  }

  EtherFlowClientBuilder retry(int count) {
    _maxRetries = count;
    return this;
  }

  EtherFlowClientBuilder retryDelay(Duration delay) {
    _retryDelay = delay;
    return this;
  }

  EtherFlowClientBuilder header(String key, String value) {
    _headers[key] = value;
    return this;
  }

  EtherFlowClient build() => EtherFlowClient(EtherFlowConfig(
        baseUrl: _baseUrl,
        timeout: _timeout,
        maxRetries: _maxRetries,
        retryDelay: _retryDelay,
        defaultHeaders: Map.unmodifiable(_headers),
      ));
}

// ─────────────────────────────────────────────────────────────────────────────
// Example Usage
// ─────────────────────────────────────────────────────────────────────────────

/*
 * Run with: dart run etherflow_client.dart

class User {
  final int id;
  final String name;
  final String email;

  const User({required this.id, required this.name, required this.email});

  factory User.fromJson(Map<String, dynamic> json) => User(
        id: json['id'] as int,
        name: json['name'] as String,
        email: json['email'] as String,
      );

  @override
  String toString() => 'User($id, $name, $email)';
}

Future<void> main() async {
  // 1. Create client using builder
  final client = EtherFlowClient.builder()
      .baseUrl('https://jsonplaceholder.typicode.com')
      .retry(3)
      .timeout(const Duration(seconds: 10))
      .build();

  try {
    // 2. GET — auto-decode to User
    final user = await client.get('/users/1', decoder: User.fromJson);
    print('User: ${user.name} — ${user.email}');

    // 3. GET list
    final users = await client.getList('/users', decoder: User.fromJson);
    print('Total users: ${users.length}');

    // 4. POST with body
    final raw = await client.getRaw('/posts');
    print('Posts count: ${(raw as List).length}');

    // 5. Safe result — never throws
    final result = await client.getResult('/users/999', decoder: User.fromJson);
    switch (result) {
      case EtherFlowSuccess(:final data):
        print('Found: ${data.name}');
      case EtherFlowFailure(:final error):
        print('Error: $error');
    }

    // 6. Health check
    final health = await client.checkHealth();
    print('Status: ${health['status']}');

  } finally {
    client.close();
  }
}
*/
