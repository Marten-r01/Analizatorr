plugins {
    kotlin("jvm") version "2.3.20"
    application
    kotlin("plugin.serialization") version "2.3.20"
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.4.1"
val exposedVersion = "1.1.1"
val postgresVersion = "42.7.5"
val h2Version = "2.3.232"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    testRuntimeOnly("com.h2database:h2:$h2Version")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("analizator.ApplicationKt")
}
