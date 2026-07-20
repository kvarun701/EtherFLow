// VaporServer.swift
// EtherFlow-compatible REST API server using Vapor 4 (Swift)
//
// Exposes the same endpoints as the Python Flask/FastAPI servers so EtherFlow
// Java/Kotlin clients can call them seamlessly.
//
// Run with:
//   swift run
//
// Endpoints:
//   GET  /api/swift/health
//   GET  /api/swift/hello?name=Alice
//   GET  /api/swift/users/:id
//   POST /api/swift/external-post

import Vapor
import Foundation

// MARK: - Models

struct GreetingResponse: Content {
    let service: String
    let greeting: String
    let framework: String
}

struct UserResponse: Content {
    let id: Int
    let name: String
    let role: String
    let active: Bool
}

struct HealthResponse: Content {
    let status: String
    let framework: String
    let message: String
}

struct PostPayload: Content {
    let title: String?
    let body: String?
    let userId: Int?
}

struct PostResponse: Content {
    let status: String
    let source: String
    let receivedPayload: PostPayload
    let remoteId: Int
}

// MARK: - Server Configuration

func configure(_ app: Application) throws {
    app.http.server.configuration.port = 5004

    // ── Routes ────────────────────────────────────────────────────────────────

    // Health
    app.get("api", "swift", "health") { req async throws -> HealthResponse in
        HealthResponse(
            status: "UP",
            framework: "Vapor 4 (Swift)",
            message: "Swift API server is running smoothly via EtherFlow"
        )
    }

    // Hello
    app.get("api", "swift", "hello") { req async throws -> GreetingResponse in
        let name = req.query[String.self, at: "name"] ?? "EtherFlow User"
        return GreetingResponse(
            service: "Swift Vapor API",
            greeting: "Hello, \(name) from Swift + Vapor!",
            framework: "Vapor 4 / Swift 5.9"
        )
    }

    // Get user by ID
    app.get("api", "swift", "users", ":id") { req async throws -> UserResponse in
        guard let id = req.parameters.get("id", as: Int.self) else {
            throw Abort(.badRequest, reason: "Invalid user ID")
        }
        return UserResponse(id: id, name: "Swift User \(id)", role: "Developer", active: true)
    }

    // Post external
    app.post("api", "swift", "external-post") { req async throws -> PostResponse in
        let payload = try req.content.decode(PostPayload.self)

        // Call third-party API using EtherFlowClient (from same package)
        let client = EtherFlowClient(baseURL: "https://jsonplaceholder.typicode.com")
        let created: [String: AnyDecodable] = try await client.post(
            "/posts",
            body: payload
        )

        return PostResponse(
            status: "success",
            source: "Swift Vapor → Third-Party API",
            receivedPayload: payload,
            remoteId: created["id"]?.value as? Int ?? 0
        )
    }
}

// MARK: - AnyDecodable helper

struct AnyDecodable: Decodable {
    let value: Any

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let int = try? container.decode(Int.self)        { value = int }
        else if let str = try? container.decode(String.self) { value = str }
        else if let dbl = try? container.decode(Double.self) { value = dbl }
        else if let bool = try? container.decode(Bool.self)  { value = bool }
        else { value = NSNull() }
    }
}

// MARK: - Entry Point

@main
struct VaporApp {
    static func main() async throws {
        var env = try Environment.detect()
        try LoggingSystem.bootstrap(from: &env)
        let app = Application(env)
        defer { app.shutdown() }
        try configure(app)
        print("Swift Vapor API server running on http://localhost:5004")
        try await app.runFromAsyncMainEntrypoint()
    }
}
