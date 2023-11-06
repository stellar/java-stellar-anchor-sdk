package org.stellar.anchor.client.run_profiles

import org.stellar.anchor.client.TestProfileExecutor
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
