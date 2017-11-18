package com.neuronrobotics.bowlerstudio.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ScriptingHelper {

  /**
   * Read all bytes from a file as UTF-8.
   *
   * @param file File to read from
   * @return UTF-8 String
   */
  public static String readBytesAsUTF8(File file) {
    return readBytesWithCharset(file, "UTF-8");
  }

  /**
   * Read all bytes from a file with some charset.
   *
   * @param file File to read from
   * @param charset Charset to use for the output String
   * @return String with provided charset
   */
  public static String readBytesWithCharset(File file, String charset) {
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      return new String(bytes, charset);
    } catch (IOException e) {
      return null;
    }
  }

}
