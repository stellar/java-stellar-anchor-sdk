package org.stellar.anchor.platform.event

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.*
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.api.sep.sep24.TransactionResponse
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.platform.config.ClientsConfig
import org.stellar.anchor.platform.config.PropertySecretConfig
import org.stellar.anchor.sep24.MoreInfoUrlConstructor
import org.stellar.anchor.sep24.Sep24Helper
import org.stellar.anchor.sep24.Sep24Helper.fromTxn
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.StringHelper.json
import org.stellar.sdk.KeyPair

class ClientStatusCallbackHandlerTest {
  private lateinit var handler: ClientStatusCallbackHandler
  private lateinit var secretConfig: PropertySecretConfig
  private lateinit var clientConfig: ClientsConfig.ClientConfig
  private lateinit var signer: KeyPair
  private lateinit var ts: String
  private lateinit var event: AnchorEvent

  @MockK(relaxed = true) private lateinit var sep24TransactionStore: Sep24TransactionStore
  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var assetService: AssetService
  @MockK(relaxed = true) lateinit var moreInfoUrlConstructor: MoreInfoUrlConstructor

  @BeforeEach
  fun setUp() {
    clientConfig = ClientsConfig.ClientConfig()
    clientConfig.type = ClientsConfig.ClientType.CUSTODIAL
    clientConfig.signingKey = "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2"
    clientConfig.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"

    sep24TransactionStore = mockk<Sep24TransactionStore>()
    sep31TransactionStore = mockk<Sep31TransactionStore>()
    every { sep24TransactionStore.findByTransactionId(any()) } returns null
    mockkStatic(Sep24Helper::class)
    every { fromTxn(any(), any(), any()) } returns mockk<TransactionResponse>()

    assetService = mockk<AssetService>()
    moreInfoUrlConstructor = mockk<MoreInfoUrlConstructor>()

    secretConfig = mockk()
    every { secretConfig.sep10SigningSeed } returns
      "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF"
    signer = KeyPair.fromSecretSeed(secretConfig.sep10SigningSeed)

    ts = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
    event = AnchorEvent()
    event.transaction = GetTransactionResponse()
    event.transaction.sep = PlatformTransactionData.Sep.SEP_24

    handler =
      ClientStatusCallbackHandler(
        secretConfig,
        clientConfig,
        sep24TransactionStore,
        sep31TransactionStore,
        assetService,
        moreInfoUrlConstructor
      )
  }

  @Test
  fun `test verify request signature`() {
    // header example: "X-Stellar-Signature": "t=....., s=......"
    // Get the signature from request
    val payload = json(event)
    val request =
      ClientStatusCallbackHandler.buildHttpRequest(signer, payload, clientConfig.callbackUrl)
    val requestHeader = request.headers["Signature"]
    val parsedSignature = requestHeader?.split(", ")?.get(1)?.substring(2)
    val decodedSignature = Base64.getDecoder().decode(parsedSignature)

    // re-compose the signature from request info for verify
    val tsInRequest = requestHeader?.split(", ")?.get(0)?.substring(2)
    val payloadToVerify = tsInRequest + "." + request.url.host + "." + payload
    val signatureToVerify = signer.sign(payloadToVerify.toByteArray())

    Assertions.assertArrayEquals(decodedSignature, signatureToVerify)
  }
}
