plugins {
    kotlin("multiplatform") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "io.etherflow"
version = "0.1.0"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":etherflow-client"))
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
