package org.stellar.anchor.platform.custody.fireblocks

import com.google.gson.reflect.TypeToken
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.security.Signature
import java.util.*
import kotlin.test.assertEquals
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.fireblocks.FireblocksEventObject
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails
import org.stellar.anchor.api.custody.fireblocks.TransactionStatus
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.platform.config.PropertyCustodySecretConfig
import org.stellar.anchor.platform.custody.CustodyPayment
import org.stellar.anchor.platform.custody.Sep24CustodyPaymentHandler
import org.stellar.anchor.platform.custody.Sep31CustodyPaymentHandler
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService.FIREBLOCKS_SIGNATURE_HEADER
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.platform.utils.RSAUtil
import org.stellar.anchor.platform.utils.RSAUtil.RSA_ALGORITHM
import org.stellar.anchor.platform.utils.RSAUtil.SHA512_WITH_RSA_ALGORITHM
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils
import org.stellar.sdk.Server
import org.stellar.sdk.requests.PaymentsRequestBuilder
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PathPaymentStrictReceiveOperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import org.stellar.sdk.responses.operations.SetTrustLineFlagsOperationResponse

class FireblocksEventServiceTest {

  private val gson = GsonUtils.getInstance()

  private lateinit var secretConfig: PropertyCustodySecretConfig
  private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  private lateinit var sep24CustodyPaymentHandler: Sep24CustodyPaymentHandler
  private lateinit var sep31CustodyPaymentHandler: Sep31CustodyPaymentHandler
  private lateinit var horizon: Horizon
  private lateinit var server: Server
  private lateinit var paymentsRequestBuilder: PaymentsRequestBuilder
  private lateinit var page: Page<OperationResponse>

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    custodyTransactionRepo = mockk()
    sep24CustodyPaymentHandler = mockk()
    sep31CustodyPaymentHandler = mockk()
    horizon = mockk()
    server = mockk()
    paymentsRequestBuilder = mockk()
    page = mockk()
  }

  @Test
  fun `test handleFireblocksEvent() for valid SUBMITTED event object and signature test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val signature: String =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_valid_signature.txt")
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)
    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_request.json")

    eventsService.handleEvent(eventObject, httpHeaders)
  }

  @Test
  fun `test handleFireblocksEvent() for valid CONFIRMING event object with no Stellar transaction test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )

    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/confirming_event_request.json")
        .trimIndent()
    val signature: String = generateSignature(eventObject)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    val paymentCapture = slot<CustodyPayment>()

    every { horizon.server } throws java.lang.RuntimeException("Horizon error")
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), capture(paymentCapture)) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObject, httpHeaders)

    JSONAssert.assertEquals(
      getResourceFileAsString(
        "custody/fireblocks/webhook/completed_event_no_stellar_txn_payment.json"
      ),
      gson.toJson(paymentCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun `test handleFireblocksEvent() for valid FAILED event object and signature with no Stellar transaction test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )

    val signature: String =
      getResourceFileAsString("custody/fireblocks/webhook/failed_event_signature.txt")
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)
    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/failed_event_request.json").trimIndent()

    val paymentCapture = slot<CustodyPayment>()

    every { horizon.server } throws java.lang.RuntimeException("Horizon error")
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), capture(paymentCapture)) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObject, httpHeaders)

    JSONAssert.assertEquals(
      getResourceFileAsString(
        "custody/fireblocks/webhook/failed_event_no_stellar_txn_payment.json"
      ),
      gson.toJson(paymentCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun `test handleFireblocksEvent() for valid COMPLETED event object and signature test with Stellar transaction unknown payment test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )
    val operationRecordsJson =
      getResourceFileAsString("custody/fireblocks/webhook/payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<SetTrustLineFlagsOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/confirming_event_request.json")
    val signature: String = generateSignature(eventObject)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    val paymentCapture = slot<CustodyPayment>()

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("testTxHash") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), capture(paymentCapture)) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObject, httpHeaders)

    verify(exactly = 0) { custodyTransactionRepo.findByExternalTxId(any()) }
  }

  @Test
  fun `test handleFireblocksEvent() for valid CONFIRMING event object and signature with Stellar transaction payment test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )
    val operationRecordsJson =
      getResourceFileAsString("custody/fireblocks/webhook/payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<PaymentOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/confirming_event_request.json")
        .trimIndent()
    val signature: String = generateSignature(eventObject)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    val paymentCapture = slot<CustodyPayment>()

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("testTxHash") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), capture(paymentCapture)) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObject, httpHeaders)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/stellar_txn_payment.json"),
      gson.toJson(paymentCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun `test handleFireblocksEvent() for valid CONFIRMING event object and signature with Stellar transaction path payment test`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )
    val operationRecordsJson =
      getResourceFileAsString("custody/fireblocks/webhook/path_payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<PathPaymentStrictReceiveOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/confirming_event_request.json")
        .trimIndent()
    val signature: String = generateSignature(eventObject)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    val paymentCapture = slot<CustodyPayment>()

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("testTxHash") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), capture(paymentCapture)) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObject, httpHeaders)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/stellar_txn_path_payment.json"),
      gson.toJson(paymentCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @ParameterizedTest
  @EnumSource(
    value = TransactionStatus::class,
    mode = EnumSource.Mode.INCLUDE,
    names =
      [
        "QUEUED",
        "PENDING_AUTHORIZATION",
        "PENDING_SIGNATURE",
        "BROADCASTING",
        "PENDING_3RD_PARTY_MANUAL_APPROVAL",
        "PENDING_3RD_PARTY",
        "COMPLETED",
        "PARTIALLY_COMPLETED",
        "PENDING_AML_SCREENING",
        "REJECTED"
      ]
  )
  fun `test handleEvent() for ignored event object statuses`(status: TransactionStatus) {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val eventObject = FireblocksEventObject()
    eventObject.data = TransactionDetails.builder().status(status).build()

    val eventObjectTxt = gson.toJson(eventObject, FireblocksEventObject::class.java)

    val signature: String = generateSignature(eventObjectTxt)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    eventsService.handleEvent(eventObjectTxt, httpHeaders)

    verify(exactly = 0) { custodyTransactionRepo.findByExternalTxId(any()) }
  }

  @Test
  fun `test handleFireblocksEvent() for invalid public key`() {
    val config = getFireblocksConfig(StringUtils.EMPTY)
    val ex =
      assertThrows<InvalidConfigException> {
        FireblocksEventService(
          custodyTransactionRepo,
          sep24CustodyPaymentHandler,
          sep31CustodyPaymentHandler,
          horizon,
          config
        )
      }
    assertEquals("Failed to generate Fireblocks public key", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for missed fireblocks-signature header`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val eventObject = StringUtils.EMPTY
    val emptyHeaders: Map<String, String> = mapOf()

    val ex =
      assertThrows<BadRequestException> { eventsService.handleEvent(eventObject, emptyHeaders) }
    assertEquals("'fireblocks-signature' header missed", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for empty signature`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val eventObject = StringUtils.EMPTY
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to StringUtils.EMPTY)

    val ex =
      assertThrows<BadRequestException> { eventsService.handleEvent(eventObject, httpHeaders) }
    assertEquals("'fireblocks-signature' is empty", ex.message)
  }

  @Test
  fun `test handleFireblocksEvent() for invalid signature`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val invalidSignature: String =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_invalid_signature.txt")
    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_request.json")
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to invalidSignature)

    eventsService.handleEvent(eventObject, httpHeaders)
  }

  @Test
  fun `test handleFireblocksEvent() for invalid signature encoding`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )

    val invalidSignature = "test"
    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_request.json")
    val httpHeaders = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to invalidSignature)

    eventsService.handleEvent(eventObject, httpHeaders)
  }

  @Test
  fun `test handleFireblocksEvent() set missing external transaction id`() {
    val config =
      getFireblocksConfig(getResourceFileAsString("custody/fireblocks/webhook/public_key.txt"))
    val eventsService =
      FireblocksEventService(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler,
        horizon,
        config
      )
    val custodyTxn =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/custody_transaction.json"),
        JdbcCustodyTransaction::class.java
      )

    val eventObject: String =
      getResourceFileAsString("custody/fireblocks/webhook/confirming_event_request.json")
        .trimIndent()
    val signature: String = generateSignature(eventObject)
    val httpHeaders: Map<String, String> = mutableMapOf(FIREBLOCKS_SIGNATURE_HEADER to signature)

    val transactionToUpdate = slot<JdbcCustodyTransaction>()
    val externalTransactionId = "testEventId"

    every { horizon.server } throws java.lang.RuntimeException("Horizon error")
    every { custodyTransactionRepo.findByExternalTxId(any()) } returns custodyTxn
    every { sep24CustodyPaymentHandler.onSent(eq(custodyTxn), any()) } just runs
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns custodyTxn
    every { custodyTransactionRepo.save(capture(transactionToUpdate)) } returns custodyTxn

    eventsService.handleEvent(eventObject, httpHeaders)
    assertEquals(externalTransactionId, transactionToUpdate.captured.externalTxId)
  }

  private fun getFireblocksConfig(publicKey: String): FireblocksConfig {
    val config = FireblocksConfig(secretConfig)
    config.publicKey = publicKey
    return config
  }

  private fun generateSignature(requestBody: String): String {
    val privateKey =
      RSAUtil.generatePrivateKey(
        getResourceFileAsString("custody/fireblocks/webhook/private_key.txt"),
        RSA_ALGORITHM
      )

    val data = requestBody.toByteArray()

    val sig: Signature = Signature.getInstance(SHA512_WITH_RSA_ALGORITHM)
    sig.initSign(privateKey)
    sig.update(data)
    val signatureBytes: ByteArray = sig.sign()

    return String(Base64.getEncoder().encode(signatureBytes))
  }
}
