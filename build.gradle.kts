plugins {
    id("java-library")
    id("maven-publish")
    kotlin("jvm") version "2.1.0" apply false
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    kotlin("plugin.compose") version "2.1.0" apply false
}

allprojects {
    group = "io.github.kvarun701"
    version = "0.1.1"

    repositories {
        google()
        mavenCentral()
    }

    val isJavaProject = !project.name.contains("kmp")

    if (isJavaProject) {
        apply(plugin = "java-library")
        apply(plugin = "maven-publish")

        java {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
            withSourcesJar()
            withJavadocJar()
        }

        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        tasks.withType<Javadoc> {
            (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }
}

subprojects {
    if (project.name.contains("kmp")) return@subprojects

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set(project.description ?: project.name)
                    url.set("https://github.com/kvarun701/EtherFLow")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("kvarun701")
                            name.set("kvarun701")
                            email.set("kvarun701@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/kvarun701/EtherFLow.git")
                        developerConnection.set("scm:git:git@github.com:kvarun701/EtherFLow.git")
                        url.set("https://github.com/kvarun701/EtherFLow")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "OSSRH"
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                credentials {
                    username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

}
