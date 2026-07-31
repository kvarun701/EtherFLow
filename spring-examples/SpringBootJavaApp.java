// SpringBootJavaApp.java
// EtherFlow + Spring Boot — Java (Reactive Web App)
//
// Full Spring Boot application using EtherFlow as the reactive runtime.
// Java version of the Spring Boot starter example, demonstrating how to
// define RouterFunction @Beans, call Python/Node.js backends reactively,
// and use EtherFlow HttpClient with Spring's dependency injection.
//
// Dependency (pom.xml):
//   <dependency>
//     <groupId>io.github.kvarun701</groupId>
//     <artifactId>etherflow-spring-boot-starter</artifactId>
//     <version>0.1.3</version>
//     <type>pom</type>
//   </dependency>

package io.etherflow.spring.example;

import io.etherflow.client.HttpClient;
import io.etherflow.client.ParameterizedTypeReference;
import io.etherflow.client.python.PythonApiClient;
import io.etherflow.core.Mono;
import io.etherflow.web.function.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ─────────────────────────────────────────────────────────────────────────────
// Domain Models (Java Records)
// ─────────────────────────────────────────────────────────────────────────────

record Task(String id, String title, boolean completed, String priority) {}
record CreateTaskRequest(String title, String priority) {}

// ─────────────────────────────────────────────────────────────────────────────
// Service — calls external platforms reactively
// ─────────────────────────────────────────────────────────────────────────────

@Component
class ExternalApiServiceJava {

    // EtherFlow HttpClient — calls any REST API from Spring Boot
    private final HttpClient httpClient = HttpClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .retry(3)
            .cache(Duration.ofMinutes(5), 100)
            .build();

    // PythonApiClient — bridging Spring Boot → Python Flask & FastAPI
    private final PythonApiClient pythonClient = PythonApiClient.builder()
            .flaskUrl("http://localhost:5001")
            .fastApiUrl("http://localhost:5002")
            .build();

    // HttpClient for calling the Node.js Express backend
    private final HttpClient nodeClient = HttpClient.builder()
            .baseUrl("http://localhost:5005")
            .retry(2)
            .build();

    /** GET user from external API — returns Mono<Map>. */
    public Mono<Map> getExternalUser(String id) {
        return httpClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyTo(Map.class);
    }

    /** Call Python Flask backend — returns Mono<Map>. */
    public Mono<Map> callFlask(String name) {
        return pythonClient.flask()
                .get("/api/flask/hello?name=" + name, Map.class);
    }

    /** Call Python FastAPI backend — returns Mono<Map>. */
    public Mono<Map> callFastApi(int itemId) {
        return pythonClient.fastApi()
                .get("/api/fastapi/items/" + itemId, Map.class);
    }

    /** Call Node.js Express backend. */
    public Mono<Map> callNodeJs(String name) {
        return nodeClient.get()
                .uri("/api/node/hello?name=" + name)
                .retrieve()
                .bodyTo(Map.class);
    }

