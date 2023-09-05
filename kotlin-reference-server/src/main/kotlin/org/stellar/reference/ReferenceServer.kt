package org.stellar.reference

import com.sksamuel.hoplite.*
import io.ktor.server.netty.*
import mu.KotlinLogging
import org.stellar.reference.di.ConfigContainer
import org.stellar.reference.di.EventConsumerContainer
import org.stellar.reference.di.ReferenceServerContainer

val log = KotlinLogging.logger {}
lateinit var referenceKotlinServer: NettyApplicationEngine

fun main(args: Array<String>) {
  startServer(null, args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  // read config
  ConfigContainer.init(envMap)

  Thread {
      log.info("Starting event consumer")
      EventConsumerContainer.eventConsumer.start()
    }
    .start()

  // start server
  log.info { "Starting Kotlin reference server" }
  referenceKotlinServer = ReferenceServerContainer.server.start(wait)
}

fun stopServer() {
  log.info("Stopping Kotlin business reference server...")
  if (::referenceKotlinServer.isInitialized) (referenceKotlinServer).stop(5000, 30000)
  log.info("Kotlin reference server stopped...")
}
