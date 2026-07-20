//! etherflow_client.rs
//! EtherFlow HTTP Client for Rust — reqwest + tokio async
//!
//! A strongly-typed, async HTTP client that mirrors the EtherFlow Java/Kotlin
//! builder API. Uses `reqwest` for transport and `serde` for JSON.
//!
//! Usage:
//! ```rust
//! let client = EtherFlowClient::builder()
//!     .base_url("https://api.example.com")
//!     .retry(3)
//!     .build()?;
//!
//! let user: User = client.get("/users/1").await?;
//! let created: User = client.post("/users", &new_user).await?;
//! ```

use reqwest::{Client, Method, RequestBuilder, StatusCode};
use serde::{de::DeserializeOwned, Serialize};
use std::collections::HashMap;
use std::time::Duration;
use tokio::time::sleep;

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

/// EtherFlow client error.
#[derive(Debug, thiserror::Error)]
pub enum EtherFlowError {
    #[error("[EtherFlow] Network error: {0}")]
    Network(#[from] reqwest::Error),

    #[error("[EtherFlow] HTTP {status}: {body}")]
    Http { status: u16, body: String },

    #[error("[EtherFlow] Serialization error: {0}")]
    Serialize(#[from] serde_json::Error),

    #[error("[EtherFlow] Max retries exceeded: {last_error}")]
    MaxRetriesExceeded { last_error: String },

    #[error("[EtherFlow] Build error: {0}")]
    Build(String),
}

pub type EtherFlowResult<T> = Result<T, EtherFlowError>;

// ─────────────────────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────────────────────

/// Configuration for the EtherFlow Rust client.
#[derive(Debug, Clone)]
pub struct EtherFlowConfig {
    pub base_url: String,
    pub timeout: Duration,
    pub max_retries: u32,
    pub retry_delay: Duration,
    pub default_headers: HashMap<String, String>,
}

impl Default for EtherFlowConfig {
    fn default() -> Self {
        Self {
            base_url: String::new(),
            timeout: Duration::from_secs(10),
            max_retries: 3,
            retry_delay: Duration::from_millis(200),
            default_headers: HashMap::new(),
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

/// EtherFlow HTTP client for Rust.
#[derive(Clone)]
pub struct EtherFlowClient {
    config: EtherFlowConfig,
    inner: Client,
}

impl EtherFlowClient {
    /// Create client from config.
    pub fn new(config: EtherFlowConfig) -> EtherFlowResult<Self> {
        let mut builder = Client::builder()
            .timeout(config.timeout)
            .user_agent("EtherFlow-Rust-Client/1.0");

        // Apply default headers via a static header map
        let mut headers = reqwest::header::HeaderMap::new();
        headers.insert(
            reqwest::header::ACCEPT,
            reqwest::header::HeaderValue::from_static("application/json"),
        );
        for (k, v) in &config.default_headers {
            if let (Ok(name), Ok(val)) = (
                reqwest::header::HeaderName::from_bytes(k.as_bytes()),
                reqwest::header::HeaderValue::from_str(v),
            ) {
                headers.insert(name, val);
            }
        }
        builder = builder.default_headers(headers);

        let inner = builder
            .build()
            .map_err(|e| EtherFlowError::Build(e.to_string()))?;

        Ok(Self { config, inner })
    }

    /// Fluent builder factory.
    pub fn builder() -> EtherFlowClientBuilder {
        EtherFlowClientBuilder::default()
    }

    /// Convenience factory — mirrors Java EtherFlowClient.create().
    pub fn create(base_url: impl Into<String>) -> EtherFlowResult<Self> {
        Self::new(EtherFlowConfig {
            base_url: base_url.into(),
            ..Default::default()
        })
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /// GET request — deserialise response JSON into T.
    pub async fn get<T: DeserializeOwned>(&self, path: &str) -> EtherFlowResult<T> {
        self.execute(Method::GET, path, None::<&()>, &HashMap::new()).await
    }

    /// GET with additional headers.
    pub async fn get_with_headers<T: DeserializeOwned>(
        &self,
        path: &str,
        headers: &HashMap<String, String>,
    ) -> EtherFlowResult<T> {
        self.execute(Method::GET, path, None::<&()>, headers).await
    }

    /// POST with JSON body.
    pub async fn post<B: Serialize, T: DeserializeOwned>(
        &self,
        path: &str,
        body: &B,
    ) -> EtherFlowResult<T> {
        self.execute(Method::POST, path, Some(body), &HashMap::new()).await
    }

    /// PUT with JSON body.
    pub async fn put<B: Serialize, T: DeserializeOwned>(
        &self,
        path: &str,
        body: &B,
    ) -> EtherFlowResult<T> {
        self.execute(Method::PUT, path, Some(body), &HashMap::new()).await
    }

    /// PATCH with JSON body.
    pub async fn patch<B: Serialize, T: DeserializeOwned>(
        &self,
        path: &str,
        body: &B,
    ) -> EtherFlowResult<T> {
        self.execute(Method::PATCH, path, Some(body), &HashMap::new()).await
    }

    /// DELETE request.
    pub async fn delete<T: DeserializeOwned>(&self, path: &str) -> EtherFlowResult<T> {
        self.execute(Method::DELETE, path, None::<&()>, &HashMap::new()).await
    }

    /// Safe GET — returns Result, never panics.
    pub async fn get_result<T: DeserializeOwned>(&self, path: &str) -> EtherFlowResult<T> {
        self.get(path).await
    }

    /// Health check — calls `/health` on the base URL.
    pub async fn check_health(&self) -> HashMap<String, serde_json::Value> {
        match self.get::<HashMap<String, serde_json::Value>>("/health").await {
            Ok(map) => map,
            Err(e) => {
                let mut m = HashMap::new();
                m.insert("status".to_string(), serde_json::Value::String("DOWN".to_string()));
                m.insert("error".to_string(), serde_json::Value::String(e.to_string()));
                m
            }
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    fn resolve_url(&self, path: &str) -> String {
        if path.starts_with("http") {
            path.to_string()
        } else {
            format!("{}{}", self.config.base_url.trim_end_matches('/'), path)
        }
    }

    async fn execute<B: Serialize, T: DeserializeOwned>(
        &self,
        method: Method,
        path: &str,
        body: Option<&B>,
        extra_headers: &HashMap<String, String>,
    ) -> EtherFlowResult<T> {
        let url = self.resolve_url(path);
        let mut last_error = EtherFlowError::MaxRetriesExceeded {
            last_error: "Unknown".to_string(),
        };
        let mut delay = self.config.retry_delay;

        for attempt in 0..=self.config.max_retries {
            let mut req_builder: RequestBuilder = self.inner.request(method.clone(), &url);

            req_builder = req_builder.header("Content-Type", "application/json");
            for (k, v) in extra_headers {
                req_builder = req_builder.header(k, v);
            }
            if let Some(b) = body {
                req_builder = req_builder.json(b);
            }

            match req_builder.send().await {
                Ok(resp) => {
                    let status = resp.status();
                    let body_text = resp.text().await.unwrap_or_default();

                    if status.is_success() {
                        let parsed = serde_json::from_str::<T>(&body_text)?;
                        return Ok(parsed);
                    }

                    let err = EtherFlowError::Http {
                        status: status.as_u16(),
                        body: body_text,
                    };

                    // Don't retry 4xx
                    if status.is_client_error() {
                        return Err(err);
                    }
                    last_error = err;
                }
                Err(e) => {
                    last_error = EtherFlowError::Network(e);
                }
            }

            if attempt < self.config.max_retries {
                eprintln!(
                    "[EtherFlow.Rust] Retry {}/{} after {:?}: {}",
                    attempt + 1,
                    self.config.max_retries,
                    delay,
                    last_error
                );
                sleep(delay).await;
                delay *= 2;
            }
        }

        Err(EtherFlowError::MaxRetriesExceeded {
            last_error: last_error.to_string(),
        })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

/// Fluent builder for EtherFlowClient (mirrors Java API).
#[derive(Default)]
pub struct EtherFlowClientBuilder {
    config: EtherFlowConfig,
}

impl EtherFlowClientBuilder {
    pub fn base_url(mut self, url: impl Into<String>) -> Self {
        self.config.base_url = url.into();
        self
    }

    pub fn timeout(mut self, d: Duration) -> Self {
        self.config.timeout = d;
        self
    }

    pub fn retry(mut self, n: u32) -> Self {
        self.config.max_retries = n;
        self
    }

    pub fn retry_delay(mut self, d: Duration) -> Self {
        self.config.retry_delay = d;
        self
    }

    pub fn header(mut self, key: impl Into<String>, value: impl Into<String>) -> Self {
        self.config.default_headers.insert(key.into(), value.into());
        self
    }

    pub fn build(self) -> EtherFlowResult<EtherFlowClient> {
        EtherFlowClient::new(self.config)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Example Usage
// ─────────────────────────────────────────────────────────────────────────────

/*
// Cargo.toml dependencies:
// reqwest = { version = "0.12", features = ["json"] }
// serde = { version = "1", features = ["derive"] }
// serde_json = "1"
// tokio = { version = "1", features = ["full"] }
// thiserror = "1"

use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize, Serialize)]
struct User {
    id: Option<u32>,
    name: String,
    email: String,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 1. Create client using builder
    let client = EtherFlowClient::builder()
        .base_url("https://jsonplaceholder.typicode.com")
        .retry(3)
        .timeout(std::time::Duration::from_secs(10))
        .build()?;

    // 2. GET — deserialise into struct
    let user: User = client.get("/users/1").await?;
    println!("User: {} — {}", user.name, user.email);

    // 3. GET list
    let users: Vec<User> = client.get("/users").await?;
    println!("Total users: {}", users.len());

    // 4. POST with body
    let new_user = User { id: None, name: "Alice".to_string(), email: "alice@example.com".to_string() };
    let created: serde_json::Value = client.post("/users", &new_user).await?;
    println!("Created post ID: {}", created["id"]);

    // 5. Health check
    let health = client.check_health().await;
    println!("Status: {:?}", health.get("status"));

    Ok(())
}
*/
