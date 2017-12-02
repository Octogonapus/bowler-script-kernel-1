package com.neuronrobotics.bowlerstudio.assets;

import com.google.common.base.Throwables;

import com.neuronrobotics.bowlerstudio.BowlerKernel;
import com.neuronrobotics.bowlerstudio.LoggerUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class StudioBuildInfo {
  private static Class baseBuildInfoClass = BowlerKernel.class;

  public static String getVersion() {
    String out = getTag("app.version");

    if (out == null) {
      throw new RuntimeException("Failed to loadJNI version number");
    }
    return out;
  }

  public static int getProtocolVersion() {
    return getBuildInfo()[0];
  }

  public static int getSDKVersion() {
    return getBuildInfo()[1];
  }

  public static int getBuildVersion() {
    return getBuildInfo()[2];
  }

  /**
   * Return the build version as an array of three ints (semver).
   *
   * @return Build version
   */
  public static int[] getBuildInfo() {
    try {
      String out = getVersion();
      String[] splits = out.split("[.]+");
      int[] rev = new int[3];
      for (int i = 0; i < 3; i++) {
        rev[i] = new Integer(splits[i]);
      }
      return rev;
    } catch (NumberFormatException e) {
      return new int[]{0, 0, 0};
    }
  }

  private static String getTag(String target) {
    try {
      StringBuilder out = new StringBuilder();
      InputStream inputStream = getBuildPropertiesStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      String line;
      try {
        while (null != (line = reader.readLine())) {
          out.append(line).append("\n");
        }
      } catch (IOException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not write line.\n" + Throwables.getStackTraceAsString(e));
      }

      String[] splitAll = out.toString().split("[\n]+");
      for (String elem : splitAll) {
        if (elem.contains(target)) {
          String[] split = elem.split("[=]+");
          return split[1];
        }
      }
    } catch (NullPointerException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not split string.\n" + Throwables.getStackTraceAsString(e));
    }
    return null;
  }

  public static String getBuildDate() {
    StringBuilder builder = new StringBuilder();
    InputStream is = StudioBuildInfo.class.getResourceAsStream("/META-INF/MANIFEST.MF");
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;

    try {
      while (null != (line = br.readLine())) {
        builder.append(line).append("\n");
      }
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not append to string.\n" + Throwables.getStackTraceAsString(e));
    }

    return ""; //TODO: builder not used?
  }

  private static InputStream getBuildPropertiesStream() {
    return baseBuildInfoClass.getResourceAsStream("build.properties");
  }

  public static String getSDKVersionString() {
    return getName();
  }

  public static boolean isOS64bit() {
    return (System.getProperty("os.arch").contains("x86_64"));
  }

  public static boolean isARM() {
    return (System.getProperty("os.arch").toLowerCase().contains("arm"));
  }

  public static boolean isLinux() {
    return (System.getProperty("os.name").toLowerCase().contains("linux"));
  }

  public static boolean isWindows() {
    return (System.getProperty("os.name").toLowerCase().contains("win"));
  }

  public static boolean isMac() {
    return (System.getProperty("os.name").toLowerCase().contains("mac"));
  }

  public static boolean isUnix() {
    return (isLinux() || isMac());
  }

  public static Class getBaseBuildInfoClass() {
    return baseBuildInfoClass;
  }

  public static void setBaseBuildInfoClass(Class baseClass) {
    baseBuildInfoClass = baseClass;
  }

  /**
   * Return the name with version info.
   *
   * @return Name with version info
   */
  public static String getName() {
    return "Bowler Studio "
        + getProtocolVersion() + "." + getSDKVersion() + "("
        + getBuildVersion() + ")";
  }
}
