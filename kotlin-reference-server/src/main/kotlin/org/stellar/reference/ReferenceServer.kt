package org.stellar.reference

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.stellar.reference.di.ConfigContainer
import org.stellar.reference.di.EventConsumerContainer
import org.stellar.reference.di.ReferenceServerContainer

val log = KotlinLogging.logger {}
lateinit var eventConsumingExecutor: ExecutorService

fun main(args: Array<String>) {
  startServer(null, args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  // read config
  ConfigContainer.init(envMap)
  eventConsumingExecutor = DaemonExecutors.newFixedThreadPool(1)
  eventConsumingExecutor.submit {
    log.info("Starting event consumer")
    runBlocking { EventConsumerContainer.eventConsumer.start() }
  }

  // start server
  log.info { "Starting Kotlin reference server" }
  ReferenceServerContainer.startServer(wait)
}

fun stopServer() {
  log.info("Stopping event consumer...")
  EventConsumerContainer.eventConsumer.stop()

  log.info("Stopping Kotlin business reference server...")
  ReferenceServerContainer.server.stop(5000, 30000)

  eventConsumingExecutor.shutdown()
  eventConsumingExecutor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)
}

class DaemonThreadFactory : ThreadFactory {
  override fun newThread(r: Runnable): Thread {
    val thread = Executors.defaultThreadFactory().newThread(r)
    thread.setDaemon(true) // Set the thread as a daemon thread
    return thread
  }
}

class DaemonExecutors {
  companion object {
    private val daemonThreadFactory: ThreadFactory = DaemonThreadFactory()

    fun newFixedThreadPool(threadCount: Int): ExecutorService {
      return Executors.newFixedThreadPool(threadCount, daemonThreadFactory)
    }
  }
}
