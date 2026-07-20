// EtherFlowClient.swift
// EtherFlow Reactive HTTP Client for Swift — URLSession + async/await
//
// Mirrors the EtherFlow Java/Kotlin client API using Swift's native
// async/await concurrency model and URLSession transport.
//
// Supported platforms: macOS 12+, iOS 15+, watchOS 8+, tvOS 15+
//
// Usage:
//   let client = EtherFlowClient(baseURL: "https://api.example.com")
//   let user: User = try await client.get("/users/1")
//   let created: User = try await client.post("/users", body: newUser)

import Foundation

// MARK: - Errors

public enum EtherFlowError: Error, LocalizedError {
    case invalidURL(String)
    case networkError(Error)
    case httpError(statusCode: Int, body: String)
    case decodingError(Error)
    case maxRetriesExceeded(lastError: Error)

    public var errorDescription: String? {
        switch self {
        case .invalidURL(let url):          return "[EtherFlow] Invalid URL: \(url)"
        case .networkError(let e):          return "[EtherFlow] Network error: \(e.localizedDescription)"
        case .httpError(let code, let b):   return "[EtherFlow] HTTP \(code): \(b)"
        case .decodingError(let e):         return "[EtherFlow] Decoding error: \(e.localizedDescription)"
        case .maxRetriesExceeded(let e):    return "[EtherFlow] Max retries exceeded. Last: \(e.localizedDescription)"
        }
    }
}

// MARK: - Configuration

public struct EtherFlowConfig {
    public var baseURL: String
    public var timeoutInterval: TimeInterval
    public var maxRetries: Int
    public var retryDelay: TimeInterval         // base delay (doubles each attempt)
    public var defaultHeaders: [String: String]

    public init(
        baseURL: String = "",
        timeoutInterval: TimeInterval = 10,
        maxRetries: Int = 3,
        retryDelay: TimeInterval = 0.2,
        defaultHeaders: [String: String] = [:]
    ) {
        self.baseURL        = baseURL.hasSuffix("/") ? String(baseURL.dropLast()) : baseURL
        self.timeoutInterval = timeoutInterval
        self.maxRetries     = maxRetries
        self.retryDelay     = retryDelay
        self.defaultHeaders = defaultHeaders
    }
}

// MARK: - EtherFlowClient

/// Native Swift HTTP client for calling REST APIs built on EtherFlow (or any JSON REST API).
/// Uses `URLSession` and Swift's `async`/`await` — no third-party dependencies.
public final class EtherFlowClient {

    private let config: EtherFlowConfig
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    // MARK: Init

    public init(config: EtherFlowConfig = EtherFlowConfig()) {
        self.config = config

        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest  = config.timeoutInterval
        sessionConfig.timeoutIntervalForResource = config.timeoutInterval * 4
        sessionConfig.httpAdditionalHeaders      = config.defaultHeaders as [AnyHashable: Any]
        self.session = URLSession(configuration: sessionConfig)

        self.decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        self.encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
    }

    /// Convenience initialiser — pass just the base URL.
    public convenience init(baseURL: String, maxRetries: Int = 3) {
        self.init(config: EtherFlowConfig(baseURL: baseURL, maxRetries: maxRetries))
    }

    // MARK: - Public API

    /// Perform a GET request and decode the response into `T`.
    public func get<T: Decodable>(
        _ path: String,
        headers: [String: String] = [:]
    ) async throws -> T {
        let request = try buildRequest(method: "GET", path: path, headers: headers)
        return try await execute(request)
    }

    /// Perform a GET request and return the raw response body as `Data`.
    public func getData(_ path: String, headers: [String: String] = []) async throws -> Data {
        let request = try buildRequest(method: "GET", path: path, headers: headers)
        return try await executeRaw(request).0
    }

    /// Perform a POST request with a JSON-encodable body and decode the response.
    public func post<Body: Encodable, T: Decodable>(
        _ path: String,
        body: Body,
        headers: [String: String] = [:]
    ) async throws -> T {
        let bodyData = try encoder.encode(body)
        let request  = try buildRequest(method: "POST", path: path, body: bodyData, headers: headers)
        return try await execute(request)
    }

    /// Perform a PUT request.
    public func put<Body: Encodable, T: Decodable>(
        _ path: String,
        body: Body,
        headers: [String: String] = [:]
    ) async throws -> T {
        let bodyData = try encoder.encode(body)
        let request  = try buildRequest(method: "PUT", path: path, body: bodyData, headers: headers)
        return try await execute(request)
    }

    /// Perform a PATCH request.
    public func patch<Body: Encodable, T: Decodable>(
        _ path: String,
        body: Body,
        headers: [String: String] = [:]
    ) async throws -> T {
        let bodyData = try encoder.encode(body)
        let request  = try buildRequest(method: "PATCH", path: path, body: bodyData, headers: headers)
        return try await execute(request)
    }

    /// Perform a DELETE request.
    public func delete<T: Decodable>(
        _ path: String,
        headers: [String: String] = [:]
    ) async throws -> T {
        let request = try buildRequest(method: "DELETE", path: path, headers: headers)
        return try await execute(request)
    }

