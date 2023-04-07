package org.stellar.anchor.platform.run_profiles

import org.stellar.anchor.platform.TestProfileExecutor
import org.stellar.anchor.util.Log.info

fun registerShutdownHook(testProfileExecutor: TestProfileExecutor) {
  info("Registering docker compose shutdown hook...")
  Runtime.getRuntime()
    .addShutdownHook(
      object : Thread() {
        override fun run() {
          testProfileExecutor.shutdown()
        }
      }
    )
}
