package org.stellar.anchor.sep1;

/** The interface to reads resource external to the application. */
public interface ResourceReader {
  /**
   * Reads the specified resource as a string.
   *
   * @param path The location of the resource.
   * @return The content of the resource.
   */
  String readResourceAsString(String path);
}
