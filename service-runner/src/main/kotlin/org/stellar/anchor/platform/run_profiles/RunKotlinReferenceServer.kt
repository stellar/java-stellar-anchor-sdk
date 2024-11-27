@file:JvmName("RunKotlinReferenceServer")

package org.stellar.anchor.platform.run_profiles

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.*
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
  registerShutdownHook(testProfileExecutor)
  testProfileExecutor.start(true) {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
  }
}
