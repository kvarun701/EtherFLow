/**
 * etherflow-client.ts
 * EtherFlow Reactive HTTP Client for Node.js & Browser — TypeScript
 *
 * A strongly-typed, Promise-based HTTP client that mirrors the EtherFlow
 * Java/Kotlin builder API. Uses the native `fetch` API (Node 18+ / browser).
 * Zero external runtime dependencies.
 *
 * Usage:
 *   const client = EtherFlowClient.builder().baseUrl("https://api.example.com").retry(3).build();
 *   const user = await client.get<User>("/users/1");
 *   const created = await client.post<User>("/users", { name: "Alice", email: "alice@example.com" });
 */

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

export interface EtherFlowConfig {
  baseUrl?: string;
  timeoutMs?: number;
  maxRetries?: number;
  retryDelayMs?: number;
  defaultHeaders?: Record<string, string>;
}

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface EtherFlowResponse<T> {
  data: T;
  status: number;
  headers: Record<string, string>;
}

export type SafeResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: EtherFlowError };

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

export class EtherFlowError extends Error {
  constructor(
    message: string,
    public readonly statusCode?: number,
    public readonly responseBody?: string
  ) {
    super(message);
    this.name = "EtherFlowError";
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Request Builder
// ─────────────────────────────────────────────────────────────────────────────

export class EtherFlowRequest {
  private _headers: Record<string, string> = {};
  private _body?: unknown;
  private _timeoutMs?: number;

  constructor(
    private readonly client: EtherFlowClient,
    private readonly method: HttpMethod,
    private readonly path: string
  ) {}

  /** Add a Bearer auth token. */
  bearerAuth(token: string): this {
    this._headers["Authorization"] = `Bearer ${token}`;
    return this;
  }

  /** Add a custom header. */
  header(key: string, value: string): this {
    this._headers[key] = value;
    return this;
  }

  /** Set the request body (will be JSON-serialised). */
  body(data: unknown): this {
    this._body = data;
    return this;
  }

  /** Override timeout for this request. */
  timeout(ms: number): this {
    this._timeoutMs = ms;
    return this;
  }

  /** Execute and deserialise to T. */
  async retrieve<T>(): Promise<T> {
    return this.client["_execute"]<T>(
      this.method,
      this.path,
      this._headers,
      this._body,
      this._timeoutMs
    );
  }

  /** Execute and return a safe Result (never throws). */
  async safeRetrieve<T>(): Promise<SafeResult<T>> {
    try {
      const data = await this.retrieve<T>();
      return { ok: true, data };
    } catch (e) {
      return { ok: false, error: e instanceof EtherFlowError ? e : new EtherFlowError(String(e)) };
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

export class EtherFlowClient {
  private readonly baseUrl: string;
  private readonly timeoutMs: number;
  private readonly maxRetries: number;
  private readonly retryDelayMs: number;
  private readonly defaultHeaders: Record<string, string>;

  private constructor(config: EtherFlowConfig = {}) {
    this.baseUrl        = (config.baseUrl ?? "").replace(/\/$/, "");
    this.timeoutMs      = config.timeoutMs      ?? 10_000;
    this.maxRetries     = config.maxRetries     ?? 3;
    this.retryDelayMs   = config.retryDelayMs   ?? 200;
    this.defaultHeaders = {
      "Accept":       "application/json",
      "Content-Type": "application/json",
      "User-Agent":   "EtherFlow-TypeScript-Client/1.0",
      ...config.defaultHeaders,
    };
  }

  // ── Builder ──────────────────────────────────────────────────────────────

  static builder(): EtherFlowClientBuilder {
    return new EtherFlowClientBuilder();
  }

  /** Convenience factory — mirrors Java EtherFlowClient.create() */
  static create(baseUrl: string): EtherFlowClient {
    return new EtherFlowClient({ baseUrl });
  }

  // ── Fluent HTTP verbs ────────────────────────────────────────────────────

  get(path: string): EtherFlowRequest {
    return new EtherFlowRequest(this, "GET", path);
  }

  post(path: string): EtherFlowRequest {
    return new EtherFlowRequest(this, "POST", path);
  }

  put(path: string): EtherFlowRequest {
    return new EtherFlowRequest(this, "PUT", path);
  }

  patch(path: string): EtherFlowRequest {
    return new EtherFlowRequest(this, "PATCH", path);
  }

  delete(path: string): EtherFlowRequest {
    return new EtherFlowRequest(this, "DELETE", path);
  }

  // ── Shorthand (mirrors Java PythonApiClient / FlaskApiClient) ────────────

  async getJson<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return this._execute("GET", path, headers ?? {});
  }

  async postJson<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> {
    return this._execute("POST", path, headers ?? {}, body);
  }

  async putJson<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> {
    return this._execute("PUT", path, headers ?? {}, body);
  }

  async deleteJson<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return this._execute("DELETE", path, headers ?? {});
  }

  // ── Health check ─────────────────────────────────────────────────────────

  async checkHealth(): Promise<Record<string, unknown>> {
    try {
      return await this.getJson<Record<string, unknown>>("/health");
    } catch {
      return { status: "DOWN", error: "Health check failed" };
    }
  }

  // ── Internal execution with retry ────────────────────────────────────────

  private async _execute<T>(
    method: HttpMethod,
    path: string,
    extraHeaders: Record<string, string> = {},
    body?: unknown,
    overrideTimeoutMs?: number
  ): Promise<T> {
    const url = path.startsWith("http") ? path : `${this.baseUrl}${path}`;
    const headers: Record<string, string> = { ...this.defaultHeaders, ...extraHeaders };
    const timeoutMs = overrideTimeoutMs ?? this.timeoutMs;

    let lastError: Error = new EtherFlowError("Unknown error");
    let delay = this.retryDelayMs;

    for (let attempt = 0; attempt <= this.maxRetries; attempt++) {
      try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);

        const init: RequestInit = {
          method,
          headers,
          signal: controller.signal,
        };
        if (body !== undefined && method !== "GET" && method !== "DELETE") {
          init.body = JSON.stringify(body);
        }

        const response = await fetch(url, init);
        clearTimeout(timer);

        const responseText = await response.text();

        if (!response.ok) {
          // Don't retry 4xx
          if (response.status >= 400 && response.status < 500) {
            throw new EtherFlowError(
              `[EtherFlow] HTTP ${response.status}: ${response.statusText}`,
              response.status,
              responseText
            );
          }
          throw new EtherFlowError(
            `[EtherFlow] HTTP ${response.status}: ${response.statusText}`,
            response.status,
            responseText
          );
        }

        try {
          return JSON.parse(responseText) as T;
        } catch {
          return responseText as unknown as T;
        }

      } catch (err) {
        lastError = err instanceof Error ? err : new EtherFlowError(String(err));

        // Don't retry client errors
        if (err instanceof EtherFlowError && err.statusCode && err.statusCode >= 400 && err.statusCode < 500) {
          throw err;
        }

        if (attempt < this.maxRetries) {
          console.warn(`[EtherFlow.TS] Retry ${attempt + 1}/${this.maxRetries} after ${delay}ms: ${lastError.message}`);
          await new Promise(resolve => setTimeout(resolve, delay));
          delay *= 2; // exponential back-off
        }
      }
    }
    throw new EtherFlowError(`[EtherFlow] Max retries exceeded. Last: ${lastError.message}`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

export class EtherFlowClientBuilder {
  private config: EtherFlowConfig = {};

  baseUrl(url: string): this {
    this.config.baseUrl = url;
    return this;
  }

  timeout(ms: number): this {
    this.config.timeoutMs = ms;
    return this;
  }

  retry(count: number): this {
    this.config.maxRetries = count;
    return this;
  }

  retryDelay(ms: number): this {
    this.config.retryDelayMs = ms;
    return this;
  }

  header(key: string, value: string): this {
    this.config.defaultHeaders = { ...this.config.defaultHeaders, [key]: value };
    return this;
  }

  build(): EtherFlowClient {
    return EtherFlowClient["create"](this.config.baseUrl ?? "");
    // @ts-ignore — access private constructor
    return new (EtherFlowClient as any)(this.config);
  }
}

// Re-export builder's build properly
(EtherFlowClientBuilder.prototype as any).build = function(this: EtherFlowClientBuilder) {
  // @ts-ignore
  return new (EtherFlowClient as any)((this as any).config);
};

// ─────────────────────────────────────────────────────────────────────────────
// Example Usage
// ─────────────────────────────────────────────────────────────────────────────

/*
interface User {
  id: number;
  name: string;
  email: string;
}

async function main() {
  // 1. Create client (mirrors Java EtherFlowClient.builder())
  const client = EtherFlowClient.builder()
    .baseUrl("https://jsonplaceholder.typicode.com")
    .retry(3)
    .timeout(10_000)
    .build();

  // 2. GET — deserialise to typed interface
  const user = await client.get("/users/1").retrieve<User>();
  console.log(`User: ${user.name} — ${user.email}`);

  // 3. GET list
  const users = await client.get("/users").retrieve<User[]>();
  console.log(`Total users: ${users.length}`);

  // 4. POST with body
  const created = await client.post("/posts")
    .body({ title: "EtherFlow TS", body: "Hello!", userId: 1 })
    .retrieve<{ id: number }>();
  console.log(`Created post ID: ${created.id}`);

  // 5. Bearer auth
  const admin = await client.get("/admin/users")
    .bearerAuth("my-secret-token")
    .retrieve<User[]>();

  // 6. Safe result — never throws
  const result = await client.get("/users/999").safeRetrieve<User>();
  if (result.ok) {
    console.log("Found:", result.data.name);
  } else {
    console.error("Error:", result.error.message);
  }

  // 7. Health check
  const health = await client.checkHealth();
  console.log("Service status:", health.status);
}

main().catch(console.error);
*/
