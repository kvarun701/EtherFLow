description = "Sample EtherFlow reactive web application"

plugins {
    application
}

application {
    mainClass = "io.etherflow.sample.SampleApp"
}

dependencies {
    implementation(project(":etherflow-starter-webflux"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
}
