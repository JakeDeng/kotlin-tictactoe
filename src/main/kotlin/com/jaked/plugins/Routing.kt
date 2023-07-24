package com.jaked.plugins

import com.jaked.domain.Game
import com.jaked.socket
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(game: Game) {
    routing {
        socket(game)
    }
}
