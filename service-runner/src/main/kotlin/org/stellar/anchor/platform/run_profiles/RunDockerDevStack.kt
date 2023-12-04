@file:JvmName("RunDockerDevStack")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.*

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig())
  launch { registerShutdownHook(testProfileExecutor) }
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "true"
    it.env[RUN_ALL_SERVERS] = "false"
  }

  while (true) {
    delay(60000)
  }
}
