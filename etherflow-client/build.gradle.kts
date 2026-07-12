plugins {
    kotlin("jvm") version "2.1.0"
}

description = "Reactive HTTP client — Mono/Flux, OkHttp transport, JSON codec, retry, caching"

dependencies {
    api(project(":etherflow-core"))
    api(project(":etherflow-codec"))
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(kotlin("stdlib"))
}

sourceSets {
    main {
        java.srcDirs("src/main/java", "src/main/kotlin")
    }
}
