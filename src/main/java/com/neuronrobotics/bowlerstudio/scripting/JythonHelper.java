package com.neuronrobotics.bowlerstudio.scripting;

import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.DeviceManager;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javafx.scene.control.Tab;

import org.python.util.PythonInterpreter;

public class JythonHelper implements IScriptingLanguage {

  private PythonInterpreter interp;

  @Override
  public Object inlineScriptRun(String code, ArrayList<Object> args) {
    Properties props = new Properties();
    PythonInterpreter.initialize(System.getProperties(), props,
        new String[]{""});
    if (interp == null) {
      interp = new PythonInterpreter();

      interp.exec("import sys");
    }

    for (String pm : DeviceManager.listConnectedDevice(null)) {
      BowlerAbstractDevice bad = DeviceManager.getSpecificDevice(null, pm);

      try {
        interp.set(bad.getScriptingName(),
            Class.forName(bad.getClass().getName())
                .cast(bad));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      System.err.println("Device " + bad.getScriptingName() + " is " + bad);
    }

    interp.set("args", args);
    interp.exec(code);
    ArrayList<Object> results = new ArrayList<>();

    try {
      results.add(interp.get("csg", CSG.class));
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      results.add(interp.get("tab", Tab.class));
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      results.add(interp.get("device", BowlerAbstractDevice.class));
    } catch (Exception e) {
      e.printStackTrace();
    }

    Log.debug("Jython return = " + results);
    return results;
  }

  @Override
  public Object inlineScriptRun(File code, ArrayList<Object> args) {
    String codeString = ScriptingHelper.readBytesAsUTF8(code);
    if (codeString != null) {
      return inlineScriptRun(codeString, args);
    }

    return null;
  }

  @Override
  public String getShellType() {
    return "Jython";
  }

  @Override
  public boolean getIsTextFile() {
    return true;
  }

  @Override
  public ArrayList<String> getFileExtenetion() {
    return new ArrayList<>(Arrays.asList("py", "jy"));
  }

}
