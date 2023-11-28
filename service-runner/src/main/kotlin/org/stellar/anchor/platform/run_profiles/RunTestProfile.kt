@file:JvmName("RunTestProfile")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.TestProfileExecutor
import org.stellar.anchor.platform.testProfileExecutor

fun main() = runBlocking {
  testProfileExecutor = TestProfileExecutor(TestConfig())
  launch { registerShutdownHook(testProfileExecutor) }
  testProfileExecutor.start(true)
}
