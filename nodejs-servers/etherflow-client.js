/**
 * etherflow-client.js
 * EtherFlow HTTP Client for Node.js — JavaScript (CommonJS + ESM compatible)
 *
 * A fluent, Promise-based HTTP client for Node.js that mirrors the EtherFlow
 * Java/Kotlin builder API. Uses the native `fetch` API (Node 18+).
 * Zero external dependencies.
 *
 * Usage:
 *   const { EtherFlowClient } = require('./etherflow-client');
 *   const client = EtherFlowClient.builder().baseUrl('https://api.example.com').retry(3).build();
 *   const user = await client.getJson('/users/1');
 */

'use strict';

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

class EtherFlowError extends Error {
  /**
   * @param {string} message
   * @param {number} [statusCode]
   * @param {string} [responseBody]
   */
  constructor(message, statusCode, responseBody) {
    super(message);
    this.name         = 'EtherFlowError';
    this.statusCode   = statusCode;
    this.responseBody = responseBody;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

class EtherFlowClient {
  /**
   * @param {{
   *   baseUrl?: string,
   *   timeoutMs?: number,
   *   maxRetries?: number,
   *   retryDelayMs?: number,
   *   defaultHeaders?: Record<string,string>
   * }} config
   */
  constructor(config = {}) {
    this._baseUrl        = (config.baseUrl ?? '').replace(/\/$/, '');
    this._timeoutMs      = config.timeoutMs      ?? 10_000;
    this._maxRetries     = config.maxRetries     ?? 3;
    this._retryDelayMs   = config.retryDelayMs   ?? 200;
    this._defaultHeaders = {
      'Accept':       'application/json',
      'Content-Type': 'application/json',
      'User-Agent':   'EtherFlow-Node-Client/1.0',
      ...config.defaultHeaders,
    };
  }

  // ── Builder ────────────────────────────────────────────────────────────────

  static builder() {
    return new EtherFlowClientBuilder();
  }

  static create(baseUrl) {
    return new EtherFlowClient({ baseUrl });
  }

  // ── Public API (shorthand — mirrors Java getJson/postJson) ─────────────────

  /** @returns {Promise<any>} */
  async getJson(path, headers = {}) {
    return this._execute('GET', path, undefined, headers);
  }

  /** @returns {Promise<any>} */
  async postJson(path, body, headers = {}) {
    return this._execute('POST', path, body, headers);
  }

  /** @returns {Promise<any>} */
  async putJson(path, body, headers = {}) {
    return this._execute('PUT', path, body, headers);
  }

  /** @returns {Promise<any>} */
  async patchJson(path, body, headers = {}) {
    return this._execute('PATCH', path, body, headers);
  }

  /** @returns {Promise<any>} */
  async deleteJson(path, headers = {}) {
    return this._execute('DELETE', path, undefined, headers);
  }

  /** Safe GET — returns { ok: true, data } or { ok: false, error }. */
  async getResult(path, headers = {}) {
    try {
      return { ok: true, data: await this.getJson(path, headers) };
    } catch (e) {
      return { ok: false, error: e };
    }
  }

  /** Health check — calls /health on the base URL. */
  async checkHealth() {
    try {
      return await this.getJson('/health');
    } catch {
      return { status: 'DOWN', error: 'Health check failed' };
    }
  }

  // ── Internal ───────────────────────────────────────────────────────────────

  async _execute(method, path, body, extraHeaders = {}) {
    const url     = path.startsWith('http') ? path : `${this._baseUrl}${path}`;
    const headers = { ...this._defaultHeaders, ...extraHeaders };

    let lastError = new EtherFlowError('Unknown error');
    let delay     = this._retryDelayMs;

    for (let attempt = 0; attempt <= this._maxRetries; attempt++) {
      try {
        const controller = new AbortController();
        const timer      = setTimeout(() => controller.abort(), this._timeoutMs);

        /** @type {RequestInit} */
        const init = { method, headers, signal: controller.signal };
        if (body !== undefined && method !== 'GET' && method !== 'DELETE') {
          init.body = JSON.stringify(body);
        }

        const response = await fetch(url, init);
        clearTimeout(timer);

        const text = await response.text();
        if (!response.ok) {
          const err = new EtherFlowError(
            `[EtherFlow] HTTP ${response.status}: ${response.statusText}`,
            response.status,
            text
          );
          if (response.status >= 400 && response.status < 500) throw err;
          throw err;
        }

        try { return JSON.parse(text); } catch { return text; }

      } catch (err) {
        lastError = err instanceof EtherFlowError ? err : new EtherFlowError(String(err));
        if (err instanceof EtherFlowError && err.statusCode >= 400 && err.statusCode < 500) throw err;

        if (attempt < this._maxRetries) {
          console.warn(`[EtherFlow.JS] Retry ${attempt + 1}/${this._maxRetries} after ${delay}ms`);
          await new Promise(r => setTimeout(r, delay));
          delay *= 2;
        }
      }
    }
    throw new EtherFlowError(`[EtherFlow] Max retries exceeded: ${lastError.message}`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

class EtherFlowClientBuilder {
  constructor() { this._config = {}; }

  baseUrl(url)       { this._config.baseUrl      = url;  return this; }
  timeout(ms)        { this._config.timeoutMs    = ms;   return this; }
  retry(n)           { this._config.maxRetries   = n;    return this; }
  retryDelay(ms)     { this._config.retryDelayMs = ms;   return this; }
  header(key, value) {
    this._config.defaultHeaders = { ...this._config.defaultHeaders, [key]: value };
    return this;
  }
  build() { return new EtherFlowClient(this._config); }
}

// ─────────────────────────────────────────────────────────────────────────────
// Exports
// ─────────────────────────────────────────────────────────────────────────────

module.exports = { EtherFlowClient, EtherFlowClientBuilder, EtherFlowError };

// ─────────────────────────────────────────────────────────────────────────────
// Runnable demo (node etherflow-client.js)
// ─────────────────────────────────────────────────────────────────────────────

if (require.main === module) {
  (async () => {
    const client = EtherFlowClient.builder()
        .baseUrl('https://jsonplaceholder.typicode.com')
        .retry(3)
        .build();

    const user = await client.getJson('/users/1');
    console.log(`User: ${user.name} — ${user.email}`);

    const users = await client.getJson('/users');
    console.log(`Total users: ${users.length}`);

    const created = await client.postJson('/posts', {
      title: 'EtherFlow Node.js', body: 'Hello!', userId: 1
    });
    console.log(`Created post ID: ${created.id}`);

    const result = await client.getResult('/users/999');
    if (result.ok) console.log('Found:', result.data.name);
    else console.error('Error:', result.error.message);
  })().catch(console.error);
}
