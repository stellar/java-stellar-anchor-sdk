package org.stellar.anchor.util;

import java.util.Enumeration;
import java.util.Properties;

public class PrefixedProperties extends Properties {
  public PrefixedProperties(String prefix, Properties props) {
    if (props == null) {
      return;
    }

    Enumeration<?> en = props.propertyNames();
    while (en.hasMoreElements()) {
      String propName = (String) en.nextElement();
      String propValue = props.getProperty(propName);

      if (propName.startsWith(prefix)) {
        String key = propName.substring(prefix.length());
        setProperty(key, propValue);
      }
    }
  }
}