    /// Safe variant — returns `Result<T, EtherFlowError>` instead of throwing.
    public func getResult<T: Decodable>(
        _ path: String,
        headers: [String: String] = [:]
    ) async -> Result<T, EtherFlowError> {
        do {
            let value: T = try await get(path, headers: headers)
            return .success(value)
        } catch let e as EtherFlowError {
            return .failure(e)
        } catch {
            return .failure(.networkError(error))
        }
    }

    // MARK: - Health Check

    /// Calls `/health` on the configured base URL and returns status.
    public func checkHealth() async -> [String: String] {
        do {
            let data = try await getData("/health")
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: String] {
                return json
            }
        } catch {}
        return ["status": "DOWN", "error": "Health check failed"]
    }

    // MARK: - Private Helpers

    private func buildRequest(
        method: String,
        path: String,
        body: Data? = nil,
        headers: [String: String] = [:]
    ) throws -> URLRequest {
        let urlString = path.hasPrefix("http") ? path : "\(config.baseURL)\(path)"
        guard let url = URL(string: urlString) else {
            throw EtherFlowError.invalidURL(urlString)
        }

        var request          = URLRequest(url: url)
        request.httpMethod   = method
        request.timeoutInterval = config.timeoutInterval
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("EtherFlow-Swift-Client/1.0", forHTTPHeaderField: "User-Agent")

        if let body = body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        return request
    }

    private func execute<T: Decodable>(_ request: URLRequest) async throws -> T {
        let (data, _) = try await executeRaw(request)
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw EtherFlowError.decodingError(error)
        }
    }

    private func executeRaw(_ request: URLRequest) async throws -> (Data, URLResponse) {
        var lastError: Error = EtherFlowError.networkError(URLError(.unknown))
        var delay = config.retryDelay

        for attempt in 0...config.maxRetries {
            do {
                let (data, response) = try await session.data(for: request)
                guard let http = response as? HTTPURLResponse else {
                    throw EtherFlowError.networkError(URLError(.badServerResponse))
                }
                guard (200...299).contains(http.statusCode) else {
                    let body = String(data: data, encoding: .utf8) ?? ""
                    throw EtherFlowError.httpError(statusCode: http.statusCode, body: body)
                }
                return (data, response)
            } catch let e as EtherFlowError {
                // Don't retry on HTTP 4xx
                if case .httpError(let code, _) = e, (400...499).contains(code) { throw e }
                lastError = e
            } catch {
                lastError = error
            }

            if attempt < config.maxRetries {
                print("[EtherFlow.Swift] Retry \(attempt + 1)/\(config.maxRetries) after \(delay)s: \(lastError.localizedDescription)")
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                delay *= 2
            }
        }
        throw EtherFlowError.maxRetriesExceeded(lastError: lastError)
    }
}

// MARK: - Builder DSL (mirrors EtherFlow Java builder)

public extension EtherFlowClient {
    /// Fluent builder for EtherFlowClient.
    final class Builder {
        private var config = EtherFlowConfig()

        @discardableResult
        public func baseURL(_ url: String) -> Builder {
            config.baseURL = url; return self
        }

        @discardableResult
        public func timeout(_ seconds: TimeInterval) -> Builder {
            config.timeoutInterval = seconds; return self
        }

        @discardableResult
        public func retry(_ count: Int) -> Builder {
            config.maxRetries = count; return self
        }

        @discardableResult
        public func header(_ key: String, _ value: String) -> Builder {
            config.defaultHeaders[key] = value; return self
        }

        public func build() -> EtherFlowClient {
            EtherFlowClient(config: config)
        }
    }

    static func builder() -> Builder { Builder() }
}

// MARK: - Example Usage

/*
 // Runnable example (requires macOS 12+ / iOS 15+):

 struct User: Codable {
     let id: Int
     let name: String
     let email: String
 }

 struct NewPost: Encodable {
     let title: String
     let body: String
     let userId: Int
 }

 @main
 struct EtherFlowDemo {
     static func main() async throws {
         // 1. Create client using builder (mirrors Java API)
         let client = EtherFlowClient.builder()
             .baseURL("https://jsonplaceholder.typicode.com")
             .retry(3)
             .timeout(10)
             .build()

         // 2. GET — deserialise into Swift struct
         let user: User = try await client.get("/users/1")
         print("User: \(user.name) — \(user.email)")

         // 3. GET list
         let users: [User] = try await client.get("/users")
         print("Total users: \(users.count)")

         // 4. POST with body
         let post = NewPost(title: "EtherFlow Swift", body: "Hello from Swift!", userId: 1)
         let created: [String: Any] = try await client.post("/posts", body: post)
         print("Created post ID: \(created["id"] ?? "?")")

         // 5. Safe result — never throws
         let result: Result<User, EtherFlowError> = await client.getResult("/users/999")
         switch result {
         case .success(let u):  print("Found: \(u.name)")
         case .failure(let e):  print("Error: \(e.errorDescription ?? "")")
         }

         // 6. Health check
         let health = await client.checkHealth()
         print("Service status: \(health["status"] ?? "unknown")")
     }
 }
 */
