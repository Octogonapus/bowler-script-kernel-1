package com.neuronrobotics.bowlerstudio.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;

public class ConfigurationDatabase {

  private static final String repo = "BowlerStudioConfiguration";
  private static final String HTTPS_GITHUB_COM_NEURON_ROBOTICS_BOWLER_STUDIO_CONFIGURATION_GIT =
      "https://github.com/CommonWealthRobotics/" + repo + ".git";
  private static final Type TT_mapStringString
      = new TypeToken<HashMap<String, HashMap<String, Object>>>() {
  }.getType();
  private static String gitSource = null; // madhephaestus
  private static String dbFile = "database.json";
  private static boolean checked;
  private static Map<String, HashMap<String, Object>> database = null;
  //chreat the gson object, this is the parsing factory
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

  public static Object getObject(String paramsKey, String objectKey, Object defaultValue) {
    if (getParamMap(paramsKey).get(objectKey) == null) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Cant find: " + paramsKey + ":" + objectKey);
      setObject(paramsKey, objectKey, defaultValue);
    }
    return getParamMap(paramsKey).get(objectKey);
  }

  public static Map<String, Object> getParamMap(String paramsKey) {
    getDatabase().computeIfAbsent(paramsKey, k -> new HashMap<>());
    return getDatabase().get(paramsKey);
  }

  /**
   * Set the value of an object in the map.
   *
   * @param paramsKey Key for the map
   * @param objectKey Key for the object
   * @param value     New object
   * @return The previous value for the object key
   */
  public static Object setObject(String paramsKey, String objectKey, Object value) {
    return getParamMap(paramsKey).put(objectKey, value);
  }

  /**
   * Remove and object from the map.
   *
   * @param paramsKey Key for the map
   * @param objectKey Key for the object
   * @return The previous value for the object key
   */
  public static Object removeObject(String paramsKey, String objectKey) {
    return getParamMap(paramsKey).remove(objectKey);
  }

  public static void save() {
    String writeOut;
    getDatabase();
    //synchronized(database){
    writeOut = gson.toJson(database, TT_mapStringString);
    //}

    try {
      ScriptingEngine.pushCodeToGit(getGitSource(),
          ScriptingEngine.getFullBranch(getGitSource()),
          getDbFile(),
          writeOut,
          "Saving database");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, HashMap<String, Object>> getDatabase() {
    if (database != null) {
      return database;
    }

    try {
      database = (HashMap<String, HashMap<String, Object>>)
          ScriptingEngine.inlineFileScriptRun(loadFile(), null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (database == null) {
      database = new HashMap<>();
    }

    return database;
  }

  public static File loadFile() throws Exception {
    return ScriptingEngine.fileFromGit(getGitSource(), getDbFile());
  }

  public static String getGitSource() throws Exception {
    if (!checked) {
      checked = true;

      if (ScriptingEngine.hasNetwork() && ScriptingEngine.isLoginSuccess()) {
        ScriptingEngine.setAutoupdate(true);
        org.kohsuke.github.GitHub github = ScriptingEngine.getGithub();
        GHMyself self = github.getMyself();
        Map<String, GHRepository> myPublic = self.getAllRepositories();

        for (Map.Entry<String, GHRepository> entry : myPublic.entrySet()) {
          if (entry.getKey().contentEquals(repo)
              && entry.getValue().getOwnerName().equals(self.getName())) {
            setRepo(entry.getValue());
          }
        }

        if (gitSource == null) {
          GHRepository defaultRep = github.getRepository("CommonWealthRobotics/" + repo);
          GHRepository forkedRep = defaultRep.fork();
          setRepo(forkedRep);
        }
      } else {
        ConfigurationDatabase
            .setGitSource(HTTPS_GITHUB_COM_NEURON_ROBOTICS_BOWLER_STUDIO_CONFIGURATION_GIT);
      }
    }

    return gitSource;
  }

  public static void setGitSource(String myAssets) {
    database = null;
    gitSource = myAssets;
    getDatabase();
  }

  private static void setRepo(GHRepository forkedRep) {
    String myAssets = forkedRep.getGitTransportUrl().replaceAll("git://", "https://");
    setGitSource(myAssets);
  }

  public static String getDbFile() {
    return dbFile;
  }

  public static void setDbFile(String dbFile) {
    ConfigurationDatabase.dbFile = dbFile;
    setGitSource(gitSource);
  }

}
