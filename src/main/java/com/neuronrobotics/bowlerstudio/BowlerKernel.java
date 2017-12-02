package com.neuronrobotics.bowlerstudio;

import com.google.common.base.Throwables;

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.imageprovider.OpenCVLoader;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jline.ConsoleReader;
import jline.Terminal;

public class BowlerKernel {

  private static File historyFile = new File(
      ScriptingEngine.getWorkspace().getAbsolutePath() + "/bowler.history");

  static {
    historyFile = new File(ScriptingEngine.getWorkspace().getAbsolutePath() + "/bowler.history");
    ArrayList<String> history = new ArrayList<>();
    if (!historyFile.exists()) {
      try {
        historyFile.createNewFile();
      } catch (IOException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not create new history file.\n" + Throwables.getStackTraceAsString(e));
      }
      history.add("println SDKBuildInfo.getVersion()");
      history.add("for(int i=0;i<1000000;i++) { println dyio.getValue(0) }");
      history.add("dyio.setValue(0,128)");
      history.add("println dyio.getValue(0)");
      history.add("ScriptingEngine.inlineGistScriptRun(\"d4312a0787456ec27a2a\", \"helloWorld"
          + ".groovy\" , null)");
      history.add("DeviceManager.addConnection(new DyIO(ConnectionDialog.promptConnection()),"
          + "\"dyio\")");
      history.add("DeviceManager.addConnection(new DyIO(new SerialConnection(\"/dev/DyIO0\")),"
          + "\"dyio\")");
      history.add("shellType Clojure #Switches shell to Clojure");
      history.add("shellType Jython #Switches shell to Python");
      history.add("shellType Groovy #Switches shell to Groovy/Java");

      history.add("println \"Hello world!\"");


      writeHistory(history);
    }
  }

  /**
   * Print the correct program usage and exit.
   */
  private static void fail() {
    System.err
        .println("Usage: \r\njava -jar BowlerScriptKernel.jar -s <file 1> .. <file n> # This will"
            + " loadJNI one script after the next ");
    System.err
        .println("java -jar BowlerScriptKernel.jar -p <file 1> .. <file n> # This will loadJNI one "
            + "script then take the list of objects returned and pss them to the next script as "
            + "its 'args' variable ");
    System.err
        .println("java -jar BowlerScriptKernel.jar -r <Groovy Jython or Clojure> (Optional)(-s or"
            + " -p)<file 1> .. <file n> # This will start a shell in the requested langauge and "
            + "run the files provided. ");

    System.exit(1);
  }

  /**
   * Start the REPL.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      fail();
    }

    OpenCVLoader.loadJNI(); // Loads the OpenCV JNI (java native interface)
    //    File servo = ScriptingEngine.fileFromGit(
    //        "https://github.com/CommonWealthRobotics/BowlerStudioVitamins.git",
    //        "BowlerStudioVitamins/stl/servo/smallservo.stl");
    //    ArrayList<CSG> cad = (ArrayList<CSG>) ScriptingEngine.inlineGistScriptRun(
    //        "4814b39ee72e9f590757",
    //        "javaCad.groovy",
    //        null);
    //    LoggerUtilities.getLogger().log(Level.INFO,
    //        servo.exists() + " exists: " + servo);

    boolean startLoadingScripts = false;
    Object ret = null;
    for (String s : args) {
      if (startLoadingScripts) {
        try {
          ret = ScriptingEngine.inlineFileScriptRun(new File(s), null);
        } catch (Error e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not run script.\n" + Throwables.getStackTraceAsString(e));
          fail();
        }
      }
      if (s.contains("script") || s.contains("-s")) {
        startLoadingScripts = true;
      }
    }
    startLoadingScripts = false;

    for (String s : args) {
      if (startLoadingScripts) {
        try {
          ret = ScriptingEngine.inlineFileScriptRun(new File(s), (ArrayList<Object>) ret);
        } catch (Error e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not run script.\n" + Throwables.getStackTraceAsString(e));
          fail();
        }
      }

      if (s.contains("pipe") || s.contains("-p")) {
        startLoadingScripts = true;
      }
    }

    boolean runShell = false;
    String groovy = "Groovy";
    String shellTypeStorage = groovy;
    for (String elem : args) {
      if (runShell) {
        try {
          shellTypeStorage = elem;
        } catch (Exception e) {
          shellTypeStorage = groovy;
        }
        break;
      }

      if (elem.contains("repl") || elem.contains("-r")) {
        runShell = true;
      }
    }

    LoggerUtilities.getLogger().log(Level.INFO,
        "Starting Bowler REPL in language: " + shellTypeStorage);

    if (!Terminal.getTerminal().isSupported()) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Terminal not supported: " + Terminal.getTerminal());
    }

    ConsoleReader reader = new ConsoleReader();
    reader.addTriggeredAction(Terminal.CTRL_C, e -> System.exit(0));

    if (!historyFile.exists()) {
      historyFile.createNewFile();
      reader.getHistory().addToHistory("println SDKBuildInfo.getVersion()");
      reader.getHistory().addToHistory("for(int i=0;i<100;i++) { println dyio.getValue(0) }");
      reader.getHistory().addToHistory("dyio.setValue(0,128)");
      reader.getHistory().addToHistory("println dyio.getValue(0)");
      reader.getHistory().addToHistory("ScriptingEngine.inlineGistScriptRun"
          + "(\"d4312a0787456ec27a2a\", \"helloWorld.groovy\" , null)");
      reader.getHistory().addToHistory("DeviceManager.addConnection(new DyIO(ConnectionDialog"
          + ".promptConnection()),\"dyio\")");
      reader.getHistory().addToHistory("DeviceManager.addConnection(new DyIO(new SerialConnection"
          + "(\"/dev/DyIO0\")),\"dyio\")");
      reader.getHistory().addToHistory("BowlerKernel.speak(\"Text to speech works like this\")");
      reader.getHistory().addToHistory("println \"Hello world!\"");
      writeHistory(reader.getHistory().getHistoryList());
    } else {
      List<String> history = loadHistory();
      for (String elem : history) {
        reader.getHistory().addToHistory(elem);
      }
    }
    reader.setBellEnabled(false);
    reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));

    Runtime.getRuntime().addShutdownHook(new Thread(() ->
        writeHistory(reader.getHistory().getHistoryList())));

    String line;
    try {
      while ((line = reader.readLine("Bowler " + shellTypeStorage + "> ")) != null) {
        if (line.equalsIgnoreCase("quit")
            || line.equalsIgnoreCase("exit")) {
          break;
        }

        if (line.equalsIgnoreCase("history")
            || line.equalsIgnoreCase("h")) {
          List<String> historyList = reader.getHistory().getHistoryList();
          for (String string : historyList) {
            LoggerUtilities.getLogger().log(Level.INFO, string);
          }
          continue;
        }

        if (line.startsWith("shellType")) {
          try {
            shellTypeStorage = line.split(" ")[1];
          } catch (Exception e) {
            shellTypeStorage = groovy;
          }
          continue;
        }

        try {
          ret = ScriptingEngine.inlineScriptStringRun(line, null,
              shellTypeStorage);
          if (ret != null) {
            LoggerUtilities.getLogger().log(Level.INFO,
                ret.toString());
          }
        } catch (Error e) {
          LoggerUtilities.getLogger().log(Level.SEVERE,
              "Error: " + Throwables.getStackTraceAsString(e));
        } catch (Exception e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not run script.\n" + Throwables.getStackTraceAsString(e));
        }
      }
    } catch (Exception e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Exception.\n" + Throwables.getStackTraceAsString(e));
    }

  }

  /**
   * Load history from the history file.
   *
   * @return History
   * @throws IOException FileReader IOException
   */
  public static ArrayList<String> loadHistory() throws IOException {
    ArrayList<String> history = new ArrayList<>();
    // Construct BufferedReader from FileReader
    BufferedReader br = new BufferedReader(new FileReader(historyFile));

    String line;
    while ((line = br.readLine()) != null) {
      history.add(line);
    }
    br.close();
    return history;
  }

