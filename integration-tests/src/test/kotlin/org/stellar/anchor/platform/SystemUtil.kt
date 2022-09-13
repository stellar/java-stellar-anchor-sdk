package org.stellar.anchor.platform

import java.lang.reflect.Field

class SystemUtil {
  companion object {
    fun setEnv(key: String, value: String?) {
      try {
        val env = System.getenv()
        val cl: Class<*> = env.javaClass
        val field: Field = cl.getDeclaredField("m")
        field.isAccessible = true
        val writableEnv = field.get(env) as MutableMap<String, String>
        if (value == null) {
          writableEnv.remove(key)
        } else {
          writableEnv[key] = value
        }
      } catch (e: Exception) {
        throw IllegalStateException("Failed to set environment variable", e)
      }
    }
  }
}
