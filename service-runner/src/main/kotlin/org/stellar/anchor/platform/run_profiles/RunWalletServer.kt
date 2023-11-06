@file:JvmName("RunWalletServer")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.*

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig(testProfileName = "default"))
  registerShutdownHook(testProfileExecutor)
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_WALLET_SERVER] = "true"
  }
}
