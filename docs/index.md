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
- **Java 21+** — Sealed classes, pattern matching, records, virtual threads compatible

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

## License

MIT
