---
title: EtherFlow
description: A lightweight reactive web framework for Java — zero Spring dependency
---

# EtherFlow

A lightweight reactive web framework for Java built from scratch — zero Spring dependency.

[GitHub](https://github.com/kvarun701/EtherFLow) | [API Docs](api)

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

## Comparison with Spring WebFlux

| Aspect | Spring WebFlux | EtherFlow |
|---|---|---|
| Dependencies | Spring Context, AOP, Beans, Web, Reactor Core, Reactor Netty, Jackson — hundreds of classes | Only Jackson + Netty |
| Startup time | 2-5 seconds (context scanning) | < 100ms |
| JAR size | ~15-20 MB | ~2 MB |
| Complexity | Deep hierarchy, proxies, 15+ annotations | Minimal interfaces, no proxies |
| Learning curve | Must understand DI, AOP, Bean lifecycle | Just Mono/Flux and RouterFunction |

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

### Kotlin + KMP Client

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.etherflow:etherflow-client-kmp:0.1.0")
}
```

```kotlin
import io.etherflow.client.kmp.*
import io.etherflow.client.kmp.internal.platformEngine

val client = httpClient {
    baseUrl = "https://api.example.com"
    retryCount = 2
}.apply { install(platformEngine()) }

// JSON
val user: User = client.get("/users/1").bodyAs<User>()

// Multipart upload
client.post("/upload").multipart {
    field("user", "bob")
    file("avatar", "photo.jpg", bytes, "image/jpeg")
}.body()

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

### KMP targets & features

| Target | Engine | JSON | Multipart | Download | Streaming | WebSocket |
|--------|--------|------|-----------|----------|-----------|-----------|
| JVM / Android | OkHttp | ✅ | ✅ | ✅ | ✅ 8 KB | ✅ |
| iOS | NSURLSession | ✅ | ✅ | ✅ | ⏳ | ✅ |
| JS Browser | `window.fetch` | ✅ | ✅ | ❌ | ✅ | ✅ |

### Modules

| Module | Description |
|--------|-------------|
| `etherflow-core` | Mono/Flux, Schedulers — reactive types |
| `etherflow-web` | RouterFunction DSL, DispatcherHandler |
| `etherflow-server-netty` | Netty server adapter |
| `etherflow-client-kmp` | KMP HTTP client (multiplatform) |

## License

MIT
