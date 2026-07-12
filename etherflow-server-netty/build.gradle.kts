description = "Netty server adapter for EtherFlow"

dependencies {
    api(project(":etherflow-web"))
    implementation("io.netty:netty-all:4.1.117.Final")
}
