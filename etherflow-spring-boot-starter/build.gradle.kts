description = "Spring Boot starter for EtherFlow reactive web framework"

dependencies {
    api(project(":etherflow-spring-boot-autoconfigure"))
    api(project(":etherflow-starter-webflux"))
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    api("org.springframework.boot:spring-boot-starter")
}
