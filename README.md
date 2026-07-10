# EtherFlow

**A lightweight reactive web framework for Java — zero Spring dependency.**

EtherFlow is a from-scratch implementation of a reactive web framework inspired by Spring WebFlux. It gives you `Mono`/`Flux` reactive types, a `RouterFunction` DSL for HTTP endpoints, JSON serialization via Jackson, and a Netty server adapter — all without pulling in the Spring Framework.

---

## Features

- **Reactive Streams SPI** — `Publisher`, `Subscriber`, `Subscription`, `Processor` interfaces
- **Mono / Flux** — Asynchronous reactive types with rich operators: `map`, `flatMap`, `filter`, `switchIfEmpty`, `then`, `thenReturn`, `subscribeOn`, `publishOn`, `block`, `defer`, `fromCallable`
- **Schedulers** — `parallel()`, `single()`, `boundedElastic()`, `timer()`, `immediate()`
- **Functional Endpoints** — `RouterFunction` builder + `HandlerFunction` + `RequestPredicate` DSL
- **JSON Serialization / Deserialization** — Jackson-based `HttpMessageReader`/`Writer`
- **Front Controller** — `DispatcherHandler` with pluggable `HandlerMapping` / `HandlerAdapter`
- **Filter Chain** — `WebFilter` + `WebExceptionHandler` pipeline
- **Netty Server Adapter** — Run on Netty with a single entry point
- **Zero Spring Dependency** — No ApplicationContext, no autoconfiguration, no XML — just pure Java
- **Java 21+** — Sealed classes, pattern matching, records, virtual threads compatible

---

## Why EtherFlow over Spring WebFlux?

| Aspect | Spring WebFlux | EtherFlow |
|--------|---------------|-----------|
| **Dependencies** | Spring Context, Spring AOP, Spring Beans, Spring Web, Reactor Core, Reactor Netty, Jackson — hundreds of classes | Only Jackson + Netty |
| **Startup time** | 2-5 seconds (context scanning) | < 100ms |
| **JAR size** | ~15-20 MB | ~2 MB |
| **Complexity** | Deep class hierarchy, proxy-based AOP, 15+ annotations | Minimal interfaces, no proxies, no reflection magic |
| **Learning curve** | Must understand DI, AOP, Bean lifecycle, autoconfiguration | Just Mono/Flux and RouterFunction |
| **Debugging** | Proxy chains, nested contexts, complex stack traces | Straightforward call stacks |
| **Control** | Framework dictates architecture via IoC | You control every component manually or via simple composition |
| **Virtual threads** | Works but adds another layer of complexity | Clean — just use `Schedulers.immediate()` on virtual threads |

### When to pick EtherFlow instead of WebFlux

- You want a **minimal, understandable** reactive stack
- You're building a **gateway, proxy, or embedded server** where startup time and footprint matter
- You want **full control** without framework magic
- You're **learning** reactive programming and want to see how the pieces fit
- You need reactive HTTP for a **CLI tool, agent, or edge service**

### When to stick with Spring WebFlux

- You need the **whole Spring ecosystem** (Security, Data R2DBC, Cloud, Actuator)
- Your team is already deeply invested in Spring patterns (DI, AOP, autoconfiguration)
- You need battle-tested enterprise features (distributed tracing, bulkhead, circuit breaker)

---

## Architecture

```
┌─────────────────────────────────────────────┐
│  Functional Endpoints                        │
│  RouterFunction → HandlerFunction            │
│  RequestPredicate (path, method, headers)    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  DispatcherHandler (Front Controller)        │
│  HandlerMapping → HandlerAdapter             │
│  WebFilter → WebExceptionHandler             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  HttpHandler (Server Abstraction)            │
│  ServerWebExchange                           │
│  ServerHttpRequest / ServerHttpResponse      │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Codec Layer                                 │
│  HttpMessageReader / HttpMessageWriter       │
│  JacksonCodec (JSON)                         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Reactive Core                               │
│  Mono<T> / Flux<T> + Schedulers              │
│  Reactive Streams SPI (Publisher/Subscriber) │
└─────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Add the dependency

```xml
<repositories>
    <repository>
        <id>etherflow-snapshots</id>
        <url>https://github.com/kvarun701/EtherFlow/raw/main/maven-repo</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.etherflow</groupId>
    <artifactId>etherflow-starter-webflux</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <type>pom</type>
</dependency>
```

Or clone and build locally:

```bash
git clone https://github.com/kvarun701/EtherFlow.git
cd EtherFlow
mvn install -DskipTests
```

Then add:

```xml
<dependency>
    <groupId>io.etherflow</groupId>
    <artifactId>etherflow-starter-webflux</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <type>pom</type>
</dependency>
```

### 2. Create a handler

```java
import io.etherflow.core.Mono;
import io.etherflow.web.function.*;

public class UserHandler {

    public Mono<ServerResponse> getUser(ServerRequest request) {
        String id = request.pathVariable("id");
        return Mono.just(ServerResponse.ok("{\"id\":\"" + id + "\",\"name\":\"Alice\"}"));
    }

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        return Mono.just(ServerResponse.ok("[{\"id\":\"1\",\"name\":\"Alice\"}]"));
    }

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyTo(String.class)
                .flatMap(body -> Mono.just(ServerResponse.created()));
    }
}
```

### 3. Define routes

```java
import io.etherflow.web.function.*;
import static io.etherflow.web.function.RequestPredicate.*;

UserHandler handler = new UserHandler();

