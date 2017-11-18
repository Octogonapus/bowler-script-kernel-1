package com.neuronrobotics.bowlerstudio.vitamins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Purchasing {

  private static final Map<String, HashMap<String, HashMap<String, PurchasingData>>> databaseSet =
      new HashMap<>();
  private static final String defaultgitRpoDatabase
      = "https://github.com/CommonWealthRobotics/Hardware-Purchasing.git";
  private static String jsonRootDir = "json/";
  private static String gitRpoDatabase = defaultgitRpoDatabase;
  //Create the type, this tells GSON what datatypes to instantiate when parsing and saving the json
  private static Type TT_mapStringString
      = new TypeToken<HashMap<String, HashMap<String, PurchasingData>>>() {
  }.getType();
  //chreat the gson object, this is the parsing factory
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

  public static HashMap<String, PurchasingData> getConfiguration(String type, String id) {
    HashMap<String, HashMap<String, PurchasingData>> database = getDatabase(type);
    database.computeIfAbsent(id, k -> new HashMap<>());
    return database.get(id);
  }

  public static void saveDatabase(String type) throws Exception {
    // Save contents and publish them
    String jsonString = makeJson(type);
    try {
      ScriptingEngine.pushCodeToGit(
          getGitRpoDatabase(),// git repo, change this if you fork this demo
          ScriptingEngine.getFullBranch(getGitRpoDatabase()), // branch or tag
          getRootFolder() + type + ".json", // local path to the file in git
          jsonString, // content of the file
          "Pushing changed Database");//commit message

    } catch (org.eclipse.jgit.api.errors.TransportException ex) {
      System.out.println("You need to fork " + getGitRpoDatabase() + " to have permission to save");
      System.out.println("You do not have permission to push to this repo, change the GIT repo to"
          + " your fork with setGitRpoDatabase(String gitRpoDatabase) ");
      throw ex;
    }

  }

  public static String makeJson(String type) {
    return gson.toJson(getDatabase(type), TT_mapStringString);
  }

  public static void newVitamin(String type, String id) throws Exception {
    HashMap<String, HashMap<String, PurchasingData>> database = getDatabase(type);
    if (database.keySet().size() > 0) {
      String exampleKey = null;

      for (String key : database.keySet()) {
        if (!key.contains("meta")) {
          exampleKey = key;
        }
      }

      if (exampleKey != null) {
        // this database has examples, load an example
        HashMap<String, PurchasingData> exampleConfiguration = getConfiguration(type, exampleKey);
        HashMap<String, PurchasingData> newConfig = getConfiguration(type, id);
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
                                  PurchasingData parameter) throws Exception {
    HashMap<String, PurchasingData> config = getConfiguration(type, id);
    config.put(parameterName, parameter);
  }

  public static HashMap<String, HashMap<String, PurchasingData>> getDatabase(String type) {
    if (databaseSet.get(type) == null) {
      // we are using the default vitamins configuration
      //https://github.com/madhephaestus/Hardware-Dimensions.git

      // create some variables, including our database
      String jsonString;
      InputStream inPut;

      // attempt to load the JSON file from the GIt Repo and pars the JSON string
      File f;
      try {
        f = ScriptingEngine.fileFromGit(
                getGitRpoDatabase(),// git repo, change this if you fork this demo
                getRootFolder() + type + ".json"// File from within the Git repo
            );
        inPut = FileUtils.openInputStream(f);

        jsonString = IOUtils.toString(inPut);
        // perform the GSON parse
        HashMap<String, HashMap<String, PurchasingData>> database =
            gson.fromJson(jsonString, TT_mapStringString);

        if (database == null) {
          throw new RuntimeException("create a new one");
        }

        databaseSet.put(type, database);
      } catch (Exception e) {
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
      folder = ScriptingEngine.fileFromGit(
              getGitRpoDatabase(),// git repo, change this if you fork this demo
              getRootFolder() + "capScrew.json"
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
    HashMap<String, HashMap<String, PurchasingData>> database = getDatabase(type);
    Set<String> keys = database.keySet();

    for (String s : keys) {
      if (!s.contains("meta")) {
        types.add(s);
      }
    }

    return types;
  }

  public static ArrayList<String> listVitaminVariants(String type, String size) {
    ArrayList<String> types = new ArrayList<>();
    HashMap<String, PurchasingData> database = getDatabase(type).get(size);
    Set<String> keys = database.keySet();

    for (String variant : keys) {
      PurchasingData pd = database.get(variant);
      if (!variant.endsWith("variant-1")) {
        // exclude the stub generated purchasing data
        try {
          URL u = new URL(pd.getAPIUrl());
          HttpURLConnection huc = (HttpURLConnection) u.openConnection();
          huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
          huc.connect();
          huc.getResponseCode();
          huc.disconnect();
          u = new URL(pd.getCartUrl());
          huc = (HttpURLConnection) u.openConnection();
          huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
          huc.connect();
          huc.getResponseCode();
          huc.disconnect();
          types.add(variant);
        } catch (java.net.ConnectException ce) {
          // server or cart is not available, reject vitamin
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    return types;
  }

  public static PurchasingData get(String type, String size, String variant) {
    try {
      return getDatabase(type).get(size).get(variant);
    } catch (NullPointerException ex) {
      throw new RuntimeException("Vitamin "
          + type + " "
          + size + " "
          + variant + " does not exist");
    }
  }


  public static String getGitRpoDatabase() throws IOException {
    return gitRpoDatabase;
  }

  public static void setGitRpoDatabase(String gitRpoDatabase) {
    Purchasing.gitRpoDatabase = gitRpoDatabase;
  }

  public static String getJsonRootDir() {
    return jsonRootDir;
  }

  public static void setJsonRootDir(String jsonRootDir) {
    Purchasing.jsonRootDir = jsonRootDir;
  }

}
