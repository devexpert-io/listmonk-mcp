plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "io.devexpert"
version = "1.0.0"

application {
    mainClass.set("io.devexpert.listmonk.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // MCP Core
    implementation(libs.mcp.kotlin.sdk)
    
    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // IO
    implementation(libs.kotlinx.io.core)
    
    // HTTP Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    
    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging.jvm)
    
    // Configuration
    implementation(libs.kaml)
    
    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("listmonk-mcp")
    archiveVersion.set("")
    archiveClassifier.set("all")
}