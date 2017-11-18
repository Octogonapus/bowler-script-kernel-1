package com.neuronrobotics.bowlerstudio.scripting;


import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.io.IOUtils;


public class RobotHelper implements IScriptingLanguage {

  @Override
  public Object inlineScriptRun(File code, ArrayList<Object> args) {
    byte[] bytes;
    try {
      bytes = Files.readAllBytes(code.toPath());
      String s = new String(bytes, "UTF-8");
      MobileBase mb;
      try {
        mb = new MobileBase(IOUtils.toInputStream(s, "UTF-8"));

        mb.setGitSelfSource(ScriptingEngine.findGitTagFromFile(code));
        return mb;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    return null;
  }

  @Override
  public Object inlineScriptRun(String code, ArrayList<Object> args) {
    MobileBase mb;

    try {
      mb = new MobileBase(IOUtils.toInputStream(code, "UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return mb;
  }

  @Override
  public String getShellType() {
    return "MobilBaseXML";
  }

  @Override
  public boolean getIsTextFile() {
    return true;
  }

  @Override
  public ArrayList<String> getFileExtenetion() {
    return new ArrayList<>(Collections.singletonList("xml"));
  }

}
