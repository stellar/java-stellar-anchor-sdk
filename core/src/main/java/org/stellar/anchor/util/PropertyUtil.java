package org.stellar.anchor.util;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import org.apache.commons.beanutils.PropertyUtils;

public class PropertyUtil {
  @SuppressWarnings("unchecked")
  public static void set(Object instance, String path, Object value)
      throws ReflectiveOperationException {
    Target location = findTarget(instance, path, true);
    // Now we have reached the final target
    if (location.instance instanceof Map) {
      PropertyUtils.setMappedProperty(location.instance, location.field, value);
    } else {
      PropertyUtils.setNestedProperty(location.instance, location.field, value);
    }
  }

  @SuppressWarnings("unchecked")
  public static Optional<Object> get(Object bean, String path) {
    try {
      Target target = findTarget(bean, path, false);
      if (target.instance == null) {
        return Optional.empty();
      }
      if (target.instance instanceof Map) {
        return Optional.ofNullable(PropertyUtils.getMappedProperty(target.instance, target.field));
      } else {
        return Optional.ofNullable(PropertyUtils.getNestedProperty(target.instance, target.field));
      }
    } catch (ReflectiveOperationException ex) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  static Target findTarget(Object target, String path, boolean create)
      throws ReflectiveOperationException {
    StringTokenizer st = new StringTokenizer(path, ".");
    String name;
    for (name = st.nextToken(); st.hasMoreTokens(); name = st.nextToken()) {
      if (target instanceof Map) {
        target = PropertyUtils.getMappedProperty(target, name);
      } else {
        Object nextTarget = PropertyUtils.getNestedProperty(target, name);
        // If the field does not exist, try to create an instance with the default constructor
        if (nextTarget == null && create) {
          Class<?> type = PropertyUtils.getPropertyType(target, name);
          Constructor<?> cons = type.getConstructor();
          nextTarget = cons.newInstance();
          PropertyUtils.setNestedProperty(target, name, nextTarget);
        }
        target = nextTarget;
      }
    }

    return Target.of(target, name);
  }

  static class Target {
    Object instance;
    String field;

    public static Target of(Object instance, String field) {
      Target target = new Target();
      target.instance = instance;
      target.field = field;
      return target;
    }
  }
}
