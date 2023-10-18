package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KClass

class TestMutex(vararg clzs: KClass<*>) {
  companion object {
    // A map of known classes to their locks
    val knownClasses = mutableMapOf<KClass<*>, ReentrantLock>()
  }

  // The classes to lock. The classes must always be locked by the same order otherwise it will be
  // vulnerable to deadlocks.
  private val classes = clzs.sortedBy { it.qualifiedName }

  fun withLock(action: () -> Unit) {
    try {
      lock()
      action()
    } finally {
      unlock()
    }
  }

  fun lock() {
    for (clazz in classes) {
      var rlock: ReentrantLock?
      synchronized(knownClasses) {
        rlock = knownClasses[clazz]
        if (rlock == null) {
          rlock = ReentrantLock()
          knownClasses[clazz] = rlock!!
        }
      }
      rlock!!.lock()
    }
  }

  fun unlock() {
    for (clazz in classes.asReversed()) {
      knownClasses[clazz]!!.unlock()
    }
  }
}
/**
 * Obtain locks of the clzs in order and execute the action.
 *
 * @param clzs the Kotlin classes to lock
 * @param action the action to execute
 */
fun withLock(vararg clzs: KClass<*>, action: () -> Unit) {
  TestMutex(*clzs).withLock { action() }
}

/**
 * Locks the clzs in order, mocks the static methods of the clzs, executes the action, and unmocks.
 *
 * @param clzs the Kotlin classes to lock and mock
 * @param action the action to execute
 */
fun lockAndMockStatic(vararg clzs: KClass<*>, action: () -> Unit) {
  // Lock all the classes in order
  withLock(*clzs) {
    // static mocks
    for (kclz in clzs) {
      mockkStatic(kclz)
    }
    // call action
    action()
    // static unmocks
    for (kclz in clzs) {
      unmockkStatic(kclz)
    }
  }
}
