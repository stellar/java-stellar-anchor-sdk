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
  private lateinit var signer: KeyPair
  private lateinit var ts: String
  private lateinit var event: AnchorEvent
  private lateinit var payload: String

  @BeforeEach
  fun setUp() {
    val clientConfig = ClientsConfig.ClientConfig()
    clientConfig.type = ClientsConfig.ClientType.CUSTODIAL
    clientConfig.signingKey = "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2"
    clientConfig.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"
    handler = ClientStatusCallbackHandler(clientConfig)

    secretConfig = mockk()
    every { secretConfig.sep10SigningSeed } returns
      "SDNMFWJGLVR4O2XV3SNEJVF53MMLQWYFYFC7HT7JZ5235AXPETHB4K3D"
    signer = KeyPair.fromSecretSeed(secretConfig.sep10SigningSeed)

    ts = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
    event = AnchorEvent()
    payload = ts + "." + NetUtil.getDomainFromURL(clientConfig.callbackUrl) + "." + json(event)
  }

  @Test
  fun `test verify request signature`() {
    val originSignature = signer.sign(payload.toByteArray())

    // header example: "X-Stellar-Signature": "t=....., s=......"
    val requestHeader =
      handler.buildSignedCallbackRequest(signer, event).headers["X-Stellar-Signature"]
    val parsedSignature = requestHeader?.split(", ")?.get(1)?.substring(2)
    val decodedSignature = Base64.getDecoder().decode(parsedSignature)

    Assertions.assertArrayEquals(decodedSignature, originSignature)
  }
}
