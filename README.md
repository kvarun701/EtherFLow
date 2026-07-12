# EtherFlow

**A lightweight reactive web framework for Java and Kotlin — zero Spring dependency.**

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
- **Zero Spring Dependency** — No ApplicationContext, no autoconfiguration, no XML — just pure Java/Kotlin
- **Java 21+ & Kotlin** — Sealed classes, pattern matching, records, data classes, reified generics

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
| **Kotlin** | Works but reactive types clash with coroutines | Pure Java types — no coroutine conflicts |

### When to pick EtherFlow instead of WebFlux

- You want a **minimal, understandable** reactive stack
- You're building a **gateway, proxy, or embedded server** where startup time and footprint matter
- You want **full control** without framework magic
- You're **learning** reactive programming and want to see how the pieces fit
- You need reactive HTTP for a **CLI tool, agent, or edge service**
- You want a **Kotlin-friendly** reactive stack without coroutine complexity

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

### Build from source

**Maven:**
```bash
git clone https://github.com/kvarun701/EtherFLow.git
cd EtherFlow
mvn install -DskipTests
```

**Gradle:**
```bash
git clone https://github.com/kvarun701/EtherFLow.git
cd EtherFlow
./gradlew jar
```

### Add the dependency

**Maven:**
```xml
<dependency>
    <groupId>io.etherflow</groupId>
    <artifactId>etherflow-starter-webflux</artifactId>
    <version>0.1.0</version>
    <type>pom</type>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.etherflow:etherflow-starter-webflux:0.1.0")
```

**Gradle (Groovy DSL):**
```groovy
implementation 'io.etherflow:etherflow-starter-webflux:0.1.0'
```

### Run the sample

**Maven:**
```bash
mvn exec:java -pl etherflow-sample
mvn package -pl etherflow-sample -DskipTests
java -jar etherflow-sample/target/etherflow-sample-0.1.0.jar
```

**Gradle:**
```bash
./gradlew :etherflow-sample:run
./gradlew :etherflow-sample:jar
java -jar etherflow-sample/build/libs/etherflow-sample-0.1.0.jar
```

### Hello World in 30 seconds

**Java:**
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

**Kotlin:**
```kotlin
import io.etherflow.core.Mono
import io.etherflow.server.netty.NettyServer
import io.etherflow.web.DispatcherHandler
import io.etherflow.web.function.*

fun main() {
    val routes = RouterFunction.route()
        .GET("/hello") { Mono.just(ServerResponse.ok("Hello EtherFlow!")) }
        .build()

    val dispatcher = DispatcherHandler()
    dispatcher.addHandlerMapping(RouterFunctionMapping(routes))
    dispatcher.addHandlerAdapter(RouterFunctionMapping(routes))

    val server = NettyServer(8080, dispatcher)
    server.start()
    println("Server running on http://localhost:8080")
    server.await()
}
```

---

## Building a REST API with Kotlin

EtherFlow works seamlessly with Kotlin's idiomatic features: data classes, lambda syntax, reified generics, and extension functions.

### Domain model with data classes

```kotlin
@JvmInline
value class TaskId(val value: String)

data class Task(
    val id: TaskId,
    val title: String,
    val completed: Boolean = false
)
```

### Handler with idiomatic Kotlin

```kotlin
import io.etherflow.core.Mono
import io.etherflow.web.function.*

class TaskHandler {
    private val tasks = mutableMapOf<String, Task>()

    fun listTasks(request: ServerRequest): Mono<ServerResponse> =
        Mono.just(ServerResponse.ok(tasks.values.toList()))

    fun getTask(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return tasks[id]?.let {
            Mono.just(ServerResponse.ok(it))
        } ?: Mono.just(ServerResponse.notFound())
    }

    fun createTask(request: ServerRequest): Mono<ServerResponse> =
        request.bodyTo<Task>().flatMap { task ->
            tasks[task.id.value] = task
            Mono.just(ServerResponse.created().body(task))
        }
}
```

The `bodyTo<T>()` call uses a Kotlin inline reified extension — no class token needed.

### Route definition with trailing lambda

```kotlin
val routes = RouterFunction.route()
    .GET("/tasks") { handler.listTasks(it) }
    .GET("/tasks/{id}") { handler.getTask(it) }
    .POST("/tasks") { handler.createTask(it) }
    .build()
```

### Full runnable Kotlin app

```kotlin
import io.etherflow.core.Mono
import io.etherflow.server.netty.NettyServer
import io.etherflow.web.DispatcherHandler
import io.etherflow.web.function.*

data class Task(val id: String, val title: String, val completed: Boolean = false)

fun main() {
    val tasks = mutableMapOf<String, Task>()

    val routes = RouterFunction.route()
        .GET("/tasks") { Mono.just(ServerResponse.ok(tasks.values.toList())) }
        .GET("/tasks/{id}") { req ->
            val task = tasks[req.pathVariable("id")]
            if (task != null) Mono.just(ServerResponse.ok(task))
            else Mono.just(ServerResponse.notFound())
        }
        .POST("/tasks") { req ->
            req.bodyTo<Task>().flatMap { task ->
                tasks[task.id] = task
                Mono.just(ServerResponse.created().body(task))
            }
        }
        .build()

    val dispatcher = DispatcherHandler()
    dispatcher.addHandlerMapping(RouterFunctionMapping(routes))
    dispatcher.addHandlerAdapter(RouterFunctionMapping(routes))

    val server = NettyServer(8080, dispatcher)
    server.start()
    println("Task API running on http://localhost:8080")
    server.await()
}
```

