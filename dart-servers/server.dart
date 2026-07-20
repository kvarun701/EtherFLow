/// server.dart — EtherFlow-compatible REST API Server for Dart
/// Uses the `shelf` package to expose the same endpoints as Flask/FastAPI
/// so EtherFlow Java/Kotlin clients can call them seamlessly.
///
/// Run: dart run server.dart
/// Port: 5009

import 'dart:convert';
import 'dart:io';
import 'package:shelf/shelf.dart';
import 'package:shelf/shelf_io.dart';
import 'package:shelf_router/shelf_router.dart';
import 'etherflow_client.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

Response jsonResponse(Map<String, dynamic> body, {int status = 200}) =>
    Response(
      status,
      body: jsonEncode(body),
      headers: {'Content-Type': 'application/json'},
    );

// ─────────────────────────────────────────────────────────────────────────────
// Handlers
// ─────────────────────────────────────────────────────────────────────────────

Response health(Request req) => jsonResponse({
      'status': 'UP',
      'framework': 'Shelf (Dart)',
      'message': 'Dart API server is running smoothly via EtherFlow',
    });

Response hello(Request req) {
  final name = req.url.queryParameters['name'] ?? 'EtherFlow User';
  return jsonResponse({
    'service': 'Dart Shelf API',
    'greeting': 'Hello, $name from Dart!',
    'framework': 'Shelf / Dart 3.x',
  });
}

Response getUser(Request req, String id) {
  final userId = int.tryParse(id);
  if (userId == null || userId <= 0) {
    return jsonResponse({'error': 'Invalid user ID'}, status: 400);
  }
  return jsonResponse({
    'id': userId,
    'name': 'Dart User $userId',
    'role': 'Developer',
    'active': true,
  });
}

Future<Response> predict(Request req) async {
  final body = jsonDecode(await req.readAsString()) as Map<String, dynamic>? ?? {};
  final inputs = (body['inputs'] as List?)?.map((e) => (e as num).toDouble()).toList()
      ?? [10.0, 20.0, 30.0];

  final total = inputs.fold<double>(0, (a, b) => a + b);
  final avg   = inputs.isEmpty ? 0.0 : total / inputs.length;

  return jsonResponse({
    'status': 'success',
    'inputs': inputs,
    'prediction': {
      'sum':     total,
      'average': avg,
      'score':   (avg * 1.5 * 100).round() / 100,
    },
  });
}

Future<Response> externalPost(Request req) async {
  final payload = jsonDecode(await req.readAsString()) as Map<String, dynamic>? ?? {};
  final client = EtherFlowClient.create('https://jsonplaceholder.typicode.com');

  try {
    final created = await client.getRaw('/posts');
    client.close();
    return jsonResponse({
      'status': 'success',
      'source': 'Dart Shelf → Third-Party API',
      'remoteResponse': created,
    }, status: 201);
  } catch (e) {
    client.close();
    return jsonResponse({'status': 'error', 'message': e.toString()}, status: 502);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Router
// ─────────────────────────────────────────────────────────────────────────────

Router buildRouter() {
  final router = Router();
  router.get('/api/dart/health',         health);
  router.get('/api/dart/hello',          hello);
  router.get('/api/dart/users/<id>',     getUser);
  router.post('/api/dart/predict',       predict);
  router.post('/api/dart/external-post', externalPost);
  return router;
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

Future<void> main() async {
  final handler = Pipeline()
      .addMiddleware(logRequests())
      .addHandler(buildRouter());

  final server = await serve(handler, InternetAddress.anyIPv4, 5009);
  print('Dart Shelf API server running on http://localhost:${server.port}');
  print('  GET  http://localhost:${server.port}/api/dart/health');
  print('  GET  http://localhost:${server.port}/api/dart/hello?name=Alice');
  print('  GET  http://localhost:${server.port}/api/dart/users/1');
  print('  POST http://localhost:${server.port}/api/dart/predict');
  print('  POST http://localhost:${server.port}/api/dart/external-post');
}
