plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.sonarqube") version "7.3.0.8198"
}

group = "com.example"
version = "0.0.1"
val ktorVersion = "3.4.2"
val jsonVersion = "1.11.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$jsonVersion")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

jib {
    to {
        image = "localhost/shop-backend"
        tags = setOf("latest")
    }
}

sonar {
    properties {
        property("sonar.projectKey", "wojcho_ebiznes_5_kotlin_backend")
        property("sonar.organization", "wojcho")
    }
}

dependencyLocking {
    lockAllConfigurations()
}
