pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "etherflow"

include(
    "etherflow-streams",
    "etherflow-core",
    "etherflow-codec",
    "etherflow-http",
    "etherflow-web",
    "etherflow-server-netty",
    "etherflow-starter-webflux",
    "etherflow-spring-boot-autoconfigure",
    "etherflow-spring-boot-starter",
    "etherflow-sample",
    "etherflow-client",
    "etherflow-client-kmp",
    "etherflow-client-compose",
    "etherflow-sample-spring-boot",
)
