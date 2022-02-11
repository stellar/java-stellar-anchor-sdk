package org.stellar.anchor.paymentservice.circle

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer
import org.stellar.sdk.Server
import org.stellar.sdk.responses.GsonSingleton
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.operations.OperationResponse
import shadow.com.google.common.reflect.TypeToken

class StellarReconciliationTest {
  internal class StellarReconciliationImpl : StellarReconciliation {

    private var horizonServer: Server? = null

    override fun getHorizonServer(): Server {
      if (horizonServer == null) {
        val type = (object : TypeToken<Page<OperationResponse>>() {}).type
        val mockStellarPaymentResponsePage: Page<OperationResponse> =
          GsonSingleton.getInstance()
            .fromJson(CirclePaymentServiceTest.mockStellarPaymentResponsePageBody, type)
        horizonServer = mockk<Server>()
        every { horizonServer!!.payments().forTransaction(any()).execute() } returns
          mockStellarPaymentResponsePage
      }

      return horizonServer!!
    }
  }

  private lateinit var gson: Gson

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    gson =
      GsonBuilder()
        .registerTypeAdapter(CircleTransfer::class.java, CircleTransfer.Serialization())
        .create()
  }

  @Test
  fun test_updateTransferStellarSender() {
    val originalTransfer =
      gson.fromJson(
        CirclePaymentServiceTest.mockStellarToWalletTransferJson,
        CircleTransfer::class.java
      )

    assertNull(originalTransfer.source.address)

    val updatedTransfer = gson.fromJson(gson.toJson(originalTransfer), CircleTransfer::class.java)
    val impl = StellarReconciliationImpl()
    impl.updateStellarSenderAddress(updatedTransfer)
    assertEquals(
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
      updatedTransfer.source.address
    )

    assertNull(originalTransfer.source.address)
  }

  @Test
  fun test_updatedTransferStellarSender() {
    val originalTransfer =
      gson.fromJson(
        CirclePaymentServiceTest.mockStellarToWalletTransferJson,
        CircleTransfer::class.java
      )

    assertNull(originalTransfer.source.address)

    val impl = StellarReconciliationImpl()
    var updatedTransfer: CircleTransfer? = null
    assertDoesNotThrow {
      updatedTransfer = impl.updatedStellarSenderAddress(originalTransfer).block()
    }
    assertEquals(
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
      updatedTransfer?.source?.address
    )

    assertNull(originalTransfer.source.address)
  }
}