RouterFunction routes = RouterFunction.route()
        .GET("/users/{id}", handler::getUser)
        .GET("/users", handler::listUsers)
        .POST("/users", handler::createUser)
        .build();
```

### 4. Wire it up

```java
import io.etherflow.http.*;
import io.etherflow.web.*;

DispatcherHandler dispatcher = new DispatcherHandler();
RouterFunctionMapping mapping = new RouterFunctionMapping(routes);

dispatcher.addHandlerMapping(mapping);
dispatcher.addHandlerAdapter(mapping);

// Use with any HttpHandler consumer (Netty adapter, test harness, etc.)
HttpHandler app = dispatcher;
```

### 5. Full sample

```java
package com.example;

import io.etherflow.core.Mono;
import io.etherflow.http.*;
import io.etherflow.web.*;
import io.etherflow.web.function.*;

public class App implements HttpHandler {

    private final DispatcherHandler handler = new DispatcherHandler();

    public App() {
        RouterFunction routes = RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("Hello EtherFlow!")))
                .GET("/users/{id}", req -> {
                    String id = req.pathVariable("id");
                    return Mono.just(ServerResponse.ok("{\"id\":\"" + id + "\"}"));
                })
                .POST("/echo", req ->
                        req.bodyTo(String.class)
                                .flatMap(body -> Mono.just(ServerResponse.ok(body))))
                .build();

        RouterFunctionMapping mapping = new RouterFunctionMapping(routes);
        handler.addHandlerMapping(mapping);
        handler.addHandlerAdapter(mapping);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        return handler.handle(exchange);
    }

    public static void main(String[] args) {
        System.out.println("App ready — endpoints:");
        System.out.println("  GET  /hello");
        System.out.println("  GET  /users/{id}");
        System.out.println("  POST /echo");
    }
}
```

---

## Modules

| Module | Description |
|--------|-------------|
| `etherflow-streams` | Reactive Streams SPI: `Publisher`, `Subscriber`, `Subscription`, `Processor` |
| `etherflow-core` | `Mono<T>`, `Flux<T>`, `Schedulers` — core reactive types with operators |
| `etherflow-codec` | `DataBuffer`, `HttpMessageReader`/`Writer`, `JacksonCodec` for JSON ser/des |
| `etherflow-http` | `HttpHandler`, `ServerWebExchange`, `ServerHttpRequest`/`Response`, `WebFilter`, `WebExceptionHandler` |
| `etherflow-web` | `DispatcherHandler`, `HandlerMapping`, `HandlerAdapter`, `RouterFunction`, `HandlerFunction`, `RequestPredicate`, `ServerRequest`/`Response` |
| `etherflow-server-netty` | Netty server adapter |
| `etherflow-starter-webflux` | Meta-pom — one dependency to pull in all modules |

---

## Operator Reference

### Mono

| Method | Description |
|--------|-------------|
| `just(T)` | Emit a single value |
| `empty()` | Complete without emitting |
| `error(Throwable)` | Emit an error |
| `fromCallable(Callable)` | Lazily produce a value |
| `defer(Supplier)` | Lazily create the Mono per subscription |
| `map(Function)` | Transform the value |
| `flatMap(Function)` | Transform into another Mono |
| `filter(Predicate)` | Only pass if predicate matches |
| `doOnSuccess(Consumer)` | Side-effect on success |
| `doOnError(Consumer)` | Side-effect on error |
| `switchIfEmpty(Supplier)` | Fallback Mono if source is empty |
| `then()` | Wait for completion, discard value |
| `thenReturn(R)` | Wait for completion, emit fixed value |
| `subscribeOn(Executor)` | Run subscription on given executor |
| `publishOn(Executor)` | Run downstream on given executor |
| `block()` | Block until value or error (for testing) |

### Flux

| Method | Description |
|--------|-------------|
| `just(T...)` | Emit multiple values |
| `fromIterable(Iterable)` | Emit from iterable |
| `range(int, int)` | Emit a range of integers |
| `empty()` | Complete without emitting |
| `error(Throwable)` | Emit an error |
| `map(Function)` | Transform each element |
| `flatMap(Function)` | Transform each into a Publisher and merge |
| `filter(Predicate)` | Only pass elements matching predicate |
| `subscribeOn(Executor)` | Run subscription on given executor |
| `publishOn(Executor)` | Run downstream on given executor |
| `then()` | Return Mono<Void> on completion |

---

## Building from Source

```bash
git clone https://github.com/kvarun701/EtherFlow.git
cd EtherFlow
mvn compile           # build
mvn test              # run tests (27+ tests)
mvn install -DskipTests  # install to local .m2
```

Requires: **Java 21+**, **Apache Maven 3.8+**

---

## Roadmap

- [x] Reactive Streams SPI
- [x] Mono / Flux with core operators
- [x] JSON serialization via Jackson
- [x] Functional endpoint DSL (RouterFunction)
- [x] DispatcherHandler with filter chain
- [x] Netty server adapter
- [ ] Annotated controllers (`@Controller`, `@RequestMapping`)
- [ ] WebClient (reactive HTTP client)
- [ ] More Flux operators (`merge`, `zip`, `concatMap`, `retry`, `timeout`)
- [ ] Customizable error handling
- [ ] Multipart support
- [ ] Server-Sent Events (SSE)
- [ ] WebSocket support
- [ ] Micrometer metrics integration
- [ ] GraalVM native-image support

---

## License

MIT
