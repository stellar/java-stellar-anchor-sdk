package org.stellar.anchor.reference.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.exception.BadRequestException

internal class FeeServiceTest {
  @Test
  fun testGetFee() {
    val feeService = FeeService()
    feeService.getFee(
      GetFeeRequest.builder().sendAmount("100").sendAsset("USDC").receiveAsset("BRL").build()
    )
    assertThrows<BadRequestException> {
      feeService.getFee(GetFeeRequest.builder().sendAsset("USDC").receiveAsset("BRL").build())
    }
    assertThrows<BadRequestException> {
      feeService.getFee(GetFeeRequest.builder().sendAmount("100").receiveAsset("BRL").build())
    }
    assertThrows<BadRequestException> {
      feeService.getFee(GetFeeRequest.builder().sendAmount("100").sendAsset("USDC").build())
    }
  }
}
