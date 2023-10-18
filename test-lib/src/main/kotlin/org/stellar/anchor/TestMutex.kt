package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KClass

class TestMutex(vararg clzs: KClass<*>) {
  companion object {
    val knownClasses = mutableMapOf<KClass<*>, ReentrantLock>()
  }

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
      var lock: ReentrantLock?
      synchronized(knownClasses) {
        lock = knownClasses[clazz]
        if (lock == null) {
          lock = ReentrantLock()
          knownClasses[clazz] = lock!!
        }
      }
      lock!!.lock()
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

// package org.stellar.anchor
//
// import io.mockk.mockkStatic
// import io.mockk.unmockkStatic
// import java.util.concurrent.locks.ReentrantLock
// import kotlin.reflect.KClass
//
// private class TestMutex(vararg clzs: KClass<*>) {
//  companion object {
//    val knownClasses = mutableMapOf<KClass<*>, ReentrantLock>()
//  }
//
//  private val classes = clzs.sortedBy { it.qualifiedName }
//
//  fun withLock(action: () -> Unit) {
//    for (clazz in classes) {
//      var lock: ReentrantLock?
//      synchronized(knownClasses) {
//        lock = knownClasses[clazz]
//        if (lock == null) {
//          lock = ReentrantLock()
//          knownClasses[clazz] = lock!!
//        }
//      }
//      lock!!.lock()
//      println("Locked ${clazz.qualifiedName} ${knownClasses[clazz]}")
//    }
//    try {
//      action()
//    } finally {
//      for (clazz in classes.asReversed()) {
//        knownClasses[clazz]!!.unlock()
//        println("Unlocked ${clazz.qualifiedName} ${knownClasses[clazz]}")
//      }
//    }
//  }
// }
//
/// **
// * Obtain locks of the clzs in order and execute the action.
// *
// * @param clzs the Kotlin classes to lock
// * @param action the action to execute
// */
// fun withLock(vararg clzs: KClass<*>, action: () -> Unit) {
//  TestMutex(*clzs).withLock { action() }
// }
//
// fun lockAndMockStatic(vararg clzs: KClass<*>, action: () -> Unit) {
//  // Lock all the classes in order
//  withLock(*clzs) {
//    // static mocks
//    for (kclz in clzs) {
//      mockkStatic(kclz)
//    }
//    // call action
//    action()
//    // static unmocks
//    for (kclz in clzs) {
//      unmockkStatic(kclz)
//    }
//  }
// }
