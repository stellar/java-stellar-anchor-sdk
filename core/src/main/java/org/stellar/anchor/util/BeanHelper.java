package org.stellar.anchor.util;

import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;

public class BeanHelper {
  @SneakyThrows
  public static void updateField(Object src, Object dest, String name) {
    updateField(src, dest, name, false);
  }

  @SneakyThrows
  public static void updateField(Object src, String srcName, Object dest, String destName) {
    updateField(src, srcName, dest, destName, false);
  }

  @SneakyThrows
  public static boolean updateField(Object src, Object dest, String name, boolean txWasUpdated) {
    return updateField(src, name, dest, name, txWasUpdated);
  }

  @SneakyThrows
  public static boolean updateField(
      Object src, String srcName, Object dest, String destName, boolean txWasUpdated) {
    try {
      Object patchValue = PropertyUtils.getNestedProperty(src, srcName);
      Object txnValue = PropertyUtils.getNestedProperty(dest, destName);
      if (patchValue != null && !Objects.equals(patchValue, txnValue)) {
        PropertyUtils.setNestedProperty(dest, destName, patchValue);
        txWasUpdated = true;
      }
      return txWasUpdated;
    } catch (NestedNullException nnex) {
      return txWasUpdated;
    }
  }
}
