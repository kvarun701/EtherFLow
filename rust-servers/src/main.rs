// main.rs — EtherFlow Axum REST API Server for Rust
// Exposes the same endpoint surface as Python Flask/FastAPI servers
// so that EtherFlow Java/Kotlin clients can call them seamlessly.
//
// Run: cargo run
// Port: 5006

mod etherflow_client;

use axum::{
    extract::{Path, Query},
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::HashMap;
use tokio::net::TcpListener;

// ─────────────────────────────────────────────────────────────────────────────
// Models
// ─────────────────────────────────────────────────────────────────────────────

#[derive(Serialize)]
struct HealthResponse {
    status: String,
    framework: String,
    message: String,
}

#[derive(Serialize)]
struct GreetingResponse {
    service: String,
    greeting: String,
    framework: String,
}

#[derive(Serialize)]
struct UserResponse {
    id: u32,
    name: String,
    role: String,
    active: bool,
}

#[derive(Deserialize)]
struct HelloQuery {
    name: Option<String>,
}

#[derive(Deserialize, Serialize)]
struct PredictRequest {
    inputs: Option<Vec<f64>>,
}

#[derive(Serialize)]
struct PredictResult {
    sum: f64,
    average: f64,
    score: f64,
}

#[derive(Serialize)]
struct PredictResponse {
    status: String,
    inputs: Vec<f64>,
    prediction: PredictResult,
}

// ─────────────────────────────────────────────────────────────────────────────
// Handlers
// ─────────────────────────────────────────────────────────────────────────────

async fn health() -> Json<Value> {
    Json(json!({
        "status":    "UP",
        "framework": "Axum (Rust)",
        "message":   "Rust API server is running smoothly via EtherFlow"
    }))
}

async fn hello(Query(params): Query<HelloQuery>) -> Json<Value> {
    let name = params.name.unwrap_or_else(|| "EtherFlow User".to_string());
    Json(json!({
        "service":   "Rust Axum API",
        "greeting":  format!("Hello, {} from Rust + Axum!", name),
        "framework": "Axum 0.7 / Tokio 1.x"
    }))
}

async fn get_user(Path(id): Path<u32>) -> Json<Value> {
    Json(json!({
        "id":     id,
        "name":   format!("Rust User {}", id),
        "role":   "Developer",
        "active": true
    }))
}

async fn predict(
    axum::extract::Json(body): axum::extract::Json<PredictRequest>
) -> Json<Value> {
    let inputs = body.inputs.unwrap_or_else(|| vec![10.0, 20.0, 30.0]);
    let total: f64 = inputs.iter().sum();
    let avg = if inputs.is_empty() { 0.0 } else { total / inputs.len() as f64 };
    let score = (avg * 1.5 * 100.0).round() / 100.0;

    Json(json!({
        "status":  "success",
        "inputs":  inputs,
        "prediction": {
            "sum":     total,
            "average": avg,
            "score":   score
        }
    }))
}

async fn external_post(
    axum::extract::Json(payload): axum::extract::Json<Value>
) -> Result<(StatusCode, Json<Value>), (StatusCode, Json<Value>)> {
    // Call JSONPlaceholder via EtherFlowClient
    let client = etherflow_client::EtherFlowClient::create(
        "https://jsonplaceholder.typicode.com"
    ).map_err(|e| (
        StatusCode::INTERNAL_SERVER_ERROR,
        Json(json!({"error": e.to_string()}))
    ))?;

    let remote: Value = client.post("/posts", &payload).await.map_err(|e| (
        StatusCode::BAD_GATEWAY,
        Json(json!({"error": e.to_string()}))
    ))?;

    Ok((StatusCode::CREATED, Json(json!({
        "status":         "success",
        "source":         "Rust Axum → Third-Party API",
        "remoteResponse": remote
    }))))
}

// ─────────────────────────────────────────────────────────────────────────────
// Router
// ─────────────────────────────────────────────────────────────────────────────

fn app() -> Router {
    Router::new()
        .route("/api/rust/health",        get(health))
        .route("/api/rust/hello",         get(hello))
        .route("/api/rust/users/:id",     get(get_user))
        .route("/api/rust/predict",       post(predict))
        .route("/api/rust/external-post", post(external_post))
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────────────────

#[tokio::main]
async fn main() {
    let listener = TcpListener::bind("0.0.0.0:5006").await.unwrap();
    println!("Rust Axum API server running on http://localhost:5006");
    println!("  GET  http://localhost:5006/api/rust/health");
    println!("  GET  http://localhost:5006/api/rust/hello?name=Alice");
    println!("  GET  http://localhost:5006/api/rust/users/1");
    println!("  POST http://localhost:5006/api/rust/predict");
    println!("  POST http://localhost:5006/api/rust/external-post");
    axum::serve(listener, app()).await.unwrap();
}
