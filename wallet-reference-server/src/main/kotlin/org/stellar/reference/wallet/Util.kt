package org.stellar.reference.wallet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import shadow.com.moandjiezana.toml.Toml

class ClientException(message: String) : Exception(message)

suspend fun fetchSigningKey(config: Config): String {
  val endpoint = Url(config.anchor)
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
  return Toml().read(response.body<String>()).getString("SIGNING_KEY")
}
