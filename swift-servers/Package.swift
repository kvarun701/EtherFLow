// swift-package: Package.swift
// Swift Package Manager manifest for EtherFlow Swift client + Vapor server

// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "etherflow-swift",
    platforms: [
        .macOS(.v13),
        .iOS(.v16),
    ],
    products: [
        .library(name: "EtherFlowSwift", targets: ["EtherFlowSwift"]),
        .executable(name: "VaporServer",  targets: ["VaporServer"]),
    ],
    dependencies: [
        .package(url: "https://github.com/vapor/vapor.git", from: "4.89.0"),
    ],
    targets: [
        // ── EtherFlow Swift Client ──────────────────────────────────────────
        .target(
            name: "EtherFlowSwift",
            path:    "Sources/EtherFlowSwift",
            sources: ["EtherFlowClient.swift"]
        ),

        // ── Vapor Server ────────────────────────────────────────────────────
        .executableTarget(
            name: "VaporServer",
            dependencies: [
                .product(name: "Vapor", package: "vapor"),
                "EtherFlowSwift",
            ],
            path:    "Sources/VaporServer",
            sources: ["VaporServer.swift"]
        ),

        // ── Tests ───────────────────────────────────────────────────────────
        .testTarget(
            name: "EtherFlowSwiftTests",
            dependencies: ["EtherFlowSwift"],
            path: "Tests/EtherFlowSwiftTests"
        ),
    ]
)
