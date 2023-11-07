package org.stellar.reference

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// This is to make sure transaction submission is mutual exclusive to avoid failures
internal val submissionLock = Mutex()

suspend fun transactionWithRetry(transactionLogic: suspend () -> Unit) =
  flow<Unit> { submissionLock.withLock { transactionLogic() } }
    .retryWhen { _, attempt ->
      if (attempt < 5) {
        delay((5 + (1..5).random()).seconds)
        return@retryWhen true
      } else {
        return@retryWhen false
      }
    }
    .collect {}