  /**
   * Write history to the history file.
   *
   * @param history History
   */
  public static void writeHistory(List<String> history) {
    LoggerUtilities.getLogger().log(Level.INFO,
        "Saving history.");

    FileOutputStream fos;
    try {
      fos = new FileOutputStream(historyFile);
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
      for (String s : history) {
        bw.write(s);
        bw.newLine();
      }

      bw.close();
    } catch (FileNotFoundException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not construct FileOutputStream to write history.\n"
              + Throwables.getStackTraceAsString(e));
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not write to history.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  /**
   * Speak a string using default parameters.
   *
   * @param msg Message to say
   * @return 0
   */
  public static int speak(String msg) {
    return speak(msg, 175.0, 120.0, 41.0, 1.0, 1.0);
  }

  /**
   * Speak a string.
   *
   * @param msg    Message to say
   * @param rate   Speech rate
   * @param pitch  Speech pitch
   * @param range  Speech range
   * @param shift  Speech shift
   * @param volume Speech volume
   * @return 0
   */
  public static int speak(String msg,
                          Double rate,
                          Double pitch,
                          Double range,
                          Double shift,
                          Double volume) {
    System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal"
        + ".KevinVoiceDirectory");
    VoiceManager voiceManager = VoiceManager.getInstance();
    com.sun.speech.freetts.Voice voice = voiceManager.getVoice("kevin16");

    LoggerUtilities.getLogger().log(Level.INFO,
        "Speaking.\nRate: " + rate
            + "\nPitch hertz: " + pitch
            + "\nPitch Range: " + range
            + "\nPitch Shift: " + shift
            + "\nVolume: " + volume);

    if (voice != null) {
      voice.setRate(rate.floatValue());
      voice.setPitch(pitch.floatValue());
      voice.setPitchRange(range.floatValue());
      voice.setPitchShift(shift.floatValue());
      voice.setVolume(volume.floatValue());
      voice.allocate();
      voice.speak(msg);
      voice.deallocate();
    } else {
      LoggerUtilities.getLogger().log(Level.INFO,
          "All voices available:");

      com.sun.speech.freetts.Voice[] voices = voiceManager.getVoices();
      for (Voice voice1 : voices) {
        LoggerUtilities.getLogger().log(Level.INFO,
            "    " + voice1.getName() + " (" + voice1.getDomain() + " domain)");
      }
    }

    // WordNumSyls feature =
    // (WordNumSyls)voice.getFeatureProcessor("word_numsyls");
    // if(feature!=null)
    // try {
    //
    // System.out.println("Syllables# = "+feature.process(null));
    // } catch (ProcessException e) {
    // e.printStackTrace();
    // }
    //
    return 0;
  }

}
