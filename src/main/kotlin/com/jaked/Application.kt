package com.jaked

import com.jaked.domain.Game
import com.jaked.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8081, host = "localhost", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val game = Game()
    configureMonitoring()
    configureSerialization()
    configureSockets()
    configureRouting(game)
}
