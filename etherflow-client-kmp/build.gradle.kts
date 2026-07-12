plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "io.etherflow"
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
        browser()
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

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val jsMain by getting {
            dependsOn(commonMain)
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
