package org.stellar.anchor.platform

data class TestConfig(var profileName: String) {
  val env = mutableMapOf<String, String>()
  //  val testEnvFile: String
  //  val configEnvFile: String
  init {
    // override test profile name with TEST_PROFILE_NAME env variable
    profileName = System.getenv("TEST_PROFILE_NAME") ?: profileName
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
