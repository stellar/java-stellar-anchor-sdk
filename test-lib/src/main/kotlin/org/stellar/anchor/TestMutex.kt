package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

private class TestMutex(vararg clzs: Class<*>) {
  companion object {
    val knownClasses = mutableMapOf<Class<*>, Mutex>()
  }

  private val classes = clzs.sortedBy { it.name }

  fun withLock(action: () -> Unit) {
    runBlocking {
      for (clazz in classes) {
        if (knownClasses[clazz] == null) {
          knownClasses[clazz] = Mutex()
        }
        knownClasses[clazz]!!.lock()
        println("locked ${clazz.name}")
      }
      try {
        action()
      } finally {
        for (clazz in classes.asReversed()) {
          knownClasses[clazz]!!.unlock()
          println("unlocked ${clazz.name}")
        }
      }
    }
  }
}

/**
 * Obtain locks of the clzs in order and execute the action.
 *
 * @param clzs the Java classes to lock
 * @param action the action to execute
 * @receiver
 */
fun withLock(vararg clzs: Class<*>, action: () -> Unit) {
  TestMutex(*clzs).withLock { action() }
}

/**
 * Obtain locks of the clzs in order and execute the action.
 *
 * @param clzs the Kotlin classes to lock
 * @param action the action to execute
 */
fun withLock(vararg clzs: KClass<*>, action: () -> Unit) {
  val javaClzs = Array<Class<*>>(clzs.size) { clzs[it].java }
  TestMutex(*javaClzs).withLock { action() }
}

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
