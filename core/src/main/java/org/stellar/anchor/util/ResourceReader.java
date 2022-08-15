package org.stellar.anchor.util;

/** The interface to reads resource external to the application. */
public interface ResourceReader {
  /**
   * Reads the specified resource as a string.
   *
   * @param path The location of the resource.
   * @return The content of the resource.
   */
  String readResourceAsString(String path);

  /**
   * Checks whether the resource exists.
   *
   * @param path The location of the resource.
   * @return True if the resource is found, False otherwise.
   */
  boolean checkResourceExists(String path);
}
