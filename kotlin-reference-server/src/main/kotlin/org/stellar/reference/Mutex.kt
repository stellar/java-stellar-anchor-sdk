package org.stellar.reference

import kotlinx.coroutines.sync.Mutex

// This is to make sure transaction submission is mutual exclusive to avoid failures
internal val submissionLock = Mutex()
