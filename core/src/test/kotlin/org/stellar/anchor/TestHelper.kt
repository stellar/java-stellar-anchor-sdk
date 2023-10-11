package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.stellar.anchor.auth.Sep10Jwt

class TestHelper {
  companion object {
    const val TEST_ACCOUNT = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
    const val TEST_MEMO = "123456"

    fun createSep10Jwt(
      account: String = TEST_ACCOUNT,
      accountMemo: String? = null,
      hostUrl: String = "",
      clientDomain: String = "vibrant.stellar.org"
    ): Sep10Jwt {
      val issuedAt: Long = System.currentTimeMillis() / 1000L
      return Sep10Jwt.of(
        "$hostUrl/auth",
        if (accountMemo == null) account else "$account:$accountMemo",
        issuedAt,
        issuedAt + 60,
        "",
        clientDomain
      )
    }
  }
}

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
