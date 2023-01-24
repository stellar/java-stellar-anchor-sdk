package com.example.data

import org.stellar.sdk.KeyPair

data class LocationConfig(val ktReferenceServerConfig: String)

data class Config(
  val port: Int,
  val anchorPlatformUrl: String,
  val horizonUrl: String,
  val secret: Secret
)

data class Secret(val sep24key: String) {
  val keyPair = KeyPair.fromSecretSeed(sep24key)
}