---

## Kotlin Spring Boot Starter

Use EtherFlow as your reactive web runtime in a Kotlin Spring Boot application.

### Add the dependency

**Maven:**
```xml
<dependency>
    <groupId>io.etherflow</groupId>
    <artifactId>etherflow-spring-boot-starter</artifactId>
    <version>0.1.0</version>
    <type>pom</type>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.etherflow:etherflow-spring-boot-starter:0.1.0")
```

**Gradle (Groovy DSL):**
```groovy
implementation 'io.etherflow:etherflow-spring-boot-starter:0.1.0'
```

### Full Kotlin Spring Boot app

```kotlin
package com.example

import io.etherflow.core.Mono
import io.etherflow.web.function.RouterFunction
import io.etherflow.web.function.ServerResponse
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TaskApplication {

    @Bean
    fun taskRoutes(): RouterFunction = RouterFunction.route()
        .GET("/hello") { Mono.just(ServerResponse.ok("Hello from Kotlin + EtherFlow!")) }
        .build()
}

fun main(args: Array<String>) {
    runApplication<TaskApplication>(*args)
}
```

### Route definitions with `@Bean` functions

```kotlin
@Bean
fun userRoutes(handler: UserHandler): RouterFunction = RouterFunction.route()
    .GET("/users") { handler.listUsers() }
    .GET("/users/{id}") { handler.getUser(it) }
    .POST("/users") { handler.createUser(it) }
    .build()

@Bean
fun taskRoutes(handler: TaskHandler): RouterFunction = RouterFunction.route()
    .GET("/tasks") { handler.listTasks() }
    .POST("/tasks") { handler.createTask(it) }
    .build()
```

### Configuration

```properties
# application.properties
etherflow.port=8080
```

EtherFlow auto-configuration collects all `RouterFunction` beans, wires them into a `DispatcherHandler`, and starts the Netty server — zero manual setup.

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

| Method | Description | Kotlin |
|--------|-------------|--------|
| `just(T)` | Emit a single value | `Mono.just(value)` |
| `empty()` | Complete without emitting | `Mono.empty()` |
| `error(Throwable)` | Emit an error | `Mono.error(e)` |
| `fromCallable(Callable)` | Lazily produce a value | `Mono.fromCallable { compute() }` |
| `defer(Supplier)` | Lazily create the Mono per subscription | `Mono.defer { createMono() }` |
| `map(Function)` | Transform the value | `.map { it.uppercase() }` |
| `flatMap(Function)` | Transform into another Mono | `.flatMap { fetch(it) }` |
| `filter(Predicate)` | Only pass if predicate matches | `.filter { it > 0 }` |
| `doOnSuccess(Consumer)` | Side-effect on success | `.doOnSuccess { log(it) }` |
| `doOnError(Consumer)` | Side-effect on error | `.doOnError { log(it) }` |
| `switchIfEmpty(Supplier)` | Fallback Mono if source is empty | `.switchIfEmpty { fallback() }` |
| `then()` | Wait for completion, discard value | `.then()` |
| `thenReturn(R)` | Wait for completion, emit fixed value | `.thenReturn(result)` |
| `subscribeOn(Executor)` | Run subscription on given executor | `.subscribeOn(scheduler)` |
| `publishOn(Executor)` | Run downstream on given executor | `.publishOn(scheduler)` |
| `block()` | Block until value or error (for testing) | `.block()` |

### Flux

| Method | Description | Kotlin |
|--------|-------------|--------|
| `just(T...)` | Emit multiple values | `Flux.just(a, b, c)` |
| `fromIterable(Iterable)` | Emit from iterable | `Flux.fromIterable(list)` |
| `range(int, int)` | Emit a range of integers | `Flux.range(1, 10)` |
| `empty()` | Complete without emitting | `Flux.empty()` |
| `error(Throwable)` | Emit an error | `Flux.error(e)` |
| `map(Function)` | Transform each element | `.map { it * 2 }` |
| `flatMap(Function)` | Transform each into a Publisher and merge | `.flatMap { fetchMany(it) }` |
| `filter(Predicate)` | Only pass elements matching predicate | `.filter { it > 5 }` |
| `subscribeOn(Executor)` | Run subscription on given executor | `.subscribeOn(scheduler)` |
| `publishOn(Executor)` | Run downstream on given executor | `.publishOn(scheduler)` |
| `then()` | Return Mono<Void> on completion | `.then()` |

---

## Building from Source

### Maven

```bash
git clone https://github.com/kvarun701/EtherFlow.git
cd EtherFlow
mvn compile                # build
mvn test                   # run tests (27+ tests)
mvn install -DskipTests    # install to local .m2
```

### Gradle

```bash
git clone https://github.com/kvarun701/EtherFlow.git
cd EtherFlow
./gradlew compileJava      # build
./gradlew test             # run tests
./gradlew jar              # build all jars
```

Requires: **Java 21+**, **Apache Maven 3.8+** (for Maven build) or **Gradle 8.12+** (for Gradle build, wrapper included)

---

## Roadmap

- [x] Reactive Streams SPI
- [x] Mono / Flux with core operators
- [x] JSON serialization via Jackson
- [x] Functional endpoint DSL (RouterFunction)
- [x] DispatcherHandler with filter chain
- [x] Netty server adapter
- [x] Kotlin DSL support (data classes, reified generics, lambdas)
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
