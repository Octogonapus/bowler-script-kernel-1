package com.neuronrobotics.bowlerstudio.scripting;

import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.DeviceManager;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

public class GroovyHelper implements IScriptingLanguage, IScriptingLanguageDebugger {

  private Object inline(Object code, ArrayList<Object> args) throws Exception {
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.addCompilationCustomizers(new ImportCustomizer()
        .addStarImports(ScriptingEngine.getImports())
        .addStaticStars(
            "com.neuronrobotics.sdk.util.ThreadUtil",
            "eu.mihosoft.vrl.v3d.Transform",
            "com.neuronrobotics.bowlerstudio.vitamins.Vitamins")
    );

    Binding binding = new Binding();
    for (String pm : DeviceManager.listConnectedDevice()) {
      BowlerAbstractDevice bad = DeviceManager.getSpecificDevice(null, pm);
      try {
        // groovy needs the objects cas to their actual type before passing into the script
        binding.setVariable(bad.getScriptingName(),
            Class.forName(bad.getClass().getName()).cast(bad));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    binding.setVariable("args", args);


    GroovyShell shell = new GroovyShell(GroovyHelper.class.getClassLoader(), binding, cc);
    Script script;
    if (String.class.isInstance(code)) {
      script = shell.parse((String) code);
    } else if (File.class.isInstance(code)) {
      script = shell.parse((File) code);
    } else {
      return null;
    }

    return script.run();
  }

  @Override
  public String getShellType() {
    return "Groovy";
  }

  @Override
  public Object inlineScriptRun(File code, ArrayList<Object> args) throws Exception {
    return inline(code, args);
  }

  @Override
  public Object inlineScriptRun(String code, ArrayList<Object> args) throws Exception {
    return inline(code, args);
  }

  @Override
  public boolean getIsTextFile() {
    return true;
  }

  @Override
  public ArrayList<String> getFileExtenetion() {
    return new ArrayList<>(Arrays.asList("java", "groovy"));
  }

  @Override
  public IDebugScriptRunner compileDebug(File file) {
    return () -> new String[]{"fileame.groovy", "345"};
  }

}
