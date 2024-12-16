@file:JvmName("RunTestProfile")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.TestProfileExecutor
import org.stellar.anchor.platform.testProfileExecutor
import org.stellar.reference.di.ConfigContainer.Companion.KT_REFERENCE_SERVER_CONFIG

fun main() = runBlocking {
  testProfileExecutor =
    TestProfileExecutor(
      TestConfig().also {
        // if KT_REFERENCE_SERVER_CONFIG environment variable is not set, set it to the default
        // value: "service-runner/src/main/resources/config/reference-config.yaml"
        if (it.env[KT_REFERENCE_SERVER_CONFIG] == null) {
          it.env[KT_REFERENCE_SERVER_CONFIG] =
            "service-runner/src/main/resources/config/reference-config.yaml"
        }
      }
    )
  launch { registerShutdownHook(testProfileExecutor) }
  testProfileExecutor.start(true)
}
