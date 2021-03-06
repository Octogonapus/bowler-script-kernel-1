package com.neuronrobotics.bowlerstudio.creature;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.util.FileWatchDeviceWrapper;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver;
import com.neuronrobotics.sdk.addons.kinematics.IDriveEngine;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;

public class MobileBaseLoader {
  private static HashMap<MobileBase, MobileBaseLoader> map = new HashMap<>();
  private MobileBase base;
  private IDriveEngine defaultDriveEngine;

  private MobileBaseLoader(MobileBase base) {
    this.setBase(base);

    setDefaultWalkingEngine(base);
  }

  public void setGitDhEngine(String gitsId, String file, DHParameterKinematics dh) {
    dh.setGitDhEngine(new String[] {gitsId, file});

    setDefaultDhParameterKinematics(dh);

  }


  public File setDefaultDhParameterKinematics(DHParameterKinematics device) {
    File code = null;
    try {
      code = ScriptingEngine.fileFromGit(device.getGitDhEngine()[0], device.getGitDhEngine()[1]);
      DhInverseSolver defaultDHSolver =
          (DhInverseSolver) ScriptingEngine.inlineFileScriptRun(code, null);

      File c = code;
      FileWatchDeviceWrapper.watch(device, code, (fileThatChanged, event) -> {

        try {
          System.out.println("D-H Solver changed, updating " + device.getScriptingName());
          DhInverseSolver d = (DhInverseSolver) ScriptingEngine.inlineFileScriptRun(c, null);
          device.setInverseSolver(d);
        } catch (Exception ex) {
          MobileBaseCadManager.get(base).getUi().highlightException(c, ex);
        }
      });

      device.setInverseSolver(defaultDHSolver);
      return code;
    } catch (Exception e1) {
      MobileBaseCadManager.get(base).getUi().highlightException(code, e1);
    }
    return null;

  }

  public void setDefaultWalkingEngine(MobileBase device) {
    if (defaultDriveEngine == null) {
      setGitWalkingEngine(device.getGitWalkingEngine()[0], device.getGitWalkingEngine()[1], device);
    }
    for (DHParameterKinematics dh : device.getAllDHChains()) {
      setDefaultDhParameterKinematics(dh);
    }
  }

  public void setGitWalkingEngine(String git, String file, MobileBase device) {

    device.setGitWalkingEngine(new String[] {git, file});
    File code = null;
    try {
      code = ScriptingEngine.fileFromGit(git, file);
    } catch (GitAPIException | IOException e) {
      MobileBaseCadManager.get(base).getUi().highlightException(code, e);
    }

    File c = code;
    FileWatchDeviceWrapper.watch(device, code, (fileThatChanged, event) -> {

      try {

        defaultDriveEngine = (IDriveEngine) ScriptingEngine.inlineFileScriptRun(c, null);
        device.setWalkingDriveEngine(defaultDriveEngine);
      } catch (Exception ex) {
        MobileBaseCadManager.get(base).getUi().highlightException(c, ex);
      }

    });

    try {
      defaultDriveEngine = (IDriveEngine) ScriptingEngine.inlineFileScriptRun(c, null);
      device.setWalkingDriveEngine(defaultDriveEngine);
    } catch (Exception ex) {
      MobileBaseCadManager.get(base).getUi().highlightException(c, ex);
    }
  }

  public static MobileBase initializeScripts(MobileBase base) {
    if (map.get(base) == null)
      map.put(base, new MobileBaseLoader(base));

    return base;
  }

  public static MobileBase fromGit(String id, String file) throws Exception {
    String xmlContent = ScriptingEngine.codeFromGit(id, file)[0];
    MobileBase mb = new MobileBase(IOUtils.toInputStream(xmlContent, "UTF-8"));

    mb.setGitSelfSource(new String[] {id, file});
    // ConnectionManager.addConnection(mb, mb.getScriptingName());
    return initializeScripts(mb);
  }

  public static MobileBaseLoader get(MobileBase base) {

    return map.get(initializeScripts(base));
  }

  public MobileBase getBase() {
    return base;
  }

  public void setBase(MobileBase base) {
    this.base = base;
  }

}
