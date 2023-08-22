package org.stellar.reference.wallet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import shadow.com.moandjiezana.toml.Toml

fun fetchSigningKey(config: Config): String = runBlocking {
  val endpoint = Url(config.anchor.endpoint)
  val client = HttpClient()

  val response =
    client.get {
      url {
        this.protocol = endpoint.protocol
        host = endpoint.host
        port = endpoint.port
        encodedPath = "/.well-known/stellar.toml"
      }
    }
  return@runBlocking Toml().read(response.body<String>()).getString("SIGNING_KEY")
}
