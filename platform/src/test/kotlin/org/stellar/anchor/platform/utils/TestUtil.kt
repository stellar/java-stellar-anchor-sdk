package org.stellar.anchor.platform.utils

import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.FeeDetails

fun Amount.toRate(): FeeDetails {
  return FeeDetails(this.amount, this.asset)
}
