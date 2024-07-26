package org.stellar.anchor.util

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.operation.Sep31Operation
import org.stellar.anchor.api.shared.*
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.sep24.PojoSep24RefundPayment
import org.stellar.anchor.sep24.PojoSep24Refunds
import org.stellar.anchor.sep24.PojoSep24Transaction
import org.stellar.anchor.sep31.PojoSep31RefundPayment
import org.stellar.anchor.sep31.PojoSep31Refunds
import org.stellar.anchor.sep31.PojoSep31Transaction
import org.stellar.anchor.sep6.PojoSep6Transaction

class TransactionMapperTest {
  companion object {
    private val stellarTransaction =
      StellarTransaction.builder()
        .id("id")
        .memo("memo")
        .memoType("text")
        .createdAt(Instant.now())
        .envelope("envelope")
        .payments(
          listOf(
            StellarPayment.builder()
              .id("id")
              .amount(Amount("100.0000", "USDC"))
              .paymentType(StellarPayment.Type.PAYMENT)
              .sourceAccount("fromAccount")
              .destinationAccount("toAccount")
              .build()
          )
        )
        .build()
  }

  @MockK(relaxed = true) private lateinit var assertService: AssetService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { assertService.getAsset("USDC", "issuer") } returns
      AssetInfo().apply {
        code = "USDC"
        issuer = "issuer"
        schema = AssetInfo.Schema.STELLAR
      }
  }

  @Test
  fun `test SEP-31 transaction mapping`() {
    val sepTxn =
      PojoSep31Transaction().apply {
        id = UUID.randomUUID().toString()
        status = "completed"
        statusEta = 10000
        amountIn = "100.0"
        amountInAsset = "USDC"
        amountOut = "100.0"
        amountOutAsset = "USD"
        feeDetails = FeeDetails("10.0", "USD")
        fromAccount = "fromAccount"
        toAccount = "toAccount"
        stellarMemo = "memo"
        stellarMemoType = "text"
        startedAt = Instant.now()
        completedAt = Instant.now()
        userActionRequiredBy = Instant.now()
        stellarTransactionId = "stellarTransactionId"
        stellarTransactions = listOf(stellarTransaction)
        externalTransactionId = "externalTransactionId"
        requiredInfoMessage = "requiredInfoMessage"
        requiredInfoUpdates =
          Sep31Operation.Fields().apply {
            transaction = mapOf("field" to AssetInfo.Field("description", null, false))
          }
        quoteId = "quoteId"
        clientDomain = "clientDomain"
        clientName = "clientName"
        fields = mapOf("field" to "value")
        refunded = true
        refunds =
          PojoSep31Refunds().apply {
            amountRefunded = "90.0"
            amountFee = "10.0"
            refundPayments =
              listOf(
                PojoSep31RefundPayment().apply {
                  id = "id"
                  amount = "90.0"
                  fee = "10.0"
                }
              )
          }
        refunded = false
        updatedAt = Instant.now()
        transferReceivedAt = Instant.now()
        amountExpected = "100.0"
        receiverId = "receiverId"
        senderId = "senderId"
        creator = StellarId("id", "memo", "memo")
      }

    val actual = GsonUtils.getInstance().toJson(TransactionMapper.toGetTransactionResponse(sepTxn))
    val expected =
      GsonUtils.getInstance()
        .toJson(
          PlatformTransactionData.builder()
            .id(sepTxn.id)
            .sep(PlatformTransactionData.Sep.SEP_31)
            .status(SepTransactionStatus.COMPLETED)
            .kind(PlatformTransactionData.Kind.RECEIVE)
            .amountExpected(Amount("100.0", "USDC"))
            .amountIn(Amount("100.0", "USDC"))
            .amountOut(Amount("100.0", "USD"))
            .feeDetails(FeeDetails("10.0", "USD"))
            .quoteId(sepTxn.quoteId)
            .startedAt(sepTxn.startedAt)
            .updatedAt(sepTxn.updatedAt)
            .completedAt(sepTxn.completedAt)
            .userActionRequiredBy(sepTxn.userActionRequiredBy)
            .transferReceivedAt(sepTxn.transferReceivedAt)
            .message(sepTxn.requiredInfoMessage)
            .refunds(
              Refunds.builder()
                .amountRefunded(Amount("90.0", "USDC"))
                .amountFee(Amount("10.0", "USDC"))
                .payments(
                  arrayOf(
                    RefundPayment.builder()
                      .id("id")
                      .idType(RefundPayment.IdType.STELLAR)
                      .amount(Amount("90.0", "USDC"))
                      .fee(Amount("10.0", "USDC"))
                      .build()
                  )
                )
                .build()
            )
            .stellarTransactions(listOf(stellarTransaction))
            .sourceAccount(sepTxn.fromAccount)
            .destinationAccount(sepTxn.toAccount)
            .externalTransactionId(sepTxn.externalTransactionId)
            .memo(sepTxn.stellarMemo)
            .memoType(sepTxn.stellarMemoType)
            .refundMemo(sepTxn.stellarMemo)
            .refundMemoType(sepTxn.stellarMemoType)
            .clientDomain(sepTxn.clientDomain)
            .clientName(sepTxn.clientName)
            .customers(sepTxn.customers)
            .creator(sepTxn.creator)
            .instructions(null)
            .build()
        )

    JSONAssert.assertEquals(expected, actual, true)
  }

  @Test
  fun `test SEP-24 transaction mapping`() {
    val sepTxn =
      PojoSep24Transaction().apply {
        id = UUID.randomUUID().toString()
        transactionId = this.id
        status = "completed"
        kind = "deposit"
        amountExpected = "100.0"
        amountIn = "100.0"
        amountInAsset = "USDC"
        amountOut = "100.0"
        amountOutAsset = "USD"
        feeDetails = FeeDetails("10.0", "USD")
        quoteId = "quoteId"
        startedAt = Instant.now()
        updatedAt = Instant.now()
        completedAt = Instant.now()
        userActionRequiredBy = Instant.now()
        message = "message"
        refunds =
          PojoSep24Refunds().apply {
            amountRefunded = "90.0"
            amountFee = "10.0"
            refundPayments =
              listOf(
                PojoSep24RefundPayment().apply {
                  id = "id"
                  amount = "90.0"
                  fee = "10.0"
                }
              )
          }
        stellarTransactionId = "stellarTransactionId"
        stellarTransactions = listOf(stellarTransaction)
        externalTransactionId = "externalTransactionId"
        fromAccount = "fromAccount"
        toAccount = "toAccount"
        memo = "memo"
        memoType = "text"
        refundMemo = "refundMemo"
        refundMemoType = "text"
        clientDomain = "clientDomain"
        clientName = "clientName"
        sep10Account = "sep10Account"
        sep10AccountMemo = "sep10AccountMemo"
        withdrawAnchorAccount = "withdrawAnchorAccount"
        claimableBalanceSupported = true
        requestAssetCode = "USDC"
        requestAssetIssuer = "issuer"
      }

    val actual =
      GsonUtils.getInstance()
        .toJson(TransactionMapper.toGetTransactionResponse(sepTxn, assertService))
    val expected =
      GsonUtils.getInstance()
        .toJson(
          PlatformTransactionData.builder()
            .id(sepTxn.id)
            .sep(PlatformTransactionData.Sep.SEP_24)
            .status(SepTransactionStatus.COMPLETED)
            .kind(PlatformTransactionData.Kind.DEPOSIT)
            .amountExpected(Amount("100.0", "stellar:USDC:issuer"))
            .amountIn(Amount("100.0", "USDC"))
            .amountOut(Amount("100.0", "USD"))
            .feeDetails(FeeDetails("10.0", "USD"))
            .quoteId(sepTxn.quoteId)
            .startedAt(sepTxn.startedAt)
            .updatedAt(sepTxn.updatedAt)
            .completedAt(sepTxn.completedAt)
            .userActionRequiredBy(sepTxn.userActionRequiredBy)
            .transferReceivedAt(null)
            .message(sepTxn.message)
            .refunds(
              Refunds.builder()
                .amountRefunded(Amount("90.0", "USDC"))
                .amountFee(Amount("10.0", "USDC"))
                .payments(
                  arrayOf(
                    RefundPayment.builder()
                      .id("id")
                      .idType(RefundPayment.IdType.STELLAR)
                      .amount(Amount("90.0", "USDC"))
                      .fee(Amount("10.0", "USDC"))
                      .build()
                  )
                )
                .build()
            )
            .stellarTransactions(listOf(stellarTransaction))
            .sourceAccount(sepTxn.fromAccount)
            .destinationAccount(sepTxn.toAccount)
            .externalTransactionId(sepTxn.externalTransactionId)
            .memo(sepTxn.memo)
            .memoType(sepTxn.memoType)
            .refundMemo(sepTxn.refundMemo)
            .refundMemoType(sepTxn.refundMemoType)
            .clientDomain(sepTxn.clientDomain)
            .clientName(sepTxn.clientName)
            .customers(
              Customers.builder()
                .sender(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
                .receiver(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
                .build()
            )
            .creator(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
            .instructions(null)
            .build()
        )

    JSONAssert.assertEquals(expected, actual, true)
  }

  @Test
  fun `test SEP-6 transaction mapping`() {
    val sepTxn =
      PojoSep6Transaction().apply {
        id = UUID.randomUUID().toString()
        transactionId = this.id
        status = "completed"
        kind = "deposit"
        amountExpected = "100.0"
        amountIn = "100.0"
        amountInAsset = "USD"
        amountOut = "100.0"
        amountOutAsset = "USDC"
        feeDetails = FeeDetails("10.0", "USD")
        quoteId = "quoteId"
        startedAt = Instant.now()
        updatedAt = Instant.now()
        completedAt = Instant.now()
        userActionRequiredBy = Instant.now()
        transferReceivedAt = Instant.now()
        message = "message"
        refunds =
          Refunds.builder()
            .amountRefunded(Amount("90.0", "USDC"))
            .amountFee(Amount("10.0", "USDC"))
            .payments(
              arrayOf(
                RefundPayment.builder()
                  .id("id")
                  .idType(RefundPayment.IdType.STELLAR)
                  .amount(Amount("90.0", "USDC"))
                  .fee(Amount("10.0", "USDC"))
                  .build()
              )
            )
            .build()
        stellarTransactionId = "stellarTransactionId"
        stellarTransactions = listOf(stellarTransaction)
        externalTransactionId = "externalTransactionId"
        fromAccount = "fromAccount"
        toAccount = "toAccount"
        memo = "memo"
        memoType = "text"
        refundMemo = "refundMemo"
        refundMemoType = "text"
        clientDomain = "clientDomain"
        clientName = "clientName"
        sep10Account = "sep10Account"
        sep10AccountMemo = "sep10AccountMemo"
        withdrawAnchorAccount = "withdrawAnchorAccount"
        requestAssetCode = "USDC"
        requestAssetIssuer = "issuer"
        statusEta = 10000
        type = "bank_account"
        requiredInfoMessage = "requiredInfoMessage"
        requiredInfoUpdates = listOf("field")
        instructions =
          mapOf(
            "field" to InstructionField.builder().value("value").description("description").build()
          )
        feeDetails = FeeDetails("10.0", "USD")
      }

    val actual =
      GsonUtils.getInstance()
        .toJson(TransactionMapper.toGetTransactionResponse(sepTxn, assertService))
    val expected =
      GsonUtils.getInstance()
        .toJson(
          PlatformTransactionData.builder()
            .id(sepTxn.id)
            .sep(PlatformTransactionData.Sep.SEP_6)
            .kind(PlatformTransactionData.Kind.DEPOSIT)
            .status(SepTransactionStatus.COMPLETED)
            .type("bank_account")
            .amountExpected(Amount("100.0", "stellar:USDC:issuer"))
            .amountIn(Amount("100.0", "USD"))
            .amountOut(Amount("100.0", "USDC"))
            .feeDetails(FeeDetails("10.0", "USD"))
            .quoteId(sepTxn.quoteId)
            .startedAt(sepTxn.startedAt)
            .updatedAt(sepTxn.updatedAt)
            .completedAt(sepTxn.completedAt)
            .userActionRequiredBy(sepTxn.userActionRequiredBy)
            .transferReceivedAt(sepTxn.transferReceivedAt)
            .message(sepTxn.message)
            .refunds(sepTxn.refunds)
            .stellarTransactions(listOf(stellarTransaction))
            .sourceAccount(sepTxn.fromAccount)
            .destinationAccount(sepTxn.toAccount)
            .externalTransactionId(sepTxn.externalTransactionId)
            .memo(sepTxn.memo)
            .memoType(sepTxn.memoType)
            .refundMemo(sepTxn.refundMemo)
            .refundMemoType(sepTxn.refundMemoType)
            .clientDomain(sepTxn.clientDomain)
            .clientName(sepTxn.clientName)
            .customers(
              Customers.builder()
                .sender(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
                .receiver(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
                .build()
            )
            .creator(StellarId(null, sepTxn.sep10Account, sepTxn.sep10AccountMemo))
            .build()
        )

    JSONAssert.assertEquals(expected, actual, true)
  }
}
