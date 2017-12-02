package com.neuronrobotics.imageprovider;

import com.google.common.base.Throwables;

import com.neuronrobotics.bowlerstudio.LoggerUtilities;

import java.io.File;
import java.util.logging.Level;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OpenCVLoader {
  private static NativeResource resource = null;

  /**
   * Load the OpenCV JNI.
   */
  public static void loadJNI() {
    if (resource != null) {
      return;
    }

    resource = new NativeResource();
    //+Core.NATIVE_LIBRARY_NAME+".so"
    //+Core.NATIVE_LIBRARY_NAME+".so"
    if (NativeResource.isLinux()) {
      String[] possibleLocals = new String[]{
          "/usr/local/share/OpenCV/java/lib/",
          "/usr/lib/jni/lib/",
          "/usr/lib/jni/"
      };

      StringBuilder erBack = new StringBuilder();
      for (String local : possibleLocals) {
        File libDirectory = new File(local);
        if (libDirectory.isDirectory()) {
          File[] possibleLibs = libDirectory.listFiles();
          for (File file : possibleLibs) {
            //System.out.println("Checking file: "+f);
            if (!file.isDirectory()
                && file.getName().contains("opencv_java24")
                && file.getName().endsWith(".so")) {
              try {
                System.load(file.getAbsolutePath());
                Mat mat = Mat.eye(3, 3, CvType.CV_8UC1); //TODO: Not used?
                LoggerUtilities.getLogger().log(Level.INFO,
                    "Loading opencv lib " + file.getAbsolutePath());
                return;
              } catch (Error e) {
                LoggerUtilities.getLogger().log(Level.WARNING,
                    "Error in OpenCV JNI Loader.\n" + Throwables.getStackTraceAsString(e));
                //try the next one
                erBack.append(" ").append(e.getMessage());
              }
            }
          }
        } else {
          erBack.append("No file ").append(local);
        }
      }

      throw new RuntimeException(erBack.toString());
    } else if (NativeResource.isWindows()) {
      String basedir = System.getenv("OPENCV_DIR");
      if (basedir == null) {
        throw new RuntimeException("OPENCV_DIR was not found, environment variable OPENCV_DIR "
            + "needs to be set");
      }
      System.err.println("OPENCV_DIR found at " + basedir);
      if ((!System.getProperty("sun.arch.data.model").contains("32") && basedir.contains("x64"))) {

        basedir.replace("x64", "x86"); //TODO: Result ignored?
        System.err.println("OPENCV_DIR environment variable is not set correctly");
      }
      basedir += "\\..\\..\\java\\";
      //if(basedir.contains("x64")){
      System.load(basedir + "x64\\" + Core.NATIVE_LIBRARY_NAME + ".dll");
      //}else{
      //System.loadJNI(basedir+"x86\\"+Core.NATIVE_LIBRARY_NAME+".dll");
      //}
    } else if (NativeResource.isOSX()) {
      String basedir = System.getenv("OPENCV_DIR");
      if (basedir == null) {
        throw new RuntimeException("OPENCV_DIR was not found, environment variable OPENCV_DIR "
            + "needs to be set");
      }
      //basedir="/Users/hephaestus/Desktop/opencv249build/";
      String lib = basedir.trim() + "/lib/lib" + Core.NATIVE_LIBRARY_NAME + ".dylib";
      System.err.println("OPENCV_DIR found at " + lib);
      System.load(lib);
    }
  }

}
