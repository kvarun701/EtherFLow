---
title: EtherFlow
description: A lightweight, high-performance Reactive Web Framework for Java, Kotlin, iOS, JS & Kotlin Multiplatform built from scratch
---

# EtherFlow

A lightweight, high-performance Reactive Web Framework for Java, Kotlin, iOS, JS & Kotlin Multiplatform built from scratch.

[GitHub](https://github.com/kvarun701/EtherFLow) | [API Docs](api)

[![Deploy to Render](https://render.com/images/deploy-to-render.svg)](https://render.com/deploy?repo=https://github.com/kvarun701/EtherFLow)

## Features

- **Reactive Streams SPI** — Publisher, Subscriber, Subscription, Processor
- **Mono / Flux** — Rich operators: map, flatMap, filter, switchIfEmpty, then, thenReturn, subscribeOn, publishOn, block, defer, fromCallable
- **Schedulers** — parallel(), single(), boundedElastic(), timer(), immediate()
- **Functional Endpoints** — RouterFunction builder + HandlerFunction + RequestPredicate DSL
- **JSON Ser/Des** — Jackson-based HttpMessageReader/Writer
- **Front Controller** — DispatcherHandler with pluggable HandlerMapping / HandlerAdapter
- **Filter Chain** — WebFilter + WebExceptionHandler pipeline
- **Netty Server** — Run on Netty with a single entry point
- **Zero Spring Dependency** — No ApplicationContext, no autoconfiguration, no XML
- **Java 21+ & Kotlin** — Sealed classes, pattern matching, records, virtual threads compatible
- **KMP HTTP Client** — Fluent API with multipart upload, binary download, streaming (`Flow<ByteArray>`), WebSocket — on JVM, Android, iOS, JS

## Quick Start

```xml
<dependency>
    <groupId>io.etherflow</groupId>
    <artifactId>etherflow-starter-webflux</artifactId>
    <version>0.1.0</version>
    <type>pom</type>
</dependency>
```

```java
import io.etherflow.core.Mono;
import io.etherflow.server.netty.NettyServer;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.*;

public class App {
    public static void main(String[] args) throws Exception {
        RouterFunction routes = RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("Hello EtherFlow!")))
                .build();

        DispatcherHandler dispatcher = new DispatcherHandler();
        dispatcher.addHandlerMapping(new RouterFunctionMapping(routes));
        dispatcher.addHandlerAdapter(new RouterFunctionMapping(routes));

        NettyServer server = new NettyServer(8080, dispatcher);
        server.start();
        System.out.println("Server running on http://localhost:8080");
        server.await();
    }
}
```

## Comparison Guide

| Aspect | Spring WebFlux | Project Reactor | EtherFlow |
|:---|:---|:---|:---|
| **Primary Focus** | Heavy enterprise-grade reactive web framework | Foundation library for reactive streams (types only) | Lightweight, self-contained reactive web framework |
| **Footprint (JAR Size)** | Large (~15-20 MB including Spring runtime) | Small (~3 MB, but requires separate Web Server like Netty) | Ultra-lightweight (~2 MB self-contained web framework) |
| **Startup Time** | Slow (2 - 5 seconds due to DI & classpath scanning) | Instant (library only, no server startup out of the box) | Blazing fast (< 100ms full server startup) |
| **Dependencies** | Spring Context, Beans, AOP, Web, Jackson, Netty, Reactor | Zero dependencies (core reactive library only) | Minimal (Netty for server + Jackson for JSON, zero Spring) |
| **Simplicity** | Complex (deep hierarchy, proxy-based AOP, 15+ annotations) | Medium (complex operators and concurrency models) | Simple & clean (minimal interfaces, explicit builder DSL) |
| **Learning Curve** | High (must learn Spring DI + Reactive models) | Medium (focuses only on reactive streams APIs) | Low (straightforward reactive model + functional DSL) |
| **Multiplatform Client** | Tied to Reactor/Netty, JVM-only | JVM-only | Multiplatform KMP client (runs on JVM, iOS, Android, JS) |

## Architecture

```
Functional Endpoints: RouterFunction → HandlerFunction
                           ↓
DispatcherHandler (Front Controller)
HandlerMapping → HandlerAdapter
WebFilter → WebExceptionHandler
                           ↓
HttpHandler (Server Abstraction)
ServerWebExchange, ServerHttpRequest/Response
                           ↓
Codec Layer: HttpMessageReader/Writer, JacksonCodec
                           ↓
Reactive Core: Mono/Flux + Schedulers + Reactive Streams SPI
```

## Building

```bash
git clone https://github.com/kvarun701/EtherFLow.git
cd EtherFlow
mvn compile
mvn test
mvn install -DskipTests
```

Requires: Java 21+, Apache Maven 3.8+

### KMP HTTP Client

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.etherflow:etherflow-client-kmp:0.1.0")
}
```

```kotlin
import io.etherflow.client.kmp.*

val client = platformHttpClient {
    baseUrl = "https://api.example.com"
    retryCount = 2
}

// JSON
val user: User = client.get("/users/1").bodyAs<User>()

// Multipart upload
client.post("/upload").multipart {
    field("user", "bob")
    file("avatar", "photo.jpg", bytes, "image/jpeg")
}.execute()

// Binary download
val bytes: ByteArray = client.get("/image.png").bodyAsBytes()
client.get("/large-file.zip").downloadTo("/tmp/output.zip")

// Streaming
val streamed = client.get("/video.mp4").stream()
streamed.chunks.collect { chunk -> /* process */ }

// WebSocket
val ws = client.webSocket("wss://echo.example.com")
ws.send("Hello")
ws.incoming.collect { msg ->
    when (msg) {
        is WebSocketMessage.Text -> println(msg.text)
        is WebSocketMessage.Binary -> println("binary: ${msg.data.size}B")
    }
}
ws.close()
```

### Java-friendly API

```java
import io.etherflow.client.kmp.*;

HttpClient client = new HttpClient(new HttpClientConfig());

HttpResponse response = client.get("https://api.example.com/users/{id}", 1)
    .bearerAuth("token123")
    .execute();

int status = response.getStatusCode();
String body = response.getBodyAsString();

HttpResponse created = client.post("https://api.example.com/users")
    .bodyJson("{\"name\":\"Alice\"}")
    .execute();

byte[] image = client.get("/image.png").bodyAsBytes();
```

### Compose Multiplatform

Add `etherflow-client-compose` for Compose-friendly HTTP helpers:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.etherflow:etherflow-client-compose:0.1.0")
}
```

```kotlin
@Composable
fun UserProfile(userId: String) {
    val client = rememberHttpClient { baseUrl = "https://api.example.com" }
    val userState = httpGetAs(client, "/users/{id}", userId, serializer = User.serializer())

    when (val state = userState.value) {
        is HttpRequestState.Loading -> CircularProgressIndicator()
        is HttpRequestState.Success -> Text("Hello, ${state.data.name}")
        is HttpRequestState.Error -> Text("Error: ${state.exception.message}")
    }
}
```

### KMP targets & features

| Target | Engine | JSON | Multipart | Download | Streaming | WebSocket | Compose |
|--------|--------|------|-----------|----------|-----------|-----------|---------|
| JVM Desktop | OkHttp | ✅ | ✅ | ✅ | ✅ 8 KB | ✅ | ✅ |
| Android | OkHttp | ✅ | ✅ | ✅ | ✅ 8 KB | ✅ | ✅ |
| iOS | NSURLSession | ✅ | ✅ | ✅ | ⏳ | ✅ | ✅ |
| JS Browser | `window.fetch` | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |

### Modules

| Module | Description |
|--------|-------------|
| `etherflow-core` | Mono/Flux, Schedulers — reactive types |
| `etherflow-web` | RouterFunction DSL, DispatcherHandler |
| `etherflow-server-netty` | Netty server adapter |
| `etherflow-client-kmp` | KMP HTTP client (multiplatform — JVM, Android, iOS, JS) |
| `etherflow-client-compose` | Compose Multiplatform helpers (Android, iOS, Desktop, Web) |

MIT

<style>
.code-container {
  position: relative;
  margin-bottom: 1.5rem;
}
.copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(30, 41, 59, 0.85);
  color: #e2e8f0;
  border: 1px solid #475569;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease, transform 0.1s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  font-family: system-ui, -apple-system, sans-serif;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
}
.code-container:hover .copy-btn {
  opacity: 1;
}
.copy-btn:hover {
  background: #334155;
  color: #fff;
  transform: translateY(-1px);
}
.copy-btn:active {
  transform: translateY(0);
}
.copy-btn.copied {
  background: #059669;
  border-color: #047857;
  color: #fff;
}
.copy-btn svg {
  flex-shrink: 0;
}
</style>

<script>
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('div.highlighter-rouge pre, figure.highlight pre, pre').forEach((pre) => {
    if (pre.parentElement.classList.contains('code-container')) return;

    // Wrap pre in container
    const wrapper = document.createElement('div');
    wrapper.className = 'code-container';
    pre.parentNode.insertBefore(wrapper, pre);
    wrapper.appendChild(pre);

    // Create copy button
    const btn = document.createElement('button');
    btn.className = 'copy-btn';
    btn.setAttribute('aria-label', 'Copy code to clipboard');
    btn.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
      </svg>
      <span>Copy</span>
    `;
    wrapper.appendChild(btn);

    btn.addEventListener('click', () => {
      const codeText = pre.innerText;
      navigator.clipboard.writeText(codeText).then(() => {
        const span = btn.querySelector('span');
        span.innerText = 'Copied!';
        btn.classList.add('copied');
        
        const originalIcon = btn.querySelector('svg').innerHTML;
        btn.querySelector('svg').innerHTML = `
          <polyline points="20 6 9 17 4 12"></polyline>
        `;

        setTimeout(() => {
          span.innerText = 'Copy';
          btn.classList.remove('copied');
          btn.querySelector('svg').innerHTML = originalIcon;
        }, 1500);
      }).catch(err => {
        console.error('Failed to copy: ', err);
      });
    });
  });
});
</script>
