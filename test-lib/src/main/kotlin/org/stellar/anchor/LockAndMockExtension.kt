package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.reflect.KClass
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LockAndMockTest : BeforeTestExecutionCallback, AfterTestExecutionCallback {
  private val mutexThreadLocal = ThreadLocal<TestMutex>()

  /**
   * Lock the classes and optionally mock the static methods of the classes before the test method
   * call.
   */
  override fun beforeTestExecution(context: ExtensionContext?) {
    val annotation = getAnnotation(context)
    if (annotation is LockAndMockStatic) {
      val mutex = TestMutex(*annotation.classes)
      mutex.lock()
      mutexThreadLocal.set(mutex)
      for (clz in annotation.classes) {
        mockkStatic(clz)
      }
    } else if (annotation is LockStatic) {
      val mutex = TestMutex(*annotation.classes)
      mutex.lock()
      mutexThreadLocal.set(mutex)
    }
  }

  /**
   * Unlock the classes and optionally unmock the static methods of the classes after the test
   * method call.
   */
  override fun afterTestExecution(context: ExtensionContext?) {
    val annotation = getAnnotation(context)
    if (annotation is LockAndMockStatic) {
      for (clz in annotation.classes) {
        unmockkStatic(clz)
      }
      mutexThreadLocal.get()?.unlock()
      mutexThreadLocal.remove()
    } else if (annotation is LockStatic) {
      mutexThreadLocal.get()?.unlock()
      mutexThreadLocal.remove()
    }
  }

  private fun getAnnotation(context: ExtensionContext?): Annotation? {
    return context?.requiredTestMethod?.annotations?.find {
      it is LockAndMockStatic || it is LockStatic
    }
  }
}

/**
 * The annotated method will lock the classes and mock the static methods of the classes in the
 * array.
 *
 * In order to use this annotation, the JUnit test class must be extended with the [LockAndMockTest]
 * extension.
 *
 * For example:
 * ```
 * @ExtendWith(LockAndMockTest::class) class MyTest {}
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockAndMockStatic(val classes: Array<KClass<*>>)

/**
 * The annotated method will lock the classes in the array. In order to use this annotation, the
 * JUnit test class must be extended with the [LockAndMockTest] extension.
 *
 * For example:
 * ```
 * @ExtendWith(LockAndMockTest::class) class MyTest {}
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockStatic(val classes: Array<KClass<*>>)
