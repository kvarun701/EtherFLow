---
title: "Building a Reactive Web Framework from Scratch in Java"
description: "How I built EtherFlow — a WebFlux-like framework with zero Spring dependencies"
---

# Building a Reactive Web Framework from Scratch in Java

Spring WebFlux is powerful, but it pulls in hundreds of dependencies. I wanted to understand exactly how reactive HTTP works, so I built **EtherFlow** — a production-quality reactive web framework with zero Spring dependencies.

Here's what I learned.

## The Stack

EtherFlow is a 8-module Maven project with only **two external dependencies**: Jackson for JSON and Netty for the server:

```
etherflow-streams    → Reactive Streams SPI (Publisher/Subscriber/Subscription/Processor)
etherflow-core       → Mono/Flux reactive types + Schedulers
etherflow-codec      → DataBuffer, HttpMessageReader/Writer, JacksonCodec
etherflow-http       → HttpHandler, ServerWebExchange, ServerHttpRequest/Response, WebFilter
etherflow-web        → DispatcherHandler, RouterFunction DSL, HandlerMapping/Adapter
etherflow-server-netty → Netty server adapter
etherflow-starter-webflux → Meta-pom (single dependency to rule them all)
etherflow-sample     → Runnable sample app
```

## Why Build This?

1. **Understanding** — Using Spring WebFlux is easy; implementing reactive streams yourself teaches you every edge case
2. **Size** — Spring WebFlux is ~15-20 MB; EtherFlow is ~2 MB
3. **Startup** — Spring takes 2-5 seconds to scan context; EtherFlow starts in <100ms
4. **Control** — No proxies, no AOP, no reflection magic. Just plain Java interfaces

## Key Design Decisions

### 1. Mono/Flux Without Reactor

Project Reactor is the standard, but it's complex. I implemented `Mono<T>` and `Flux<T>` using the Reactive Streams SPI directly. Each operator is a nested class that wraps the upstream publisher:

```java
public final <R> Mono<R> map(Function<? super T, ? extends R> mapper) {
    return new MonoMap<>(this, mapper);
}
```

### 2. RouterFunction DSL

The functional endpoint DSL uses a builder pattern with method-chaining:

```java
RouterFunction routes = RouterFunction.route()
    .GET("/users/{id}", handler::getUser)
    .POST("/users", handler::createUser)
    .build();
```

Each route is matched by a `RequestPredicate` that checks method, path pattern, and headers.

### 3. Netty Bridge

The Netty adapter converts Netty's `FullHttpRequest` into EtherFlow's `ServerHttpRequest`, processes it through the `HttpHandler` chain, and writes responses back via Netty's channel pipeline.

### 4. DispatcherHandler

The front controller pattern is simple yet powerful:

```
WebFilter 1 → WebFilter 2 → HandlerMapping → HandlerAdapter → ServerResponse.writeTo()
```

No Spring ApplicationContext — just a `CopyOnWriteArrayList` of mappings and adapters.

## Performance

Preliminary benchmarks show EtherFlow handles ~30K req/s on a simple "Hello World" endpoint on a MacBook Pro — comparable to raw Netty, with significantly less overhead than Spring WebFlux.

## Try It Yourself

```bash
git clone https://github.com/kvarun701/EtherFLow.git
cd EtherFlow
mvn install -DskipTests
mvn exec:java -pl etherflow-sample
# Server running on http://localhost:8080
```

## What's Next

- Annotated controllers (@Controller, @RequestMapping)
- WebClient (reactive HTTP client)
- More Flux operators (merge, zip, retry, timeout)
- SSE and WebSocket support
- GraalVM native-image support

---

*Built with Java 21, Netty, and Jackson — zero Spring.*
