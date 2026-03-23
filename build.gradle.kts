plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("analizator.Week2ConsoleApp")
}
