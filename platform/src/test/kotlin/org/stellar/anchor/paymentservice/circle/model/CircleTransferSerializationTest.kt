package org.stellar.anchor.paymentservice.circle.model

import com.google.gson.Gson
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.platform.payment.observer.circle.model.CircleBalance
import org.stellar.anchor.platform.payment.observer.circle.model.CirclePaymentStatus
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransactionParty
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer
import org.stellar.anchor.util.GsonUtils

class CircleTransferSerializationTest {
  private lateinit var gson: Gson

  companion object {
    const val mockCircleTransferJson =
      """{
        "id":"a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        "source":{
          "type":"wallet",
          "id":"1000066041"
        },
        "destination":{
          "type":"blockchain",
          "address":"GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
          "chain":"XLM"
        },
        "amount":{
          "amount":"1.00",
          "currency":"USD"
        },
        "transactionHash":"5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef",
        "status":"complete",
        "createDate":"2022-01-01T01:01:01.544Z"
      }"""
  }

  private fun instantFromString(dateStr: String): Instant {
    return DateTimeFormatter.ISO_INSTANT.parse(dateStr, Instant::from)
  }

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    gson =
      GsonUtils.builder()
        .registerTypeAdapter(CircleTransfer::class.java, CircleTransfer.Serialization())
        .create()
  }

  @Test
  fun testDeserialize() {
    val wantTransfer = CircleTransfer()
    wantTransfer.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    wantTransfer.source = CircleTransactionParty.wallet("1000066041")
    wantTransfer.destination =
      CircleTransactionParty.stellar(
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        null
      )
    wantTransfer.amount = CircleBalance("USD", "1.00")
    wantTransfer.transactionHash =
      "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    wantTransfer.status = CirclePaymentStatus.COMPLETE
    wantTransfer.createDate = instantFromString("2022-01-01T01:01:01.544Z")
    wantTransfer.originalResponse =
      hashMapOf<String, Any>(
        "id" to "a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        "source" to hashMapOf<String, Any>("id" to "1000066041", "type" to "wallet"),
        "destination" to
          hashMapOf<String, Any>(
            "type" to "blockchain",
            "address" to "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
            "chain" to "XLM"
          ),
        "amount" to
          hashMapOf<String, Any>(
            "amount" to "1.00",
            "currency" to "USD",
          ),
        "transactionHash" to "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef",
        "status" to "complete",
        "createDate" to "2022-01-01T01:01:01.544Z"
      )

    val transfer = gson.fromJson(mockCircleTransferJson, CircleTransfer::class.java)
    assertEquals(wantTransfer, transfer)
  }

  @Test
  fun testSerialize() {
    val transfer = CircleTransfer()
    transfer.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    transfer.source = CircleTransactionParty.wallet("1000066041")
    transfer.destination =
      CircleTransactionParty.stellar(
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        null
      )
    transfer.amount = CircleBalance("USD", "1.00")
    transfer.transactionHash = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    transfer.status = CirclePaymentStatus.COMPLETE
    transfer.createDate = instantFromString("2022-01-01T01:01:01.544Z")
    transfer.originalResponse =
      hashMapOf<String, Any>(
        "id" to "a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        "source" to hashMapOf<String, Any>("id" to "1000066041", "type" to "wallet"),
        "destination" to
          hashMapOf<String, Any>(
            "type" to "blockchain",
            "address" to "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
            "chain" to "XLM"
          ),
        "amount" to
          hashMapOf<String, Any>(
            "amount" to "1.00",
            "currency" to "USD",
          ),
        "transactionHash" to "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef",
        "status" to "complete",
        "createDate" to "2022-01-01T01:01:01.544Z"
      )

    val transferJson = gson.toJson(transfer)
    JSONAssert.assertEquals(mockCircleTransferJson, transferJson, true)
  }
}
