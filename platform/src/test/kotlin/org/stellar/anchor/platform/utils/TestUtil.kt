package org.stellar.anchor.platform.utils

import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.RateFee

fun Amount.toRate(): RateFee {
  return RateFee(this.amount, this.asset)
}
