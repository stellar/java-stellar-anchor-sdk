package com.example

import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import mu.KotlinLogging

val log = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun start() = log.info { "Starting Kotlin reference server" }.also { main(emptyArray()) }

@Suppress(
  "unused"
) // application.conf references the main function. This annotation prevents the IDE from marking it
// as unused.
fun Application.module() {
  configureRouting()
}

fun Application.configureRouting() {
  routing { sep24() }
}
