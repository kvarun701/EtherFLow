// express-server.js
// EtherFlow-compatible REST API server using Express.js (Node.js)
//
// Exposes the same endpoints as Python Flask/FastAPI servers so that
// EtherFlow Java/Kotlin clients can call them with PythonApiClient / HttpClient.
//
// Run with:
//   node express-server.js
//   # or with ts-node:
//   npx ts-node express-server.ts
//
// Endpoints:
//   GET  /api/node/health
//   GET  /api/node/hello?name=Alice
//   GET  /api/node/users/:id
//   POST /api/node/predict
//   POST /api/node/external-post

'use strict';

const express = require('express');
const app = express();
const PORT = 5005;

app.use(express.json());

// ─── Middleware ───────────────────────────────────────────────────────────────

app.use((req, _res, next) => {
  console.log(`[EtherFlow.Node] ${req.method} ${req.path}`);
  next();
});

// ─── Health ──────────────────────────────────────────────────────────────────

app.get('/api/node/health', (_req, res) => {
  res.json({
    status: 'UP',
    framework: 'Express.js (Node.js)',
    message: 'Node.js API server is running smoothly via EtherFlow',
    thirdPartyIntegration: 'Enabled (native fetch)',
  });
});

// ─── Hello ───────────────────────────────────────────────────────────────────

app.get('/api/node/hello', (req, res) => {
  const name = req.query.name || 'EtherFlow User';
  res.json({
    service: 'Node.js Express API',
    greeting: `Hello, ${name} from Node.js Express!`,
    framework: 'Express 4.x / Node.js 18+',
  });
});

// ─── User by ID ──────────────────────────────────────────────────────────────

app.get('/api/node/users/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: 'Invalid user ID' });

  res.json({
    id,
    name: `Node.js User ${id}`,
    role: 'Developer',
    active: true,
  });
});

// ─── ML Predict (same as Flask /predict) ─────────────────────────────────────

app.post('/api/node/predict', (req, res) => {
  const { inputs = [10, 20, 30] } = req.body || {};
  const total = inputs.reduce((a, b) => a + b, 0);
  const avg   = inputs.length > 0 ? total / inputs.length : 0;

  res.json({
    status: 'success',
    inputs,
    prediction: {
      sum:     total,
      average: avg,
      score:   Math.round(avg * 1.5 * 100) / 100,
    },
  });
});

// ─── External post (calls JSONPlaceholder via native fetch) ───────────────────

app.post('/api/node/external-post', async (req, res) => {
  const payload = req.body || { title: 'Default Node Post', body: 'Content' };
  try {
    const response = await fetch('https://jsonplaceholder.typicode.com/posts', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(payload),
    });
    const data = await response.json();
    res.status(201).json({
      status: 'success',
      source: 'Node.js Express → Third-Party API',
      remoteResponse: data,
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// ─── Error handlers ───────────────────────────────────────────────────────────

app.use((req, res) => res.status(404).json({ error: 'Resource Not Found', status: 404 }));

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal Server Error', status: 500 });
});

// ─── Start ────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`Node.js Express API server running on http://localhost:${PORT}`);
  console.log('Endpoints:');
  console.log(`  GET  http://localhost:${PORT}/api/node/health`);
  console.log(`  GET  http://localhost:${PORT}/api/node/hello?name=Alice`);
  console.log(`  GET  http://localhost:${PORT}/api/node/users/1`);
  console.log(`  POST http://localhost:${PORT}/api/node/predict`);
  console.log(`  POST http://localhost:${PORT}/api/node/external-post`);
});

module.exports = app;
