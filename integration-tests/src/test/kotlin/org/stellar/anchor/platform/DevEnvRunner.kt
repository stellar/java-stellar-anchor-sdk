package org.stellar.anchor.platform

fun main() {
  TestEnvRunner(TestConfig(profileName = "default")).start(true) { config ->
    config.env["run_docker"] = "true"
    config.env["run_servers"] = "true"
  }
}
