package com.neuronrobotics.bowlerstudio.vitamins;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.STL;
import eu.mihosoft.vrl.v3d.parametrics.LengthParameter;
import eu.mihosoft.vrl.v3d.parametrics.StringParameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;

public class Vitamins {

  private static final Map<String, CSG> fileLastLoaded = new HashMap<>();
  private static final Map<String, HashMap<String, HashMap<String, Object>>> databaseSet =
      new HashMap<>();
  private static final String defaultgitRpoDatabase
      = "https://github.com/madhephaestus/Hardware-Dimensions.git";
  private static String jsonRootDir = "json/";
  private static String gitRpoDatabase = defaultgitRpoDatabase;
  //Create the type, this tells GSON what datatypes to instantiate when parsing and saving the json
  private static Type TT_mapStringString
      = new TypeToken<HashMap<String, HashMap<String, Object>>>() {
  }.getType();
  //chreat the gson object, this is the parsing factory
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
  private static boolean checked;

  public static CSG get(File resource) {
    if (fileLastLoaded.get(resource.getAbsolutePath()) == null) {
      // forces the first time the files are accessed by the application to pull an update
      try {
        fileLastLoaded.put(resource.getAbsolutePath(), STL.file(resource.toPath()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return fileLastLoaded.get(resource.getAbsolutePath()).clone();
  }

  public static CSG get(String type, String id, String purchasingVariant) throws Exception {
    String key = type + id + purchasingVariant;
    if (fileLastLoaded.get(key) == null) {
      PurchasingData purchasingData = Purchasing.get(type, id, purchasingVariant);
      for (String variable : purchasingData.getVariantParameters().keySet()) {
        double data = purchasingData.getVariantParameters().get(variable);
        LengthParameter parameter
            = new LengthParameter(variable, data, (ArrayList<Double>) Arrays.asList(data, data));
        parameter.setMM(data);
      }

      try {
        fileLastLoaded.put(key, get(type, id));
      } catch (Exception e) {
        e.printStackTrace();

        gitRpoDatabase = defaultgitRpoDatabase;
        databaseSet.clear();
        fileLastLoaded.clear();
        return get(type, id);
      }
    }

    return fileLastLoaded.get(type + id);
  }

  public static CSG get(String type, String id) throws Exception {
    return get(type, id, 0);
  }

  public static CSG get(String type, String id, int depthGauge) throws Exception {
    String key = type + id;

    try {
      CSG newVitamin;
      HashMap<String, Object> script = getMeta(type);
      StringParameter size = new StringParameter(type + " Default",
          id,
          Vitamins.listVitaminSizes(type));
      size.setStrValue(id);
      Object file = script.get("scriptGit");
      Object repo = script.get("scriptFile");
      if (file != null && repo != null) {
        ArrayList<Object> servoMeasurements = new ArrayList<>();
        servoMeasurements.add(id);
        newVitamin = (CSG) ScriptingEngine.gitScriptRun(
            script.get("scriptGit").toString(), // git location of the library
            script.get("scriptFile").toString(), // file to loadJNI
            servoMeasurements
        );
        return newVitamin;
      } else {
        Log.error(key + " Failed to loadJNI from script");
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      gitRpoDatabase = defaultgitRpoDatabase;
      databaseSet.clear();
      fileLastLoaded.clear();
      if (depthGauge < 2) {
        return get(type, id, depthGauge + 1);
      } else {
        return null;
      }
    }
  }


  public static HashMap<String, Object> getMeta(String type) {
    return getConfiguration(type, "meta");
  }

  public static void setScript(String type, String git, String file) throws Exception {
    setParameter(type, "meta", "scriptGit", git);
    setParameter(type, "meta", "scriptFile", file);
  }

  public static HashMap<String, Object> getConfiguration(String type, String id) {
    HashMap<String, HashMap<String, Object>> database = getDatabase(type);
    database.computeIfAbsent(id, k -> new HashMap<>());
    return database.get(id);
  }

  public static String makeJson(String type) {
    return gson.toJson(getDatabase(type), TT_mapStringString);
  }

  public static void saveDatabase(String type) throws Exception {
    // Save contents and publish them
    String jsonString = makeJson(type);
    try {
      ScriptingEngine.pushCodeToGit(
          getGitRepoDatabase(),// git repo, change this if you fork this demo
          ScriptingEngine.getFullBranch(getGitRepoDatabase()), // branch or tag
          getRootFolder() + type + ".json", // local path to the file in git
          jsonString, // content of the file
          "Pushing changed Database");//commit message
    } catch (org.eclipse.jgit.api.errors.TransportException ex) {
      System.out.println(
          "You need to fork " + defaultgitRpoDatabase + " to have permission to save");
      System.out.println("You do not have permission to push to this repo, change the GIT repo to"
          + " your fork with setGitRpoDatabase(String gitRpoDatabase) ");
      throw ex;
    }
  }

  public static void newVitamin(String type, String id) throws Exception {
    HashMap<String, HashMap<String, Object>> database = getDatabase(type);
    if (database.keySet().size() > 0) {
      String exampleKey = null;
      for (String key : database.keySet()) {
        if (!key.contains("meta")) {
          exampleKey = key;
        }
      }

      if (exampleKey != null) {
        // this database has examples, loadJNI an example
        HashMap<String, Object> exampleConfiguration = getConfiguration(type, exampleKey);
        HashMap<String, Object> newConfig = getConfiguration(type, id);
        for (String key : exampleConfiguration.keySet()) {
          newConfig.put(key, exampleConfiguration.get(key));
        }
      }
    }

    getConfiguration(type, id);
  }

  public static void setParameter(String type,
                                  String id,
                                  String parameterName,
                                  Object parameter) throws Exception {
    HashMap<String, Object> config = getConfiguration(type, id);
    try {
      config.put(parameterName, Double.parseDouble(parameter.toString()));
    } catch (NumberFormatException ex) {
      config.put(parameterName, parameter);
    }
  }

  public static HashMap<String, HashMap<String, Object>> getDatabase(String type) {
    if (databaseSet.get(type) == null) {
      // we are using the default vitamins configuration
      //https://github.com/madhephaestus/Hardware-Dimensions.git

      // create some variables, including our database
      String jsonString;
      InputStream inPut = null;

      // attempt to loadJNI the JSON file from the GIt Repo and pars the JSON string
      File file;
      try {
        file = ScriptingEngine.fileFromGit(
            getGitRepoDatabase(),// git repo, change this if you fork this demo
            getRootFolder() + type + ".json"// File from within the Git repo
        );

        inPut = FileUtils.openInputStream(file);
        jsonString = IOUtils.toString(inPut);

        // perform the GSON parse
        HashMap<String, HashMap<String, Object>> database =
            gson.fromJson(jsonString, TT_mapStringString);

        if (database == null) {
          throw new RuntimeException("create a new one");
        }
        databaseSet.put(type, database);

        for (String key : databaseSet.get(type).keySet()) {
          HashMap<String, Object> conf = database.get(key);
          for (String confKey : conf.keySet()) {
            try {
              double num = Double.parseDouble(conf.get(confKey).toString());
              conf.put(confKey, num);
            } catch (NumberFormatException ex) {
              // leave as a string
              conf.put(confKey, conf.get(confKey).toString());
            }
          }
        }

      } catch (Exception e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Exception in getDatabase.\n" + Throwables.getStackTraceAsString(e));
        databaseSet.put(type, new HashMap<>());
      }
    }

    return databaseSet.get(type);
  }

  private static String getRootFolder() {
    return getJsonRootDir();
  }

  public static ArrayList<String> listVitaminTypes() {
    ArrayList<String> types = new ArrayList<>();
    File folder;

    try {
      folder = ScriptingEngine
          .fileFromGit(
              getGitRepoDatabase(),// git repo, change this if you fork this demo
              getRootFolder() + "hobbyServo.json"
          );
      File[] listOfFiles = folder.getParentFile().listFiles();

      for (File f : listOfFiles) {
        if (!f.isDirectory() && f.getName().endsWith(".json")) {
          types.add(f.getName().substring(0, f.getName().indexOf(".json")));
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return types;
  }

  public static ArrayList<String> listVitaminSizes(String type) {
    ArrayList<String> types = new ArrayList<>();
    HashMap<String, HashMap<String, Object>> database = getDatabase(type);
    Set<String> keys = database.keySet();

    for (String s : keys) {
      if (!s.contains("meta")) {
        types.add(s);
      }
    }

    return types;
  }

  public static String getGitRepoDatabase() throws IOException {
    if (!checked) {
      checked = true;

      if (ScriptingEngine.getLoginID() != null) {
        ScriptingEngine.setAutoupdate(true);
        org.kohsuke.github.GitHub github = ScriptingEngine.getGithub();
        GHMyself self = github.getMyself();
        Map<String, GHRepository> myPublic = self.getAllRepositories();

        for (String myRepo : myPublic.keySet()) {
          GHRepository ghrepo = myPublic.get(myRepo);

          if (myRepo.contentEquals("Hardware-Dimensions")
              && ghrepo.getOwnerName().contentEquals(self.getLogin())) {
            String myAssets = ghrepo
                .getGitTransportUrl()
                .replaceAll("git://", "https://");
            setGitRepoDatabase(myAssets);
          }
        }
      }
    }

    return gitRpoDatabase;
  }

  public static void setGitRepoDatabase(String gitRpoDatabase) {
    Vitamins.gitRpoDatabase = gitRpoDatabase;
    databaseSet.clear();
    fileLastLoaded.clear();
  }

  public static String getJsonRootDir() {
    return jsonRootDir;
  }

  public static void setJsonRootDir(String jsonRootDir) throws IOException {
    Vitamins.jsonRootDir = jsonRootDir;
    setGitRepoDatabase(getGitRepoDatabase());
  }

}
