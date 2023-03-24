package org.stellar.anchor.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


lateinit var testRunner: TestProfileRunner

fun main() = runBlocking {
  GlobalScope.launch {
    Runtime.getRuntime()
      .addShutdownHook(
        object : Thread() {
          override fun run() {
            testRunner.shutdown()
          }
        }
      )
  }

  testRunner = TestProfileRunner(TestConfig(profileName = "default"))
  testRunner.start(true) { config ->
    config.env["run_docker"] = "true"
    config.env["run_servers"] = "true"
  }
}
