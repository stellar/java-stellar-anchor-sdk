package org.stellar.anchor.platform.utils

import io.mockk.every
import javax.crypto.SecretKey
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.FeeDetails
import org.stellar.anchor.config.CustodySecretConfig
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.util.KeyUtil

// TODO: remove copypaste below
fun Amount.toRate(): FeeDetails {
  return FeeDetails(this.amount, this.asset)
}

fun String.toSecretKey(): SecretKey {
  return KeyUtil.toSecretKeySpecOrNull(this)
}

const val TEST_SIGNING_SEED = "SBVEOFAHGJCKGR4AAM7RTDRCP6RMYYV5YUV32ZK7ZD3VPDGGHYLXTZRZ"

fun SecretConfig.setupMock(block: (() -> Any)? = null) {
  val cfg = this

  every { cfg.sep6MoreInfoUrlJwtSecret } returns
    "jwt_secret_sep_6_more_info_url_jwt_secret".toSecretKey()
  every { cfg.sep10JwtSecretKey } returns "jwt_secret_sep_10_secret_key_jwt_secret".toSecretKey()
  every { cfg.sep10SigningSeed } returns TEST_SIGNING_SEED
  every { cfg.sep24InteractiveUrlJwtSecret } returns
    "jwt_secret_sep_24_interactive_url_jwt_secret".toSecretKey()
  every { cfg.sep24MoreInfoUrlJwtSecret } returns
    "jwt_secret_sep_24_more_info_url_jwt_secret".toSecretKey()
  every { cfg.callbackAuthSecret } returns "callback_auth_secret_key____________________________"
  every { cfg.callbackAuthSecretKey } returns
    "callback_auth_secret_key____________________________".toSecretKey()
  every { cfg.platformAuthSecret } returns "platform_auth_secret_key____________________________"
  every { cfg.platformAuthSecretKey } returns
    "platform_auth_secret_key____________________________".toSecretKey()

  block?.invoke()
}

fun CustodySecretConfig.setupMock() {
  val cfg = this
  every { cfg.custodyAuthSecretKey } returns
    "custody_auth_secret_key_________________________".toSecretKey()
}
