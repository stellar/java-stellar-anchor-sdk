package org.stellar.anchor

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.reflect.KClass
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LockAndMockTest : BeforeTestExecutionCallback, AfterTestExecutionCallback {
  private val mutexThreadLocal = ThreadLocal<TestMutex>()

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
  //
  //  private fun getLockAndMockStaticAnnotation(context: ExtensionContext?): LockAndMockStatic? {
  //    return context?.requiredTestMethod?.annotations?.find { it is LockAndMockStatic }
  //      as LockAndMockStatic?
  //  }
  //
  //  private fun getLockStaticAnnotation(context: ExtensionContext?): LockStatic? {
  //    return context?.requiredTestMethod?.annotations?.find { it is LockStatic } as LockStatic?
  //  }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockAndMockStatic(val classes: Array<KClass<*>>)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LockStatic(val classes: Array<KClass<*>>)
