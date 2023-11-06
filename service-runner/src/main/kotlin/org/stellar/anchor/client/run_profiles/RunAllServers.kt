@file:JvmName("RunAllServers")

package org.stellar.anchor.client.run_profiles

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.client.*

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig(testProfileName = "default"))
  launch { registerShutdownHook(testProfileExecutor) }
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "true"
  }
}
