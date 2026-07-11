package io.etherflow.sample.springboot;

import io.etherflow.core.Mono;
import io.etherflow.web.function.RouterFunction;
import io.etherflow.web.function.ServerRequest;
import io.etherflow.web.function.ServerResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootSampleApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootSampleApp.class, args);
    }

    @Bean
    public RouterFunction helloRoute() {
        return RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("Hello from EtherFlow + Spring Boot!")))
                .build();
    }

    @Bean
    public RouterFunction userRoutes() {
        return RouterFunction.route()
                .GET("/users/{id}", req -> {
                    String id = req.pathVariable("id");
                    return Mono.just(ServerResponse.ok("{\"id\":\"" + id + "\",\"name\":\"Alice\"}"));
                })
                .POST("/echo", req ->
                        req.bodyTo(String.class)
                                .flatMap(body -> Mono.just(ServerResponse.ok(body))))
                .build();
    }
}
