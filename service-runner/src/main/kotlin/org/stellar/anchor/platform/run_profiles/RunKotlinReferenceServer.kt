@file:JvmName("RunKotlinReferenceServer")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.*

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig())
  registerShutdownHook(testProfileExecutor)
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
  }
}
