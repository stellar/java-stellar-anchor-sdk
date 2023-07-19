package org.stellar.anchor.platform

data class TestConfig(var testProfileName: String) {
  val env = mutableMapOf<String, String>()
  // override test profile name with TEST_PROFILE_NAME system env variable
  private val profileName = System.getenv("TEST_PROFILE_NAME") ?: testProfileName
  init {
    // read test.env file
    val testEnv = readResourceAsMap("profiles/${profileName}/test.env")
    // read config.env file
    val configEnv = readResourceAsMap("profiles/${profileName}/config.env")
    // merge test.env, config.env and system env variables
    env.putAll(testEnv)
    env.putAll(configEnv)
    env.putAll(System.getenv())
  }
}
