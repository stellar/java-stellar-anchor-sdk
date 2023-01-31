package com.example.data

import io.ktor.server.application.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
  val id: String,
  val status: String,
  val kind: String,
  val message: String? = null,
  @SerialName("amount_in") val amountIn: Amount? = null,
  @SerialName("amount_out") val amountOut: Amount? = null,
  @SerialName("amount_fee") val amountFee: Amount? = null,
  @SerialName("to_account") val toAccount: String? = null,
  @SerialName("request_asset_code") val requestAssetCode: String? = null,
  @SerialName("request_asset_issuer") val requestAssetIssuer: String? = null,
  // TODO: this fields are not populated
  @SerialName("memo") val memo: String? = null,
  @SerialName("memo_type") val memoType: String? = null,
  @SerialName("stellar_transaction_id") val stellarTransactionId: String? = null,
  @SerialName("withdraw_anchor_account") val withdrawAnchorAccount: String? = null
)

@Serializable data class PatchTransactionsRequest(val records: List<PatchTransactionRecord>)

@Serializable
data class PatchTransactionRecord(
  val id: String,
  val status: String,
  val message: String? = null,
  @SerialName("amount_in") val amountIn: Amount? = null,
  @SerialName("amount_out") val amountOut: Amount? = null,
  @SerialName("amount_fee") val amountFee: Amount? = null,
  @SerialName("stellar_transaction_id") val stellarTransactionId: String? = null,
  val memo: String? = null,
  @SerialName("memo_type") val memoType: String? = null,
  @SerialName("withdraw_anchor_account") val withdrawAnchorAccount: String? = null
)

@Serializable data class Amount(val amount: String? = null, val asset: String? = null)

class JwtToken(
  val iss: String, // Issuer
  var sub: String, // Subject Stellar Account
  var iat: Long, // Issued At
  var exp: Long, // Expiration Time
  var jti: String // JWT ID Transaction ID
)

@Serializable
data class DepositRequest(
  val amount: String,
  val name: String,
  val surname: String,
  val email: String
)

@Serializable
data class WithdrawalRequest(
  val amount: String,
  val name: String,
  val surname: String,
  val email: String,
  val bank: String,
  val account: String
)
