package org.stellar.reference.data

import org.stellar.sdk.KeyPair

data class LocationConfig(val ktReferenceServerConfig: String)

data class Config(
  val sep24: Sep24,
)

data class Sep24(
  val port: Int,
  val enableTest: Boolean,
  val anchorPlatformUrl: String,
  val horizonUrl: String,
  val secret: String,
  val interactiveJwtKey: String,
  val custodyEnabled: Boolean,
  val rpcEnabled: Boolean
) {
  val keyPair: KeyPair? = if (custodyEnabled) null else KeyPair.fromSecretSeed(secret)
}
