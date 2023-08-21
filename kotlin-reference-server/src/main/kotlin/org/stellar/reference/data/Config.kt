package org.stellar.reference.data

import org.stellar.sdk.KeyPair

data class LocationConfig(val ktReferenceServerConfig: String)

data class Config(
  val sep24: Sep24,
  val appSettings: AppSettings,
  val integrationAuth: IntegrationAuth
)

data class Sep24(
  val port: Int,
  val enableTest: Boolean,
  val anchorPlatformUrl: String,
  val horizonUrl: String,
  val secret: String,
  val interactiveJwtKey: String
) {
  val keyPair = KeyPair.fromSecretSeed(secret)
}

data class AppSettings(
  val version: String,
  val platformApiEndpoint: String,
  val hostUrl: String,
  val distributionWallet: String,
  val distributionWalletMemo: String,
  val distributionWalletMemoType: String,
)

data class IntegrationAuth(
  val authType: AuthType,
  val platformToAnchorSecret: String,
  val anchorToPlatformSecret: String,
  val expirationMilliseconds: Long
)

enum class AuthType {
  NONE,
  API_KEY,
  JWT
}
