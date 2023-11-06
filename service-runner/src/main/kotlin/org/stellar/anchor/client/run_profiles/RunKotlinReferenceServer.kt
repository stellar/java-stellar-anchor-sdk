@file:JvmName("RunKotlinReferenceServer")

package org.stellar.anchor.client.run_profiles

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.client.*

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig(testProfileName = "default"))
  registerShutdownHook(testProfileExecutor)
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
  }
}
