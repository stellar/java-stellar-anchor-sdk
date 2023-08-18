package org.stellar.reference.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
  val id: String,
  @SerialName("stellar_account") val stellarAccount: String?,
  val memo: String?,
  val memoType: String?,
  @SerialName("first_name") val firstName: String?,
  @SerialName("last_name") val lastName: String?,
  @SerialName("email_address") val emailAddress: String?,
  @SerialName("bank_account_number") val bankAccountNumber: String?,
  @SerialName("bank_account_type") val bankAccountType: String?,
  @SerialName("bank_routing_number") val bankRoutingNumber: String?,
  @SerialName("clabe_number") val clabeNumber: String?,
)

enum class Status {
  NEEDS_INFO,
  ACCEPTED,
  PROCESSING,
  ERROR,
}
