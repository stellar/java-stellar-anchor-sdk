package org.stellar.anchor.platform.custody.fireblocks

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.mockk.mockk
import java.io.IOException
import kotlin.test.assertEquals
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.platform.config.PropertyCustodySecretConfig
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService.FIREBLOCKS_SIGNATURE_HEADER
import org.stellar.anchor.util.FileUtil.getResourceFileAsString

class FireblocksEventServiceTest {

  private lateinit var secretConfig: PropertyCustodySecretConfig

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
  }

  @Test
  fun `test handleFireblocksEvent() for valid event object and signature`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService = FireblocksEventService(config)

    val signature: String = getResourceFileAsString("custody/fireblocks/webhook/signature.txt")
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)
    val eventObject: String = getCompactJsonString("custody/fireblocks/webhook/request.json")

    eventsService.handleFireblocksEvent(eventObject, httpHeaders)
  }

  @Test
  fun `test handleFireblocksEvent() for invalid public key`() {
    val config = getFireblocksConfig(StringUtils.EMPTY)
    val ex = assertThrows<InvalidConfigException> { FireblocksEventService(config) }
    assertEquals("Failed to generate Fireblocks public key", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for missed fireblocks-signature header`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/event/public_key.txt"))
    val eventsService = FireblocksEventService(config)

    val eventObject = StringUtils.EMPTY
    val emptyHeaders: Map<String, String> = emptyMap()

    val ex =
      assertThrows<BadRequestException> {
        eventsService.handleFireblocksEvent(eventObject, emptyHeaders)
      }
    assertEquals("'fireblocks-signature' header missed", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for empty signature`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/event/public_key.txt"))
    val eventsService = FireblocksEventService(config)

    val eventObject = StringUtils.EMPTY
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to StringUtils.EMPTY)

    val ex =
      assertThrows<BadRequestException> {
        eventsService.handleFireblocksEvent(eventObject, httpHeaders)
      }
    assertEquals("'fireblocks-signature' is empty", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for invalid signature`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService = FireblocksEventService(config)

    val invalidSignature =
      "Yww6co109EfZ6BBam0zr1ewhv2gB3sFrfzcmbEFTttGp6GYVNEOslcMIMbjrFsFtkiEIO5ogvPI7Boz7y" +
        "QUiXqh92Spj1aG5NoGDdjiW2ozTJxKq7ECK9IsS5vTjIxnBXUIXokCAN2BuiyA8d7LciJ6HwzS+DIvFNyvv7uKU6O0="
    val eventObject: String = getCompactJsonString("custody/fireblocks/webhook/request.json")
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to invalidSignature)

    val ex =
      assertThrows<BadRequestException> {
        eventsService.handleFireblocksEvent(eventObject, httpHeaders)
      }
    assertEquals("Signature validation failed", ex.message)
  }

  fun getFireblocksConfig(publicKey: String): FireblocksConfig {
    val config = FireblocksConfig(secretConfig)
    config.publicKey = publicKey
    return config
  }

  @Throws(IOException::class, SepNotFoundException::class)
  fun getCompactJsonString(fileName: String): String {
    val gson = GsonBuilder().serializeNulls().create()
    val el = JsonParser.parseString(getResourceFileAsString(fileName))
    return gson.toJson(el)
  }
}