    /** Aggregate health across all Python backends. */
    public Mono<Map<String, Object>> checkAllHealth() {
        return pythonClient.checkHealth();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Handler
// ─────────────────────────────────────────────────────────────────────────────

@Component
class TaskHandlerJava {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public Mono<ServerResponse> listTasks(ServerRequest request) {
        return Mono.just(ServerResponse.ok(new ArrayList<>(tasks.values())));
    }

    public Mono<ServerResponse> getTask(ServerRequest request) {
        String id = request.pathVariable("id");
        Task task = tasks.get(id);
        return task != null
                ? Mono.just(ServerResponse.ok(task))
                : Mono.just(ServerResponse.notFound());
    }

    public Mono<ServerResponse> createTask(ServerRequest request) {
        return request.bodyTo(CreateTaskRequest.class).flatMap(req -> {
            Task task = new Task(
                    UUID.randomUUID().toString(),
                    req.title(),
                    false,
                    req.priority() != null ? req.priority() : "MEDIUM"
            );
            tasks.put(task.id(), task);
            return Mono.just(ServerResponse.created().body(task));
        });
    }

    public Mono<ServerResponse> deleteTask(ServerRequest request) {
        String id = request.pathVariable("id");
        return tasks.remove(id) != null
                ? Mono.just(ServerResponse.noContent())
                : Mono.just(ServerResponse.notFound());
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// External API Handler
// ─────────────────────────────────────────────────────────────────────────────

@Component
class ExternalApiHandlerJava {

    private final ExternalApiServiceJava service;

    public ExternalApiHandlerJava(ExternalApiServiceJava service) {
        this.service = service;
    }

    public Mono<ServerResponse> getExternalUser(ServerRequest req) {
        String id = req.pathVariable("id");
        return service.getExternalUser(id).map(user ->
                ServerResponse.ok(Map.of(
                        "source",       "Spring Boot Java → JSONPlaceholder via EtherFlow",
                        "externalUser", user
                ))
        );
    }

    public Mono<ServerResponse> callFlask(ServerRequest req) {
        String name = req.pathVariable("name");
        return service.callFlask(name).map(result ->
                ServerResponse.ok(Map.of(
                        "source",      "Spring Boot Java → Python Flask via PythonApiClient",
                        "flaskResult", result
                ))
        );
    }

    public Mono<ServerResponse> callFastApi(ServerRequest req) {
        int id = Integer.parseInt(req.pathVariable("id"));
        return service.callFastApi(id).map(result ->
                ServerResponse.ok(Map.of(
                        "source",        "Spring Boot Java → Python FastAPI via PythonApiClient",
                        "fastApiResult", result
                ))
        );
    }

    public Mono<ServerResponse> callNodeJs(ServerRequest req) {
        String name = req.pathVariable("name");
        return service.callNodeJs(name).map(result ->
                ServerResponse.ok(Map.of(
                        "source",       "Spring Boot Java → Node.js Express via HttpClient",
                        "nodeJsResult", result
                ))
        );
    }

    public Mono<ServerResponse> health(ServerRequest req) {
        return service.checkAllHealth().map(health ->
                ServerResponse.ok(Map.of(
                        "springBoot",      "UP",
                        "externalServices", health
                ))
        );
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Route Configuration
// ─────────────────────────────────────────────────────────────────────────────

@Configuration
class JavaRouteConfig {

    @Bean
    RouterFunction taskRoutes(TaskHandlerJava handler) {
        return RouterFunction.route()
                .GET("/api/tasks",         handler::listTasks)
                .GET("/api/tasks/{id}",    handler::getTask)
                .POST("/api/tasks",        handler::createTask)
                .DELETE("/api/tasks/{id}", handler::deleteTask)
                .build();
    }

    @Bean
    RouterFunction gatewayRoutes(ExternalApiHandlerJava handler) {
        return RouterFunction.route()
                .GET("/api/gateway/external-user/{id}", handler::getExternalUser)
                .GET("/api/gateway/flask/{name}",       handler::callFlask)
                .GET("/api/gateway/fastapi/{id}",       handler::callFastApi)
                .GET("/api/gateway/nodejs/{name}",      handler::callNodeJs)
                .GET("/api/gateway/health",             handler::health)
                .build();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Application Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@SpringBootApplication
public class SpringBootJavaApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootJavaApp.class, args);
        System.out.println("""
            ╔═══════════════════════════════════════════════════╗
            ║  EtherFlow + Spring Boot (Java) API Gateway       ║
            ║  http://localhost:8080                            ║
            ╠═══════════════════════════════════════════════════╣
            ║  Task CRUD:                                       ║
            ║    GET    /api/tasks                              ║
            ║    GET    /api/tasks/{id}                         ║
            ║    POST   /api/tasks                              ║
            ║    DELETE /api/tasks/{id}                         ║
            ╠═══════════════════════════════════════════════════╣
            ║  API Gateway (multi-platform bridge):             ║
            ║    GET /api/gateway/external-user/{id}            ║
            ║    GET /api/gateway/flask/{name}                  ║
            ║    GET /api/gateway/fastapi/{id}                  ║
            ║    GET /api/gateway/nodejs/{name}                 ║
            ║    GET /api/gateway/health                        ║
            ╚═══════════════════════════════════════════════════╝
        """);
    }
}
