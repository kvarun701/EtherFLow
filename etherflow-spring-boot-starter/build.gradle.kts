description = "Spring Boot starter for EtherFlow reactive web framework"

dependencies {
    api(project(":etherflow-spring-boot-autoconfigure"))
    api(project(":etherflow-starter-webflux"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    implementation("org.springframework.boot:spring-boot-starter")
}
