// SpringBootApp.kt
// EtherFlow + Spring Boot — Kotlin (Reactive Web App)
//
// Full Spring Boot application using EtherFlow as the reactive runtime.
// Demonstrates how to wire EtherFlow RouterFunction beans in Spring context
// and call Python/Node.js backends reactively.
//
// Dependency (build.gradle.kts):
//   implementation("io.github.kvarun701:etherflow-spring-boot-starter:0.1.1")

package io.etherflow.spring.example

import io.etherflow.client.HttpClient
import io.etherflow.client.python.PythonApiClient
import io.etherflow.core.Mono
import io.etherflow.web.function.RouterFunction
import io.etherflow.web.function.ServerRequest
import io.etherflow.web.function.ServerResponse
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.time.Duration

// ─────────────────────────────────────────────────────────────────────────────
// Domain Models
// ─────────────────────────────────────────────────────────────────────────────

data class Task(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val priority: String = "MEDIUM"
)

data class CreateTaskRequest(
    val title: String,
    val priority: String = "MEDIUM"
)

data class ApiStatus(
    val service: String,
    val status: String,
    val message: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Service — External API Calls (Python + .NET + Node.js)
// ─────────────────────────────────────────────────────────────────────────────

@Component
class ExternalApiService {

    // EtherFlow HttpClient for calling any REST API
    private val httpClient = HttpClient.builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .retry(3)
        .cache(Duration.ofMinutes(5), 100)
        .build()

    // PythonApiClient for calling Flask (5001) + FastAPI (5002)
    private val pythonClient = PythonApiClient.builder()
        .flaskUrl("http://localhost:5001")
        .fastApiUrl("http://localhost:5002")
        .build()

    // HttpClient for calling Node.js Express (5005)
    private val nodeClient = HttpClient.builder()
        .baseUrl("http://localhost:5005")
        .retry(2)
        .build()

    /** Call JSONPlaceholder users API reactively. */
    fun getExternalUser(id: String): Mono<Map<*, *>> =
        httpClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyTo(Map::class.java)

    /** Call Flask Python backend reactively. */
    fun callFlask(name: String): Mono<Map<*, *>> =
        pythonClient.flask().get("/api/flask/hello?name=$name", Map::class.java)

    /** Call FastAPI Python backend reactively. */
    fun callFastApi(itemId: Int): Mono<Map<*, *>> =
        pythonClient.fastApi().get("/api/fastapi/items/$itemId", Map::class.java)

    /** Call Node.js Express backend reactively. */
    fun callNodeJs(name: String): Mono<Map<*, *>> =
        nodeClient.get()
            .uri("/api/node/hello?name=$name")
            .retrieve()
            .bodyTo(Map::class.java)

    /** Aggregate health of all external services. */
    fun checkAllHealth(): Mono<Map<String, Any>> =
        pythonClient.checkHealth()
}

// ─────────────────────────────────────────────────────────────────────────────
// Task Handler
// ─────────────────────────────────────────────────────────────────────────────

@Component
class TaskHandler(private val externalApiService: ExternalApiService) {

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
        request.bodyTo<CreateTaskRequest>().flatMap { req ->
            val task = Task(
                id        = java.util.UUID.randomUUID().toString(),
                title     = req.title,
                priority  = req.priority
            )
            tasks[task.id] = task
            Mono.just(ServerResponse.created().body(task))
        }

    fun deleteTask(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return if (tasks.remove(id) != null) {
            Mono.just(ServerResponse.noContent())
        } else {
            Mono.just(ServerResponse.notFound())
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// External API Handler (calls Python, Node.js, .NET)
// ─────────────────────────────────────────────────────────────────────────────

@Component
class ExternalApiHandler(private val service: ExternalApiService) {

    /** GET /api/gateway/external-user/{id} — calls JSONPlaceholder via EtherFlow. */
    fun getExternalUser(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        return service.getExternalUser(id).map { user ->
            ServerResponse.ok(mapOf(
                "source"   to "Spring Boot → JSONPlaceholder via EtherFlow",
                "externalUser" to user
            ))
        }
    }

    /** GET /api/gateway/flask/{name} — bridges Spring Boot → Python Flask. */
    fun callFlask(request: ServerRequest): Mono<ServerResponse> {
        val name = request.pathVariable("name")
        return service.callFlask(name).map { result ->
            ServerResponse.ok(mapOf(
                "source"      to "Spring Boot → Python Flask via PythonApiClient",
                "flaskResult" to result
            ))
        }
    }

    /** GET /api/gateway/fastapi/{id} — bridges Spring Boot → Python FastAPI. */
    fun callFastApi(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toIntOrNull() ?: 1
        return service.callFastApi(id).map { result ->
            ServerResponse.ok(mapOf(
                "source"        to "Spring Boot → Python FastAPI via PythonApiClient",
                "fastApiResult" to result
            ))
        }
    }

    /** GET /api/gateway/nodejs/{name} — bridges Spring Boot → Node.js Express. */
    fun callNodeJs(request: ServerRequest): Mono<ServerResponse> {
        val name = request.pathVariable("name")
        return service.callNodeJs(name).map { result ->
            ServerResponse.ok(mapOf(
                "source"       to "Spring Boot → Node.js Express via HttpClient",
                "nodeJsResult" to result
            ))
        }
    }

    /** GET /api/gateway/health — aggregate health check for all platforms. */
    fun health(request: ServerRequest): Mono<ServerResponse> =
        service.checkAllHealth().map { health ->
            ServerResponse.ok(mapOf(
                "springBoot" to "UP",
                "externalServices" to health
            ))
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Route Configuration (EtherFlow RouterFunction @Beans)
// ─────────────────────────────────────────────────────────────────────────────

@Configuration
class RouteConfig {

    /** Task CRUD routes. */
    @Bean
    fun taskRoutes(handler: TaskHandler): RouterFunction = RouterFunction.route()
        .GET("/api/tasks")             { handler.listTasks(it) }
        .GET("/api/tasks/{id}")        { handler.getTask(it) }
        .POST("/api/tasks")            { handler.createTask(it) }
        .DELETE("/api/tasks/{id}")     { handler.deleteTask(it) }
        .build()

    /** API Gateway routes — bridges to Python, Node.js, etc. */
    @Bean
    fun gatewayRoutes(handler: ExternalApiHandler): RouterFunction = RouterFunction.route()
        .GET("/api/gateway/external-user/{id}") { handler.getExternalUser(it) }
        .GET("/api/gateway/flask/{name}")        { handler.callFlask(it) }
        .GET("/api/gateway/fastapi/{id}")        { handler.callFastApi(it) }
        .GET("/api/gateway/nodejs/{name}")       { handler.callNodeJs(it) }
        .GET("/api/gateway/health")              { handler.health(it) }
        .build()
}

// ─────────────────────────────────────────────────────────────────────────────
// Application Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@SpringBootApplication
class EtherFlowSpringBootApp

fun main(args: Array<String>) {
    runApplication<EtherFlowSpringBootApp>(*args)
    println("""
        ╔═══════════════════════════════════════════════════╗
        ║  EtherFlow + Spring Boot API Gateway              ║
        ║  http://localhost:8080                            ║
        ╠═══════════════════════════════════════════════════╣
        ║  Task CRUD:                                       ║
        ║    GET    /api/tasks                              ║
        ║    GET    /api/tasks/{id}                         ║
        ║    POST   /api/tasks                              ║
        ║    DELETE /api/tasks/{id}                         ║
        ╠═══════════════════════════════════════════════════╣
        ║  API Gateway (calls Python, Node.js, etc.):       ║
        ║    GET /api/gateway/external-user/{id}            ║
        ║    GET /api/gateway/flask/{name}                  ║
        ║    GET /api/gateway/fastapi/{id}                  ║
        ║    GET /api/gateway/nodejs/{name}                 ║
        ║    GET /api/gateway/health                        ║
        ╚═══════════════════════════════════════════════════╝
    """.trimIndent())
}
