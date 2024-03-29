package org.stellar.anchor.platform.utils

import javax.crypto.SecretKey
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.FeeDetails
import org.stellar.anchor.util.KeyUtil

fun Amount.toRate(): FeeDetails {
  return FeeDetails(this.amount, this.asset)
}

fun String.toSecretKey(): SecretKey {
  return KeyUtil.toSecretKeySpecOrNull(this)
}
