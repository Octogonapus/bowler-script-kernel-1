package com.neuronrobotics.bowlerkernel;

import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * The Class SDKBuildInfo.
 */
public class BowlerKernelBuildInfo {
  /**
   * The Constant NAME.
   */
  private static final String NAME = "CommonWealthRobotics SDK "
      + getProtocolVersion()
      + "."
      + getSDKVersion()
      + "(" + getBuildVersion() + ")";

  /**
   * Gets the version.
   *
   * @return the version
   */
  public static String getVersion() {
    String out = getTag("app.version");

    if (out == null) {
      out = "0.0.0";
    }

    return out;
  }

  /**
   * Gets the protocol version.
   *
   * @return the protocol version
   */
  public static int getProtocolVersion() {
    return getBuildInfo()[0];
  }

  /**
   * Gets the SDK version.
   *
   * @return the SDK version
   */
  public static int getSDKVersion() {
    return getBuildInfo()[1];
  }

  /**
   * Gets the builds the version.
   *
   * @return the builds the version
   */
  public static int getBuildVersion() {
    return getBuildInfo()[2];
  }

  /**
   * Gets the builds the info.
   *
   * @return the builds the info
   */
  public static int[] getBuildInfo() {
    String out = getVersion();
    String[] splits = out.split("[.]+");
    int[] rev = new int[3];
    for (int i = 0; i < 3; i++) {
      rev[i] = new Integer(splits[i]);
    }
    return rev;
  }

  /**
   * Gets the tag.
   *
   * @param target the target
   * @return the tag
   */
  private static String getTag(String target) {
    try {
      StringBuilder out = new StringBuilder();
      InputStream is = getBuildPropertiesStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line;
      try {
        while (null != (line = br.readLine())) {
          out.append(line).append("\n");
        }
      } catch (IOException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not read line from tag.\n" + Throwables.getStackTraceAsString(e));
      }
      String[] splitAll = out.toString().split("[\n]+");
      for (String aSplitAll : splitAll) {
        if (aSplitAll.contains(target)) {
          String[] split = aSplitAll.split("[=]+");
          return split[1];
        }
      }
    } catch (NullPointerException e) {
      return null;
    }
    return null;
  }

  /**
   * Gets the builds the date.
   *
   * @return the builds the date
   */
  public static String getBuildDate() {
    StringBuilder out = new StringBuilder();
    InputStream is = BowlerKernelBuildInfo.class
        .getResourceAsStream("/META-INF/MANIFEST.MF");
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    try {
      while (null != (line = br.readLine())) {
        out.append(line).append("\n");
      }
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not get build date.\n" + Throwables.getStackTraceAsString(e));
    }

    return "";
  }

  /**
   * Gets the builds the properties stream.
   *
   * @return the builds the properties stream
   */
  private static InputStream getBuildPropertiesStream() {
    return BowlerKernelBuildInfo.class.getResourceAsStream("build.properties");
  }

  /**
   * Gets the SDK version string.
   *
   * @return the SDK version string
   */
  public static String getSDKVersionString() {
    return NAME;
  }

  /**
   * Checks if is o s64bit.
   *
   * @return true, if is o s64bit
   */
  public static boolean isOS64bit() {
    return (System.getProperty("os.arch").contains("x86_64"));
  }

  /**
   * Checks if is arm.
   *
   * @return true, if is arm
   */
  public static boolean isARM() {
    return (System.getProperty("os.arch").toLowerCase().contains("arm"));
  }

  /**
   * Checks if is linux.
   *
   * @return true, if is linux
   */
  public static boolean isLinux() {
    return (System.getProperty("os.name").toLowerCase().contains("linux"));
  }

  /**
   * Checks if is windows.
   *
   * @return true, if is windows
   */
  public static boolean isWindows() {
    return (System.getProperty("os.name").toLowerCase().contains("win"));
  }

  /**
   * Checks if is mac.
   *
   * @return true, if is mac
   */
  public static boolean isMac() {
    return (System.getProperty("os.name").toLowerCase().contains("mac"));
  }

  /**
   * Checks if is unix.
   *
   * @return true, if is unix
   */
  public static boolean isUnix() {
    return (isLinux() || isMac());
  }

}
