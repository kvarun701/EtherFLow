description = "Sample EtherFlow application using Spring Boot starter"

plugins {
    application
}

application {
    mainClass = "io.etherflow.sample.springboot.SpringBootSampleApp"
}

dependencies {
    implementation(project(":etherflow-spring-boot-starter"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
}
