package org.stellar.reference

import io.ktor.server.netty.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import mu.KotlinLogging
import org.stellar.reference.di.ConfigContainer
import org.stellar.reference.di.EventConsumerContainer
import org.stellar.reference.di.ReferenceServerContainer
import org.stellar.reference.event.EventConsumer

val log = KotlinLogging.logger {}
lateinit var eventConsumingExecutor: ExecutorService

lateinit var referenceKotlinServer: NettyApplicationEngine
lateinit var eventConsumer: EventConsumer

fun main(args: Array<String>) {
  startServer(null, args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  // read config
  ConfigContainer.init(envMap)
  eventConsumingExecutor = Executors.newFixedThreadPool(1)
  eventConsumingExecutor.submit {
    log.info("Starting event consumer")
    eventConsumer = EventConsumerContainer.eventConsumer.start()
  }

  // start server
  log.info { "Starting Kotlin reference server" }
  referenceKotlinServer = ReferenceServerContainer.startServer(wait)
}

fun stopServer() {
  log.info("Stopping Kotlin business reference server...")
  if (::referenceKotlinServer.isInitialized) (referenceKotlinServer).stop(5000, 30000)
  if (::eventConsumer.isInitialized) eventConsumer.stop()
  eventConsumingExecutor.shutdown()
  eventConsumingExecutor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)

  log.info("Kotlin reference server stopped...")
}
