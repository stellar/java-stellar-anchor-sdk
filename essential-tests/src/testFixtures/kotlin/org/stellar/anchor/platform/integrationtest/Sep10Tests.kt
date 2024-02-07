package org.stellar.anchor.platform.integrationtest

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.*
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.client.Sep10Client
import org.stellar.anchor.platform.*

class Sep10Tests : AbstractIntegrationTests(TestConfig()) {
  lateinit var sep10Client: Sep10Client
  lateinit var sep10ClientMultiSig: Sep10Client

  init {
    if (!::sep10Client.isInitialized) {
      sep10Client =
        Sep10Client(
          toml.getString("WEB_AUTH_ENDPOINT"),
          toml.getString("SIGNING_KEY"),
          CLIENT_WALLET_ACCOUNT,
          CLIENT_WALLET_SECRET,
        )
    }
    if (!::sep10ClientMultiSig.isInitialized) {
      sep10ClientMultiSig =
        Sep10Client(
          toml.getString("WEB_AUTH_ENDPOINT"),
          toml.getString("SIGNING_KEY"),
          CLIENT_WALLET_ACCOUNT,
          arrayOf(
            CLIENT_WALLET_SECRET,
            CLIENT_WALLET_EXTRA_SIGNER_1_SECRET,
            CLIENT_WALLET_EXTRA_SIGNER_2_SECRET,
          ),
        )
    }
  }

  @Test
  fun testChallengeSerialization() {
    val response = sep10Client.challengeJson()
    val transactionStartIndex = response.indexOf("\"transaction\"") + 15
    val transactionEndIndex = response.indexOf(",", transactionStartIndex) - 1
    val transaction = response.substring(transactionStartIndex, transactionEndIndex)
    Base64.getDecoder().decode(transaction)
  }

  @Test
  fun testAuth() {
    sep10Client.auth()
  }

  @Test
  fun testMultiSig() {
    sep10ClientMultiSig.auth()
  }

  @Test
  fun testUnsignedChallenge() {
    val challenge = sep10Client.challenge()

    assertFailsWith(
      exceptionClass = SepNotAuthorizedException::class,
      block = { sep10Client.validate(ValidationRequest.of(challenge.transaction)) },
    )
  }

  class RawStringTypeAdapter : TypeAdapter<String>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: String?) {
      if (value == null) {
        out.nullValue()
      } else {
        out.jsonValue("\"$value\"")
      }
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): String {
      return reader.nextString()
    }
  }
}
