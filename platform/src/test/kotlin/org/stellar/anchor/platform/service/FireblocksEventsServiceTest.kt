package org.stellar.anchor.platform.service

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.mockk.spyk
import java.io.IOException
import kotlin.test.assertEquals
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.platform.service.FireblocksEventsService.FIREBLOCKS_SIGNATURE_HEADER
import org.stellar.anchor.util.FileUtil.getResourceFileAsString

class FireblocksEventsServiceTest {

  @Test
  fun `test handleFireblocksEvent() for valid event object and signature`() {
    val publicKeyString = getResourceFileAsString("custody/public_key.txt")
    val eventsService = spyk(FireblocksEventsService(publicKeyString))

    val signature: String = getResourceFileAsString("custody/signature.txt")
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)
    val eventObject: String = getCompactJsonString("custody/webhook_request.json")

    eventsService.handleFireblocksEvent(eventObject, httpHeaders)
  }

  @Test
  fun `test handleFireblocksEvent() for missed fireblocks-signature header`() {
    val eventsService = spyk(FireblocksEventsService(StringUtils.EMPTY))

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
    val eventsService = spyk(FireblocksEventsService(StringUtils.EMPTY))

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
    val publicKeyString = getResourceFileAsString("custody/public_key.txt")
    val eventsService = spyk(FireblocksEventsService(publicKeyString))

    val invalidSignature =
      "Yww6co109EfZ6BBam0zr1ewhv2gB3sFrfzcmbEFTttGp6GYVNEOslcMIMbjrFsFtkiEIO5ogvPI7Boz7y" +
        "QUiXqh92Spj1aG5NoGDdjiW2ozTJxKq7ECK9IsS5vTjIxnBXUIXokCAN2BuiyA8d7LciJ6HwzS+DIvFNyvv7uKU6O0="
    val eventObject: String = getCompactJsonString("custody/webhook_request.json")
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to invalidSignature)

    val ex =
      assertThrows<BadRequestException> {
        eventsService.handleFireblocksEvent(eventObject, httpHeaders)
      }
    assertEquals("Signature validation failed", ex.message)
  }

  @Throws(IOException::class, SepNotFoundException::class)
  fun getCompactJsonString(fileName: String): String {
    val gson = GsonBuilder().serializeNulls().create()
    val el = JsonParser.parseString(getResourceFileAsString(fileName))
    return gson.toJson(el)
  }
}
