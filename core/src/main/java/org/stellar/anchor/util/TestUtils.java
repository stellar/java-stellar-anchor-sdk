package org.stellar.anchor.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestUtils {
  public static Object callPrivate(Object objectInstance, String methodName, Object... args)
      throws Throwable {
    // gather list of classes
    Class<?>[] classes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      classes[i] = args[i].getClass();
    }

    // gather the private method
    Method privateMethod;
    try {
      privateMethod = objectInstance.getClass().getDeclaredMethod(methodName, classes);
    } catch (NoSuchMethodException ex) {
      throw new IllegalAccessException(String.format("Method %s was not found", methodName));
    } catch (SecurityException ex) {
      throw new IllegalAccessException("Accessing the method $methodName is a security violation!");
    }

    // Make the method accessible
    if (!privateMethod.trySetAccessible()) {
      throw new IllegalAccessException(
          String.format("Method %s could not be made accessible", methodName));
    }

    // call the private method
    try {
      return privateMethod.invoke(objectInstance, args);
    } catch (InvocationTargetException ex) {
      throw ex.getCause();
    }
  }
}
