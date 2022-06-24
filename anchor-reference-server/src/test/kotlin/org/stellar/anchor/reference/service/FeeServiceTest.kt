package org.stellar.anchor.reference.service

import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.reference.model.Customer
import org.stellar.anchor.reference.repo.CustomerRepo

internal class FeeServiceTest {
  companion object {
    private const val stellarCircleUSDC =
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    private const val fiatUSD = "iso4217:USD"
  }

  @Test
  fun testGetFee_errorHandling() {
    val mockCustomerRepo = mockk<CustomerRepo>(relaxed = true)
    val feeService = FeeService(mockCustomerRepo)
    var feeRequestBuilder = GetFeeRequest.builder()

    // empty send_asset
    var ex: BadRequestException = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("send_asset cannot be empty.", ex.message)

    // empty receive_asset
    feeRequestBuilder = feeRequestBuilder.sendAsset(fiatUSD)
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("receive_asset cannot be empty.", ex.message)

    // empty client_id
    feeRequestBuilder = feeRequestBuilder.receiveAsset(stellarCircleUSDC)
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("client_id cannot be empty.", ex.message)

    // empty sender_amount and receiver_amount
    feeRequestBuilder = feeRequestBuilder.clientId("<client-id>")
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sender_amount or receiver_amount must be present.", ex.message)

    // empty sender_id
    feeRequestBuilder = feeRequestBuilder.sendAmount("123.45")
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sender_id cannot be empty.", ex.message)

    // not found sender_id
    every { mockCustomerRepo.findById("<sender-id>") } returns Optional.empty()
    feeRequestBuilder = feeRequestBuilder.senderId("<sender-id>")
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sender_id was not found.", ex.message)

    // empty receiver_id
    val mockCustomer = mockk<Customer>(relaxed = true)
    every { mockCustomerRepo.findById("<sender-id>") } returns Optional.of(mockCustomer)
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("receiver_id cannot be empty.", ex.message)

    // not found receiver_id
    every { mockCustomerRepo.findById("<receiver-id>") } returns Optional.empty()
    feeRequestBuilder = feeRequestBuilder.receiverId("<receiver-id>")
    ex = assertThrows { feeService.getFee(feeRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("receiver_id was not found.", ex.message)

    // success
    every { mockCustomerRepo.findById("<receiver-id>") } returns Optional.of(mockCustomer)
    assertDoesNotThrow { feeService.getFee(feeRequestBuilder.build()) }
  }
}
