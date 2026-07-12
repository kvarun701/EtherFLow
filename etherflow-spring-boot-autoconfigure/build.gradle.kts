description = "Auto-configuration for EtherFlow in Spring Boot applications"

dependencies {
    api(project(":etherflow-server-netty"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("jakarta.annotation:jakarta.annotation-api")
}
