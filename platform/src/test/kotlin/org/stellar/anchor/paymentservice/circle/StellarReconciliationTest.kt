package org.stellar.anchor.paymentservice.circle

import com.google.gson.Gson
import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.stellar.anchor.platform.payment.observer.circle.StellarReconciliation
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer
import org.stellar.anchor.platform.payment.observer.circle.util.NettyHttpClient
import org.stellar.anchor.util.GsonUtils
import org.stellar.sdk.Server
import reactor.netty.http.client.HttpClient

class StellarReconciliationTest {
  internal class StellarReconciliationImpl(private val horizonUrl: String) : StellarReconciliation {
    private val _horizonServer: Server = Server(this.horizonUrl)

    override fun getWebClient(authenticated: Boolean): HttpClient {
      return NettyHttpClient.withBaseUrl(horizonUrl)
    }

    override fun getHorizonServer(): Server {
      return this._horizonServer
    }
  }

  private lateinit var gson: Gson

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    gson =
      GsonUtils.builder()
        .registerTypeAdapter(CircleTransfer::class.java, CircleTransfer.Serialization())
        .create()
  }

  @Test
  fun test_updatedTransferStellarSender() {
    // mock response in the server
    val server = MockWebServer()
    server.start()
    val stellarResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(CirclePaymentServiceTest.mockStellarPaymentResponsePageBody)
    server.enqueue(stellarResponse)

    // verify the original transfer source address is null
    val originalTransfer =
      gson.fromJson(
        CirclePaymentServiceTest.mockStellarToWalletTransferJson,
        CircleTransfer::class.java
      )
    assertNull(originalTransfer.source.address)

    // verify the method `.updatedStellarSenderAddress` populates the source address
    val impl = StellarReconciliationImpl(server.url("").toString())
    var updatedTransfer: CircleTransfer? = null
    assertDoesNotThrow {
      updatedTransfer = impl.updatedStellarSenderAddress(originalTransfer).block()
    }
    assertEquals(
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
      updatedTransfer?.source?.address
    )

    // verify the updated transfer is a new object and did not cause any change in the original
    // transfer object
    assertNull(originalTransfer.source.address)

    // validate the request format
    val request = server.takeRequest()
    assertEquals("GET", request.method)
    println(request.requestUrl)
    MatcherAssert.assertThat(
      request.path,
      CoreMatchers.endsWith(
        "/transactions/fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1/payments"
      )
    )
  }
}
