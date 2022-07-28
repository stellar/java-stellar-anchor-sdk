package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import java.util.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.callback.GetUniqueAddressResponse
import org.stellar.anchor.api.callback.UniqueAddressIntegration
import org.stellar.anchor.api.exception.HttpException
import org.stellar.anchor.sep31.Sep31Transaction

class Sep31DepositInfoGeneratorApiTest {
  @MockK(relaxed = true) private lateinit var uniqueAddressIntegration: UniqueAddressIntegration
  @MockK(relaxed = true) private lateinit var txn: Sep31Transaction
  val txnId = "this_is_a_transaction_id"
  val stellarAddress = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
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

    every { txn.getId() } returns txnId
    every { uniqueAddressIntegration.getUniqueAddress(any()) } returns uniqueAddressResponse

    val generator = Sep31DepositInfoGeneratorApi(uniqueAddressIntegration)
    val depositInfo = generator.generate(txn)
    assertEquals(stellarAddress, depositInfo.stellarAddress)
    assertEquals(memo, depositInfo.memo)
    assertEquals(memoType, depositInfo.memoType)
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

    val generator = Sep31DepositInfoGeneratorApi(uniqueAddressIntegration)
    assertThrows<Exception> { generator.generate(txn) }
  }

  @ParameterizedTest
  @ValueSource(ints = [500, 501])
  fun test_generate_integration_exception(statusCode: Int) {
    every { txn.getId() } returns txnId
    every { uniqueAddressIntegration.getUniqueAddress(any()) } throws HttpException(statusCode)

    val generator = Sep31DepositInfoGeneratorApi(uniqueAddressIntegration)
    val ex = assertThrows<HttpException> { generator.generate(txn) }
    assertEquals(statusCode, ex.statusCode)
  }
}
