@file:Suppress("unused")

package org.stellar.anchor.client.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.callback.GetUniqueAddressResponse
import org.stellar.anchor.api.callback.UniqueAddressIntegration
import org.stellar.anchor.api.exception.HttpException
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountStore
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager
import org.stellar.anchor.sep31.Sep31Transaction

class Sep31DepositInfoApiGeneratorTest {
  @MockK(relaxed = true) private lateinit var uniqueAddressIntegration: UniqueAddressIntegration

  @MockK(relaxed = true) private lateinit var txn: Sep31Transaction

  @MockK
  private lateinit var paymentObservingAccountStore:
    _root_ide_package_.org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountStore

  // Do not mock the manager
  private lateinit var paymentObservingAccountsManager:
    _root_ide_package_.org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager

  val txnId = "this_is_a_transaction_id"
  val stellarAddress = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    paymentObservingAccountsManager =
      _root_ide_package_.org.stellar.anchor.platform.observer.stellar
        .PaymentObservingAccountsManager(paymentObservingAccountStore)
  }

  @ParameterizedTest
  @CsvSource(
    value = [",", "123,id", "ABCD,text", "OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=,hash"]
  )
  fun test_generate(memo: String?, memoType: String?) {
    val uniqueAddress =
      GetUniqueAddressResponse.UniqueAddress.builder()
        .stellarAddress(stellarAddress)
        .memo(memo)
        .memoType(memoType)
        .build()
    val uniqueAddressResponse =
      GetUniqueAddressResponse.builder().uniqueAddress(uniqueAddress).build()

    // Check the manager is not observing the account
    assertFalse(paymentObservingAccountsManager.lookupAndUpdate(uniqueAddress.stellarAddress))

    every { txn.getId() } returns txnId
    every { uniqueAddressIntegration.getUniqueAddress(any()) } returns uniqueAddressResponse

    val generator =
      _root_ide_package_.org.stellar.anchor.platform.service.Sep31DepositInfoApiGenerator(
        uniqueAddressIntegration,
        paymentObservingAccountsManager
      )
    val depositInfo = generator.generate(txn)
    assertEquals(stellarAddress, depositInfo.stellarAddress)
    assertEquals(memo, depositInfo.memo)
    assertEquals(memoType, depositInfo.memoType)

    // Check if the manager is observing the account
    assertTrue(paymentObservingAccountsManager.lookupAndUpdate(uniqueAddress.stellarAddress))
  }

  @ParameterizedTest
  @CsvSource(value = ["A123,id", "=,hash"])
  fun test_generate_error(memo: String?, memoType: String?) {
    val uniqueAddress =
      GetUniqueAddressResponse.UniqueAddress.builder()
        .stellarAddress(stellarAddress)
        .memo(memo)
        .memoType(memoType)
        .build()
    val uniqueAddressResponse =
      GetUniqueAddressResponse.builder().uniqueAddress(uniqueAddress).build()

    every { txn.getId() } returns txnId
    every { uniqueAddressIntegration.getUniqueAddress(any()) } returns uniqueAddressResponse

    val generator =
      _root_ide_package_.org.stellar.anchor.platform.service.Sep31DepositInfoApiGenerator(
        uniqueAddressIntegration,
        paymentObservingAccountsManager
      )
    assertThrows<Exception> { generator.generate(txn) }

    // Make sure the address does not go into manager when exception happens
    assertFalse(paymentObservingAccountsManager.lookupAndUpdate(uniqueAddress.stellarAddress))
  }

  @ParameterizedTest
  @ValueSource(ints = [500, 501])
  fun test_generate_integration_exception(statusCode: Int) {
    every { txn.getId() } returns txnId
    every { uniqueAddressIntegration.getUniqueAddress(any()) } throws HttpException(statusCode)

    val generator =
      _root_ide_package_.org.stellar.anchor.platform.service.Sep31DepositInfoApiGenerator(
        uniqueAddressIntegration,
        paymentObservingAccountsManager
      )
    val ex = assertThrows<HttpException> { generator.generate(txn) }
    assertEquals(statusCode, ex.statusCode)
  }
}
