package org.stellar.admin.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.stellar.admin.server.plugins.*

const val APAS_CONFIG_STORE = "APAS_CONFIG_STORE"
const val APAS_HOST = "APAS_HOST"
const val APAS_PORT = "APAS_PORT"
const val APAS_AUTH = "APAS_AUTH"

/**
 * Start the admin server.
 *
 * The following environment variables can be defined to change the behavior of the server.
 *
 *      APAS_CONFIG_STORE: The location of the configuration store. in MVP, only postgres string is supported.
 *          (optional) default to: postgresql://localhost/apas?user=apas&password=password&ssl=true
 *      APAS_HOST: The host/IP of the AP to bind.
 *          (optional) default to: localhost
 *      APAS_PORT: The port of the AP to bind.
 *          (optional) default to: 8000
 *      APAS_AUTH: The authentication mechanism.
 *          (optional) default to: none
 *
 * @param envMap the environment variables.
 * @param wait if the server should wait for the server to stop.
 */
fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  val bindHost = envMap?.get(APAS_HOST) ?: "0.0.0.0"
  val bindPort = (envMap?.get(APAS_PORT) ?: "8000").toInt()
  val auth = envMap?.get(APAS_AUTH) ?: "none"
  val configStore =
    envMap?.get(APAS_CONFIG_STORE)
      ?: "postgresql://localhost/apas?user=apas&password=password&ssl=true"

  embeddedServer(Netty, port = bindPort, host = bindHost) {
      configureHTTP()
      configureMonitoring()
      configureSerialization()
      configureAdministration()
      configureRouting()
      configureStaticFilesRouting()
    }
    .start(wait = true)
}

fun main(args: Array<String>) {
  val envMap = mutableMapOf<String, String>()
  System.getenv()[APAS_CONFIG_STORE]?.let { envMap[APAS_CONFIG_STORE] = it }
  System.getenv()[APAS_HOST]?.let { envMap[APAS_HOST] = it }
  System.getenv()[APAS_PORT]?.let { envMap[APAS_PORT] = it }
  System.getenv()[APAS_AUTH]?.let { envMap[APAS_AUTH] = it }

  startServer(envMap, true)
}
