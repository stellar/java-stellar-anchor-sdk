package com.example.data

import org.stellar.sdk.KeyPair

data class LocationConfig(val ktReferenceServerConfig: String)

data class Config(
  val sep24: Sep24,
)

data class Sep24(
  val port: Int,
  val mode: Mode,
  val anchorPlatformUrl: String,
  val horizonUrl: String,
  val secret: String
) {
  val keyPair = KeyPair.fromSecretSeed(secret)
}

enum class Mode() {
  PROXY, // Used together with https://github.com/stellar/sep24-reference-ui
  TEST // Used for integration tests
}
