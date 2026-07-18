import java.io.File

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

group = "io.github.kvarun701"
version = "0.1.0"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser {
            testTask {
                enabled = File("/Applications/Google Chrome.app").exists() || File("/Applications/Google Chrome Canary.app").exists()
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":etherflow-client-kmp"))
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
    }
}
