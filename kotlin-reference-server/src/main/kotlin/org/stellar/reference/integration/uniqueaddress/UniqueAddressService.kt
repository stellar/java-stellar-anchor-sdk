package org.stellar.reference.integration.uniqueaddress

import java.util.*
import org.apache.commons.codec.binary.Hex
import org.stellar.anchor.api.callback.GetUniqueAddressRequest
import org.stellar.anchor.api.callback.GetUniqueAddressResponse
import org.stellar.reference.data.AppSettings
import org.stellar.sdk.KeyPair
import org.stellar.sdk.MemoHash
import org.stellar.sdk.MemoId
import org.stellar.sdk.MemoText

class UniqueAddressService(private val settings: AppSettings) {
  init {
    if (settings.distributionWallet.isBlank()) {
      throw RuntimeException("distributionWallet is empty")
    } else {
      try {
        KeyPair.fromAccountId(settings.distributionWallet)
      } catch (e: Exception) {
        throw RuntimeException("Invalid distributionWallet: ${settings.distributionWallet}")
      }
    }

    val memo = settings.distributionWalletMemo
    val memoType = settings.distributionWalletMemoType

    if (memo.isNotBlank() && memoType.isNotBlank()) {
      when (memoType) {
        "text" -> MemoId(memo.toLong())
        "id" -> MemoText(memo)
        "hash" -> MemoHash(Hex.encodeHexString(Base64.getDecoder().decode(memo)))
        else ->
          throw RuntimeException(
            "Invalid distributionWalletMemoType: ${settings.distributionWalletMemoType}"
          )
      }
    }
  }
  fun getUniqueAddress(request: GetUniqueAddressRequest): GetUniqueAddressResponse {
    val builder =
      GetUniqueAddressResponse.UniqueAddress.builder().stellarAddress(settings.distributionWallet)

    if (settings.distributionWallet.isBlank() || settings.distributionWalletMemoType.isBlank()) {
      val paddedMemo = request.transactionId.take(32).padStart(32, '0')
      val encodedMemo = Base64.getEncoder().encodeToString(paddedMemo.toByteArray())
      builder.memoType("hash").memo(encodedMemo)
    } else {
      builder.memoType(settings.distributionWalletMemoType).memo(settings.distributionWalletMemo)
    }

    return GetUniqueAddressResponse.builder().uniqueAddress(builder.build()).build()
  }
}
