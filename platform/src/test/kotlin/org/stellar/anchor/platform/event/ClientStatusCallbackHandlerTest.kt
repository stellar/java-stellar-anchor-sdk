package org.stellar.anchor.platform.event

import io.mockk.every
import io.mockk.mockk
import java.util.*
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.platform.config.ClientsConfig
import org.stellar.anchor.platform.config.PropertySecretConfig
import org.stellar.anchor.util.NetUtil
import org.stellar.anchor.util.StringHelper.json
import org.stellar.sdk.KeyPair

class ClientStatusCallbackHandlerTest {
  private lateinit var handler: ClientStatusCallbackHandler
  private lateinit var secretConfig: PropertySecretConfig
  private lateinit var clientConfig: ClientsConfig.ClientConfig
  private lateinit var signer: KeyPair
  private lateinit var ts: String
  private lateinit var event: AnchorEvent
  private lateinit var payload: String

  @BeforeEach
  fun setUp() {
    clientConfig = ClientsConfig.ClientConfig()
    clientConfig.type = ClientsConfig.ClientType.CUSTODIAL
    clientConfig.signingKey = "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2"
    clientConfig.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"
    handler = ClientStatusCallbackHandler(clientConfig)

    secretConfig = mockk()
    every { secretConfig.sep10SigningSeed } returns
      "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF"
    signer = KeyPair.fromSecretSeed(secretConfig.sep10SigningSeed)

    ts = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
    event = AnchorEvent()
    payload = ts + "." + NetUtil.getDomainFromURL(clientConfig.callbackUrl) + "." + json(event)
  }

  @Test
  fun `test verify request signature`() {
    // header example: "X-Stellar-Signature": "t=....., s=......"
    // Get the signature from request
    val request = handler.buildSignedCallbackRequest(signer, event)
    val requestHeader = request.headers["Signature"]
    val parsedSignature = requestHeader?.split(", ")?.get(1)?.substring(2)
    val decodedSignature = Base64.getDecoder().decode(parsedSignature)

    // re-compose the signature from request info for verify
    val tsInRequest = requestHeader?.split(", ")?.get(0)?.substring(2)
    val payloadToVerify = tsInRequest + "." + request.url.host + "." + request.body
    val signatureToVerify = signer.sign(payloadToVerify.toByteArray())

    Assertions.assertArrayEquals(decodedSignature, signatureToVerify)
  }
}
