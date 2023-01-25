package com.example.data

import com.example.sep24.ProxySep24ParametersProcessor
import com.example.sep24.Sep24ParametersProcessor
import com.example.sep24.TestSep24ParametersProcessor
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

enum class Mode(val parametersProcessor: Sep24ParametersProcessor) {
  PROXY(
    ProxySep24ParametersProcessor
  ), // Used together with https://github.com/stellar/sep24-reference-ui
  TEST(TestSep24ParametersProcessor) // Used for integration tests
}
