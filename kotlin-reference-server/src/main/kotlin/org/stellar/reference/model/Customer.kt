package org.stellar.reference.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
  val id: String? = null,
  @SerialName("stellar_account") val stellarAccount: String? = null,
  val memo: String? = null,
  val memoType: String? = null,
  @SerialName("first_name") val firstName: String? = null,
  @SerialName("last_name") val lastName: String? = null,
  @SerialName("email_address") val emailAddress: String? = null,
  @SerialName("bank_account_number") val bankAccountNumber: String? = null,
  @SerialName("bank_account_type") val bankAccountType: String? = null,
  @SerialName("bank_routing_number") val bankRoutingNumber: String? = null,
  @SerialName("clabe_number") val clabeNumber: String? = null,
)

enum class Status {
  NEEDS_INFO,
  ACCEPTED,
  PROCESSING,
  ERROR,
}
