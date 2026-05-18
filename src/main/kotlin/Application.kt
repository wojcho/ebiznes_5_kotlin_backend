package com.example

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.cors.routing.allowCredentials
import io.ktor.server.plugins.cors.routing.allowHeader
import io.ktor.server.plugins.cors.routing.allowHost
import io.ktor.server.plugins.cors.routing.allowMethod
import io.ktor.server.plugins.cors.routing.maxAgeInSeconds
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            },
        )
    }
    install(CORS) {
        allowHost("localhost:3000", schemes = listOf("http"))
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)

        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowCredentials = true
        maxAgeInSeconds = 24 * 60 * 60
    }
    configureRouting()
}
