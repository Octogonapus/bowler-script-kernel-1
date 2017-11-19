package com.neuronrobotics.bowlerstudio.scripting;

import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import com.neuronrobotics.sdk.util.ThreadUtil;
import eu.mihosoft.vrl.v3d.parametrics.CSGDatabase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import javafx.scene.web.WebEngine;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GitHub;

public class ScriptingEngine {

  private static final String[] imports = new String[]{
      "java.nio.file",
      "java.util",
      "java.awt.image",
      "javafx.scene.text",
      "javafx.scene",
      "javafx.scene.control",
      "eu.mihosoft.vrl.v3d",
      "eu.mihosoft.vrl.v3d.svg",
      "eu.mihosoft.vrl.v3d.samples",
      "eu.mihosoft.vrl.v3d.parametrics",
      "com.neuronrobotics.imageprovider",
      "com.neuronrobotics.sdk.addons.kinematics.xml",
      "com.neuronrobotics.sdk.addons.kinematics",
      "com.neuronrobotics.sdk.dyio.peripherals",
      "com.neuronrobotics.sdk.dyio",
      "com.neuronrobotics.sdk.common",
      "com.neuronrobotics.sdk.ui",
      "com.neuronrobotics.sdk.util",
      "com.neuronrobotics.sdk.serial",
      "com.neuronrobotics.sdk.addons.kinematics",
      "com.neuronrobotics.sdk.addons.kinematics.math",
      "com.neuronrobotics.sdk.addons.kinematics.gui",
      "com.neuronrobotics.sdk.config",
      "com.neuronrobotics.bowlerkernel",
      "com.neuronrobotics.bowlerstudio",
      "com.neuronrobotics.bowlerstudio.scripting",
      "com.neuronrobotics.bowlerstudio.tabs",
      "com.neuronrobotics.bowlerstudio.physics",
      "com.neuronrobotics.bowlerstudio.physics",
      "com.neuronrobotics.bowlerstudio.vitamins",
      "com.neuronrobotics.bowlerstudio.creature",
      "com.neuronrobotics.bowlerstudio.threed"
  };

  private static final int TIME_TO_WAIT_BETWEEN_GIT_PULL = 100000;
  private static final Map<String, Long> fileLastLoaded = new HashMap<>();
  private static boolean hasnetwork = false;
  private static boolean autoupdate = false;
  private static boolean loginSuccess = false;

  private static GitHub github;
  private static HashMap<String, File> filesRun = new HashMap<>();
  private static File creds = null;
  private static File workspace;
  private static File lastFile;
  private static String loginID = null;
  private static String password = null;
  private static CredentialsProvider cp;
  private static ArrayList<IGithubLoginListener> loginListeners = new ArrayList<>();

  private static HashMap<String, IScriptingLanguage> langauges = new HashMap<>();

  private static IGitHubLoginManager loginManager = username -> {
    new RuntimeException("Login required").printStackTrace();

    if (username == null) {
      username = "";
    }

    LoggerUtilities.getLogger().log(Level.INFO,
        "#Github Login Prompt#\n"
            + "For anonymous mode, hit enter twice.\n"
            + "Github Username: (" + username + ")");
    // create a scanner so we can read the command-line input
    BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));

    String[] creds = new String[]{"", ""};
    do {
      try {
        creds[0] = buf.readLine();
      } catch (IOException e) {
        return null;
      }
      if ("" .equals(creds[0]) && "" .equals(username)) {
        LoggerUtilities.getLogger().log(Level.INFO,
            "No username, using anonymous login.");
        return null;
      } else {
        creds[0] = username;
      }
    } while (creds[0] == null);

    LoggerUtilities.getLogger().log(Level.INFO,
        "Github password: ");
    try {
      creds[1] = buf.readLine();
      if ("" .equals(creds[1])) {
        LoggerUtilities.getLogger().log(Level.INFO,
            "No username, using anonymous login.");
      }
    } catch (IOException e) {
      return null;
    }
    return creds;
  };

  static {
    tryGitHubConnection();

    workspace = new File(System.getProperty("user.home") + "/bowler-workspace/");
    if (!workspace.exists()) {
      workspace.mkdir();
    }

    try {
      loadLoginData();
    } catch (IOException e) {
      e.printStackTrace();
    }

    addScriptingLanguage(new ClojureHelper());
    addScriptingLanguage(new GroovyHelper());
    addScriptingLanguage(new JythonHelper());
    addScriptingLanguage(new RobotHelper());
    addScriptingLanguage(new JsonRunner());
    addScriptingLanguage(new ArduinoLoader());
  }

  private static void tryGitHubConnection() {
    try {
      final URL url = new URL("http://github.com");
      final URLConnection conn = url.openConnection();
      conn.connect();
      conn.getInputStream();
      hasnetwork = true;
    } catch (Exception e) {
      //No access to the server, so run off of caches gists
      hasnetwork = false;
    }
  }

  /**
   * This interface is for adding additional language support.
   *
   * @param code file content of the code to be executed
   * @param args the incoming arguments as a list of objects
   * @return the objects returned form the code that ran
   */
  public static Object inlineScriptRun(File code,
                                       ArrayList<Object> args,
                                       String shellTypeStorage) throws Exception {
    filesRun.putIfAbsent(code.getName(), code);

    if (langauges.get(shellTypeStorage) != null) {
      return langauges.get(shellTypeStorage).inlineScriptRun(code, args);
    }

    return null;
  }

  /**
   * This interface is for adding additional language support.
   *
   * @param line the text content of the code to be executed
   * @param args the incoming arguments as a list of objects
   * @return the objects returned form the code that ran
   */
  public static Object inlineScriptStringRun(String line,
                                             ArrayList<Object> args,
                                             String shellTypeStorage) throws Exception {
    if (langauges.get(shellTypeStorage) != null) {
      return langauges.get(shellTypeStorage).inlineScriptRun(line, args);
    }

    return null;
  }

  private static void loadLoginData() throws IOException {
    if (loginID == null && getCreds().exists() && hasnetwork) {
      try {
        String line;

        InputStream fis = new FileInputStream(getCreds().getAbsolutePath());
        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);

        while ((line = br.readLine()) != null) {
          if (line.startsWith("login") || line.startsWith("username")) {
            loginID = line.split("=")[1];
          }
          if (line.startsWith("password")) {
            password = line.split("=")[1];
          }
        }

        if (password != null && loginID != null) {
          // password loaded, we can now autoupdate
          ScriptingEngine.setAutoupdate(true);
        }

        if (cp == null) {
          cp = new UsernamePasswordCredentialsProvider(loginID, password);
        }
      } catch (Exception e) {
        logout();
      }
    }

  }

  public static void addScriptingLanguage(IScriptingLanguage lang) {
    langauges.put(lang.getShellType(), lang);
  }

  public static void addIGithubLoginListener(IGithubLoginListener listener) {
    if (!loginListeners.contains(listener)) {
      loginListeners.add(listener);
    }
  }

  public static void removeIGithubLoginListener(IGithubLoginListener listener) {
    if (loginListeners.contains(listener)) {
      loginListeners.remove(listener);
    }
  }

  public static File getWorkspace() {
    return workspace;
  }

  public static String getShellType(String name) {
    for (IScriptingLanguage l : langauges.values()) {
      if (l.isSupportedFileExtenetion(name)) {
        return l.getShellType();
      }
    }

    return "Groovy";
  }

  public static String getLoginID() {
    return loginID;
  }

  public static void login() throws IOException {
    if (!hasnetwork) {
      return;
    }

    loginID = null;

    gitHubLogin();
  }

  public static void logout() throws IOException {
    if (getCreds() != null && getCreds().exists()) {
      Files.delete(getCreds().toPath());
    }

    setGithub(null);
    for (IGithubLoginListener listener : loginListeners) {
      listener.onLogout(loginID);
    }

    loginID = null;
  }

  public static GitHub setupAnyonmous() throws IOException {
    LoggerUtilities.getLogger().log(Level.INFO,
        "Using anonymous login, autoupdate disabled.");
    ScriptingEngine.setAutoupdate(false);
    logout();

    setGithub(GitHub.connectAnonymously());

    return getGithub();
  }

  public static String urlToGist(String in) {
    if (in.endsWith(".git")) {
      in = in.substring(0, in.lastIndexOf('.'));
    }

    String domain = in.split("//")[1];
    String[] tokens = domain.split("/");

    if (tokens[0].toLowerCase().contains("gist.github.com") && tokens.length >= 2) {
      try {
        String id = tokens[2].split("#")[0];
        LoggerUtilities.getLogger().log(Level.INFO, "Gist URL detected: " + id);
        return id;
      } catch (ArrayIndexOutOfBoundsException e) {
        try {
          String id = tokens[1].split("#")[0];
          LoggerUtilities.getLogger().log(Level.INFO, "Gist URL detected: " + id);
          return id;
        } catch (ArrayIndexOutOfBoundsException ex) {
          LoggerUtilities.getLogger().log(Level.INFO,
              "Parsing " + in + " failed to find gist");
          return "d4312a0787456ec27a2a";
        }
      }
    }

    return null;
  }

  private static List<String> returnFirstGist(String html) {
    ArrayList<String> ret = new ArrayList<>();
    Document doc = Jsoup.parse(html);
    Elements links = doc.select("script");

    for (Element e : links) {
      Attributes attributes = e.attributes();
      String source = attributes.get("src");

      if (source.contains("https://gist.github.com/")) {
        String js = source.split(".js")[0];
        String[] id = js.split("/");
        ret.add(id[id.length - 1]);
      }
    }

    return ret;
  }

  public static List<String> getCurrentGist(String addr, WebEngine engine) {
    String gist = urlToGist(addr);

    if (gist == null) {
      try {
        LoggerUtilities.getLogger().log(Level.INFO, "Non-Gist URL detected.");

        String html;
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter sw = new StringWriter();

        transformer.transform(new DOMSource(engine.getDocument()), new StreamResult(sw));
        html = sw.getBuffer().toString();

        return returnFirstGist(html);
      } catch (TransformerException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not make a new instance of TransformerFactory.\n"
                + Throwables.getStackTraceAsString(e));
      }
    }

    ArrayList<String> ret = new ArrayList<>();
    ret.add(gist);
    return ret;
  }

  public static GitHub gitHubLogin() throws IOException {
    String[] creds = loginManager.prompt(loginID);

    if (creds == null) {
      return setupAnyonmous();
    } else {
      if (creds[0].contains("@")) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "###ERROR Enter the Username not the Email Address###");
        return gitHubLogin();
      }
      if ("" .equals(creds[0]) || "" .equals(creds[1])) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "###No Username or password###");
        return setupAnyonmous();
      }
    }

    loginID = creds[0];
    password = creds[1];

    String content = "login=" + loginID + "\n";
    content += "password=" + password + "\n";
    PrintWriter out;
    try {
      out = new PrintWriter(getCreds().getAbsoluteFile());
      out.println(content);
      out.flush();
      out.close();
      runLogin();
    } catch (Exception e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Login failed");
      setGithub(null);
    }

    if (getGithub() == null) {
      ThreadUtil.wait(200);
      return gitHubLogin();
    }

    return getGithub();
  }

  public static void runLogin() throws IOException {
    setGithub(GitHub.connect());

    if (getGithub().isCredentialValid()) {
      cp = new UsernamePasswordCredentialsProvider(loginID, password);
      for (IGithubLoginListener l : loginListeners) {
        l.onLogin(loginID);
      }
      LoggerUtilities.getLogger().log(Level.INFO,
          "Success Login as " + loginID);
      setLoginSuccess(true);
    } else {
      LoggerUtilities.getLogger().log(Level.INFO,
          "Bad login credentials for " + loginID);
      setGithub(null);
      password = null;
    }
  }

  private static void waitForLogin() throws IOException, GitAPIException {
    tryGitHubConnection();

    if (!hasnetwork) {
      return;
    }

    if (getGithub() == null) {
      if (getCreds().exists()) {
        try {
          setGithub(GitHub.connect());
        } catch (IOException ex) {
          logout();
        }
      } else {
        getCreds().createNewFile();
      }

      if (getGithub() == null) {
        login();
      }
    }

    try {
      if (getGithub().getRateLimit().remaining < 2) {
        LoggerUtilities.getLogger().log(Level.INFO,
            "##Github Is Rate Limiting You## Disabling autoupdate");
        setAutoupdate(false);
      }
    } catch (IOException e) {
      logout();

    }

    loadLoginData();
  }

  public static void deleteRepo(String remoteURI) {
    File gitRepoFile = uriToFile(remoteURI);
    try {
      FileUtils.deleteDirectory(gitRepoFile.getParentFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void deleteCache() {
    try {
      FileUtils.deleteDirectory(new File(getWorkspace().getAbsoluteFile() + "/gistcache/"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void loadFilesToList(ArrayList<String> files, File directory, String extension) {
    for (final File fileEntry : directory.listFiles()) {
      if (fileEntry.getName().endsWith(".git")
          || fileEntry.getName().startsWith(".git")) {
        continue;// ignore git files
      }

      if (extension != null) {
        if (extension.length() > 0) {
          if (!fileEntry.getName().endsWith(extension)) {
            continue;// skip this file as it fails the filter
          }
        }
      }

      // from the user
      if (fileEntry.isDirectory()) {
        loadFilesToList(files, fileEntry, extension);
      } else {
        for (IScriptingLanguage l : langauges.values()) {
          if (l.isSupportedFileExtenetion(fileEntry.getName())) {
            files.add(findLocalPath(fileEntry));
            break;
          }
        }
      }
    }
  }

  public static ArrayList<String> filesInGit(String remote,
                                             String branch,
                                             String extension) throws Exception {
    ArrayList<String> files = new ArrayList<>();

    waitForLogin();
    File gistDir = cloneRepo(remote, branch);
    loadFilesToList(files, gistDir, extension);

    return files;

  }

  public static ArrayList<String> filesInGit(String remote) throws Exception {
    return filesInGit(remote, ScriptingEngine.getFullBranch(remote), null);
  }

  public static String getUserIdOfGist(String id) throws Exception {
    waitForLogin();
    LoggerUtilities.getLogger().log(Level.INFO,
        "Loading Gist: " + id);

    GHGist gist = getGithub().getGist(id);
    return gist.getOwner().getLogin();
  }

  public static File createFile(String git,
                                String fileName,
                                String commitMessage) throws Exception {
    pushCodeToGit(git, ScriptingEngine.getFullBranch(git), fileName, null, commitMessage);
    return fileFromGit(git, fileName);
  }

  public static void commit(String id,
                            String branch,
                            String fileName,
                            String content,
                            String commitMessage,
                            boolean flagNewFile) throws Exception {
    if (loginID == null) {
      login();
    }

    if (loginID == null) {
      return;// No login info means there is no way to publish
    }

    File gistDir = cloneRepo(id, branch);
    File desired = new File(gistDir.getAbsoluteFile() + "/" + fileName);

    String localPath = gistDir.getAbsolutePath();
    File gitRepoFile = new File(localPath + "/.git");

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    Git git = new Git(localRepo);
    try {                                                // latest version
      if (flagNewFile) {
        git.add().addFilepattern(fileName).call();
      }
      if (content != null) {
        OutputStream out = null;
        try {
          out = FileUtils.openOutputStream(desired, false);
          IOUtils.write(content, out);
          out.close();
        } finally {
          IOUtils.closeQuietly(out);
        }
      }

      git.commit().setAll(true).setMessage(commitMessage).call();
    } catch (Exception ex) {
      git.close();

      throw ex;
    }
    git.close();
    try {
      if (!desired.getName().contentEquals("csgDatabase.json")) {
        String[] gitID = ScriptingEngine.findGitTagFromFile(desired);
        String remoteURI = gitID[0];
        ArrayList<String> files = ScriptingEngine.filesInGit(remoteURI);
        for (String file : files) {
          if (file.contentEquals("csgDatabase.json")) {

            File dbFile = ScriptingEngine.fileFromGit(gitID[0], file);
            if (!CSGDatabase.getDbFile().equals(dbFile)) {
              CSGDatabase.setDbFile(dbFile);
            }
            CSGDatabase.saveDatabase();
            String next = new Scanner(dbFile).useDelimiter("\\Z").next();
            ScriptingEngine.commit(remoteURI, branch, file, next, "saving CSG database", false);
          }
        }
      }
    } catch (Exception e) {
      //ignore CSG database
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Exception in commit.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  public static void pushCodeToGit(String id,
                                   String branch,
                                   String fileName,
                                   String content,
                                   String commitMessage) throws Exception {
    if (loginID == null) {
      login();
    }

    if (loginID == null) {
      return;// No login info means there is no way to publish
    }

    File gistDir = cloneRepo(id, branch);
    File desired = new File(gistDir.getAbsoluteFile() + "/" + fileName);

    boolean flagNewFile = false;
    if (!desired.exists()) {
      desired.createNewFile();
      flagNewFile = true;
    }

    pushCodeToGit(id, branch, fileName, content, commitMessage, flagNewFile);
  }

  public static void pushCodeToGit(String id, String branch, String FileName, String content,
                                   String commitMessage,
                                   boolean flagNewFile) throws Exception {
    commit(id, branch, FileName, content, commitMessage, flagNewFile);
    if (loginID == null) {
      login();
    }

    if (loginID == null) {
      return; //No login info means there is no way to publish
    }

    File gistDir = cloneRepo(id, branch);
    File desired = new File(gistDir.getAbsoluteFile() + "/" + FileName);

    if (!hasnetwork && content != null) {
      overwriteFileWithString(desired, content);
      return;
    }

    waitForLogin();
    String localPath = gistDir.getAbsolutePath();
    File gitRepoFile = new File(localPath + "/.git");

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    Git git = new Git(localRepo);
    try {
      git.pull().setCredentialsProvider(cp).call();

      if (flagNewFile) {
        git.add().addFilepattern(FileName).call();
      }

      overwriteFileWithString(desired, content);
      git.push().setCredentialsProvider(cp).call();

      LoggerUtilities.getLogger().log(Level.INFO,
          "PUSH OK! file: " + desired);
    } catch (Exception ex) {
      String[] gitID = ScriptingEngine.findGitTagFromFile(desired);
      String remoteURI = gitID[0];
      deleteRepo(remoteURI);
      git.close();
      throw ex;
    }

    git.close();
  }

  private static void overwriteFileWithString(File file, String content) {
    if (content != null) {
      OutputStream out = null;
      try {
        out = FileUtils.openOutputStream(file, false);
        IOUtils.write(content, out);
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        IOUtils.closeQuietly(out);
      }
    }
  }

  public static String[] codeFromGit(String id, String fileName) throws Exception {
    File targetFile = fileFromGit(id, fileName);

    if (targetFile.exists()) {
      String text = new String(Files.readAllBytes(Paths.get(targetFile.getAbsolutePath())),
          StandardCharsets.UTF_8);
      return new String[]{text, fileName, targetFile.getAbsolutePath()};
    }

    return null;
  }

  private static String[] codeFromGistID(String id, String fileName) throws Exception {
    String giturl = "https://gist.github.com/" + id + ".git";
    File targetFile = fileFromGit(giturl, fileName);

    if (targetFile.exists()) {
      LoggerUtilities.getLogger().log(Level.INFO,
          "Gist at GIT : " + giturl);
      // Target file is ready to go
      String text = new String(Files.readAllBytes(Paths.get(targetFile.getAbsolutePath())),
          StandardCharsets.UTF_8);
      return new String[]{text, fileName, targetFile.getAbsolutePath()};
    }

    return null;
  }

  public static Object inlineFileScriptRun(File file, ArrayList<Object> args) throws Exception {
    return inlineScriptRun(file, args, getShellType(file.getName()));
  }

  public static Object inlineGistScriptRun(String gistID,
                                           String filename,
                                           ArrayList<Object> args) throws Exception {
    String[] gistData = codeFromGistID(gistID, filename);
    return inlineScriptRun(new File(gistData[2]), args, getShellType(gistData[1]));
  }

  public static Object gitScriptRun(String gitURL,
                                    String fileName,
                                    ArrayList<Object> args) throws Exception {
    String[] gistData = codeFromGit(gitURL, fileName);
    return inlineScriptRun(new File(gistData[2]), args, getShellType(gistData[1]));
  }

  public static File fileFromGit(String remoteURI,
                                 String fileInRepo) throws GitAPIException, IOException {
    return fileFromGit(remoteURI, ScriptingEngine.getFullBranch(remoteURI), fileInRepo);
  }

  public static File fileFromGit(String remoteURI,
                                 String branch,
                                 String fileInRepo) throws GitAPIException, IOException {
    File gitRepoFile = cloneRepo(remoteURI, branch);
    String id = gitRepoFile.getAbsolutePath();

    if (fileLastLoaded.get(id) == null) {
      // forces the first time the files is accessed by the application
      // tou pull an update
      fileLastLoaded.put(id, System.currentTimeMillis() - TIME_TO_WAIT_BETWEEN_GIT_PULL * 2);
    }

    long lastTime = fileLastLoaded.get(id);
    if ((System.currentTimeMillis() - lastTime) > TIME_TO_WAIT_BETWEEN_GIT_PULL
        || !gitRepoFile.exists()) {
      fileLastLoaded.put(id, System.currentTimeMillis());

      if (isAutoupdate()) {
        try {
          if (cp == null) {
            cp = new UsernamePasswordCredentialsProvider(loginID, password);
          }

          Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile() + "/.git");
          Git git = new Git(localRepo);

          try {
            PullResult ret = git.pull().setCredentialsProvider(cp).call(); //TODO: Not used?
            git.close();
          } catch (Exception e) {
            try {
              LoggerUtilities.getLogger().log(Level.WARNING,
                  "Error ing gist, hosing: " + gitRepoFile
                      + ".\n" + Throwables.getStackTraceAsString(e));
              FileUtils.deleteDirectory(gitRepoFile);
            } catch (IOException ex) {
              LoggerUtilities.getLogger().log(Level.WARNING,
                  "Could not delete: " + gitRepoFile
                      + ".\n" + Throwables.getStackTraceAsString(ex));
            }
          }

          git.close();
        } catch (NullPointerException ex) {
          setAutoupdate(false);
        }
      }
    }

    return new File(gitRepoFile.getAbsolutePath() + "/" + fileInRepo);
  }

  public static File uriToFile(String remoteURI) {
    String[] colinSplit = remoteURI.split(":");
    String gitSplit = colinSplit[1].substring(0, colinSplit[1].lastIndexOf('.'));
    return new File(getWorkspace().getAbsolutePath() + "/gitcache/" + gitSplit + "/.git");
  }

  public static String getBranch(String remoteURI) throws IOException {
    File gitRepoFile = uriToFile(remoteURI);
    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    String branch = localRepo.getBranch();
    localRepo.close();

    return branch;
  }

  public static String getFullBranch(String remoteURI) throws IOException {
    File gitRepoFile = uriToFile(remoteURI);
    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    String branch = localRepo.getFullBranch();
    localRepo.close();

    return branch;
  }

  public static void deleteBranch(String remoteURI, String toDelete) throws Exception {
    boolean found = false;
    for (String s : listBranchNames(remoteURI)) {
      if (s.contains(toDelete)) {
        found = true;
      }
    }

    if (!found) {
      throw new RuntimeException(toDelete + " can not be deleted because it does not exist");
    }

    File gitRepoFile = uriToFile(remoteURI);
    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    Git git = new Git(localRepo);
    if (!toDelete.contains("heads")) {
      toDelete = "heads/" + toDelete;
    }
    if (!toDelete.contains("refs")) {
      toDelete = "refs/" + toDelete;
    }

    Exception ex = null; //TODO: What is happening here
    try {
      // delete branch 'branchToDelete' locally
      git.branchDelete().setBranchNames(toDelete).call();

      // delete branch 'branchToDelete' on remote 'origin'
      RefSpec refSpec = new RefSpec().setSource(null).setDestination(toDelete);
      git.push().setRefSpecs(refSpec).setRemote("origin").setCredentialsProvider(cp).call();
    } catch (Exception e) {
      ex = e;
    }

    git.close();
    if (ex != null) {
      throw ex;
    }
  }

  public static void newBranch(String remoteURI, String newBranch) throws Exception {
    for (String string : listBranchNames(remoteURI)) {
      if (string.contains(newBranch)) {
        throw new RuntimeException(newBranch + " can not be created because "
            + string + " is to similar");
      }
    }

    File gitRepoFile = uriToFile(remoteURI);
    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    String source = getFullBranch(remoteURI);
    Git git = new Git(localRepo);
    CreateBranchCommand bcc = git.branchCreate();
    CheckoutCommand checkout = git.checkout();
    bcc.setName(newBranch).setStartPoint(source).setForce(true).call();

    checkout.setName(newBranch);
    checkout.call();
    PushCommand pushCommand = git.push();
    pushCommand
        .setRemote("origin")
        .setRefSpecs(new RefSpec(newBranch + ":" + newBranch))
        .setCredentialsProvider(cp).call();

    git.close();
  }

  private static boolean hasAtLeastOneReference(Git git) throws Exception {
    Repository repo = git.getRepository();
    Config storedConfig = repo.getConfig();
    Set<String> uriList = repo.getConfig().getSubsections("remote");
    String remoteURI = null;

    for (String remoteName : uriList) {
      if (remoteURI == null) {
        remoteURI = storedConfig.getString("remote", remoteName, "url");
      }
    }

    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() < (startTime + 2000)) {
      for (Ref ref : repo.getAllRefs().values()) {
        if (ref.getObjectId() != null) {
          List<Ref> branchList = listBranches(remoteURI, git);
          if (branchList.size() > 0) {
            return true;
          }
        }
      }
    }

    throw new RuntimeException("No references or branches found!");
  }

  public static List<Ref> listBranches(String remoteURI) throws Exception {
    File gitRepoFile = uriToFile(remoteURI);

    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null); //TODO: Not used?
      return listBranches(remoteURI);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    Git git = new Git(localRepo);
    List<Ref> out = listBranches(remoteURI, git);
    git.close();
    return out;
  }

  public static List<Ref> listBranches(String remoteURI, Git git) throws Exception {
    return git.branchList().setListMode(ListMode.ALL).call();
  }

  public static List<Ref> listLocalBranches(String remoteURI) throws IOException {
    File gitRepoFile = uriToFile(remoteURI);

    if (!gitRepoFile.exists()) {
      gitRepoFile = cloneRepo(remoteURI, null);
    }

    Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile());
    Git git = new Git(localRepo);

    try {
      List<Ref> list = git.branchList().call();
      git.close();
      return list;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    git.close();
    return new ArrayList<>();
  }

  public static List<String> listLocalBranchNames(String remoteURI) throws Exception {
    ArrayList<String> branchNames = new ArrayList<>();
    List<Ref> list = listLocalBranches(remoteURI);

    for (Ref ref : list) {
      branchNames.add(ref.getName());
    }

    return branchNames;
  }

  public static List<String> listBranchNames(String remoteURI) throws Exception {
    ArrayList<String> branchNames = new ArrayList<>();
    List<Ref> list = listBranches(remoteURI);

    for (Ref ref : list) {
      branchNames.add(ref.getName());
    }

    return branchNames;
  }

  public static void pull(String remoteURI, String branch) throws IOException {
    cloneRepo(remoteURI, branch);
  }

  public static void checkoutCommit(String remoteURI,
                                    String branch,
                                    String commitHash) throws IOException {
    File gitRepoFile = ScriptingEngine.uriToFile(remoteURI);

    if (!gitRepoFile.exists() || !gitRepoFile.getAbsolutePath().endsWith(".git")) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Invalid git file: " + gitRepoFile.getAbsolutePath());
      throw new RuntimeException("Invailid git file!" + gitRepoFile.getAbsolutePath());
    }

    Repository localRepo = new FileRepository(gitRepoFile);
    Git git = new Git(localRepo);

    try {
      git.checkout().setName(commitHash).call();
      git.checkout().setCreateBranch(true).setName(branch).setStartPoint(commitHash).call();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    git.close();
  }

  public static void checkout(String remoteURI, String branch) throws IOException {
    File gitRepoFile = uriToFile(remoteURI);

    if (!gitRepoFile.exists() || !gitRepoFile.getAbsolutePath().endsWith(".git")) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Invalid git file: " + gitRepoFile.getAbsolutePath());
      throw new RuntimeException("Invailid git file!" + gitRepoFile.getAbsolutePath());
    }

    String currentBranch = getFullBranch(remoteURI);
    if (currentBranch != null) {
      Repository localRepo = new FileRepository(gitRepoFile);
      Git git = new Git(localRepo);

      if (!currentBranch.contains(branch)) {
        try {
          git.pull().setCredentialsProvider(cp).call();
          git.branchCreate()
              .setForce(true)
              .setName(branch)
              .setStartPoint("origin/" + branch)
              .call();
          git.checkout().setName(branch).call();
        } catch (Exception e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not checkout file: " + gitRepoFile.getAbsolutePath()
                  + ".\n" + Throwables.getStackTraceAsString(e));
        }
      }

      git.close();
    }
  }

  /**
   * This function retrieves the local cached version of a given git repository. If it does not
   * exist, it clones it.
   *
   * @param remoteURI Remove repository URI
   * @param branch Branch in repository
   * @return The local directory containing the .git
   */
  public static File cloneRepo(String remoteURI, String branch) {
    String[] colinSplit = remoteURI.split(":");
    String gitSplit = colinSplit[1].substring(0, colinSplit[1].lastIndexOf('.'));
    File gistDir = new File(getWorkspace().getAbsolutePath() + "/gitcache/" + gitSplit);

    if (!gistDir.exists()) {
      gistDir.mkdir();
    }

    String localPath = gistDir.getAbsolutePath();
    File gitRepoFile = new File(localPath + "/.git");
    File dir = new File(localPath);

    if (!gitRepoFile.exists()) {
      LoggerUtilities.getLogger().log(Level.INFO, "Cloning files from: " + remoteURI);

      if (branch != null) {
        LoggerUtilities.getLogger().log(Level.INFO, "            branch: " + branch);
      }

      LoggerUtilities.getLogger().log(Level.INFO, "                to: " + localPath);

      for (int i = 0; i < 5; i++) {
        // Clone the repo
        try {
          if (branch == null) {
            Git git = Git.cloneRepository()
                .setURI(remoteURI)
                .setDirectory(dir)
                .setCredentialsProvider(cp)
                .call();

            hasAtLeastOneReference(git);
            branch = getFullBranch(remoteURI);
            checkout(remoteURI, branch);
            hasAtLeastOneReference(git);
            git.close();
          } else {
            Git git = Git.cloneRepository()
                .setURI(remoteURI)
                .setBranch(branch)
                .setDirectory(dir)
                .setCredentialsProvider(cp)
                .call();

            hasAtLeastOneReference(git);
            checkout(remoteURI, branch);
            hasAtLeastOneReference(git);
            git.close();
          }
          break;
        } catch (Exception e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Failed to clone " + remoteURI + ".\n" + Throwables.getStackTraceAsString(e));

          try {
            FileUtils.deleteDirectory(new File(localPath));
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }

        ThreadUtil.wait(200 * i);
      }
    }

    if (branch != null) {
      try {
        checkout(remoteURI, branch);
      } catch (IOException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not checkout: " + remoteURI + "/" + branch
                + ".\n" + Throwables.getStackTraceAsString(e));
      }
    }

    return gistDir;
  }

  public static Git locateGit(File file) throws IOException {
    File gitRepoFile = file;

    while (gitRepoFile != null) {
      gitRepoFile = gitRepoFile.getParentFile();

      if (new File(gitRepoFile.getAbsolutePath() + "/.git").exists()) {
        Repository localRepo = new FileRepository(gitRepoFile.getAbsoluteFile() + "/.git");
        return new Git(localRepo);
      }
    }

    return null;
  }

  public static String getText(URL website) throws Exception {
    URLConnection connection = website.openConnection();
    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    StringBuilder response = new StringBuilder();
    String inputLine;

    while ((inputLine = reader.readLine()) != null) {
      response.append(inputLine).append("\n");
    }

    reader.close();
    return response.toString();
  }

  public static File getLastFile() {
    if (lastFile == null) {
      return getWorkspace();
    }

    return lastFile;
  }

  public static void setLastFile(File lastFile) {
    ScriptingEngine.lastFile = lastFile;
  }

  private static File getCreds() {
    if (creds == null) {
      setCreds(new File(System.getProperty("user.home") + "/.github"));
    }

    return creds;
  }

  public static void setCreds(File creds) {
    ScriptingEngine.creds = creds;
  }

  public static File getFileEngineRunByName(String filename) {
    return filesRun.get(filename);
  }

  public static String[] getImports() {
    return imports;
  }

  public static IGitHubLoginManager getLoginManager() {
    return loginManager;
  }

  public static void setLoginManager(IGitHubLoginManager loginManager) {
    ScriptingEngine.loginManager = loginManager;
  }

  public static boolean isAutoupdate() {
    return autoupdate;
  }

  public static boolean setAutoupdate(boolean autoupdate) throws IOException {
    if (autoupdate && !ScriptingEngine.autoupdate) {
      ScriptingEngine.autoupdate = true; //prevents recursion loop from calling loadLoginData
      loadLoginData();

      if (password == null || loginID == null) {
        login();
      }

      if (password == null || loginID == null) {
        return false;
      }
    }

    ScriptingEngine.autoupdate = autoupdate;
    return ScriptingEngine.autoupdate;
  }

  private static File fileFromGistID(String remoteURI,
                                     String branch) throws GitAPIException, IOException {
    return fileFromGit("https://gist.github.com/" + remoteURI + ".git", branch);
  }

  public static String findLocalPath(File currentFile, Git git) {
    File dir = git.getRepository().getDirectory().getParentFile();
    return dir.toURI().relativize(currentFile.toURI()).getPath();
  }

  public static String findLocalPath(File currentFile) {
    Git git;

    try {
      git = locateGit(currentFile);
      return findLocalPath(currentFile, git);
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not locate git for file: " + currentFile.getAbsolutePath()
              + ".\n" + Throwables.getStackTraceAsString(e));
    }

    return currentFile.getName();

  }

  public static String[] findGitTagFromFile(File currentFile) throws IOException {
    Git git = locateGit(currentFile);
    return new String[]{git.getRepository()
        .getConfig()
        .getString("remote", "origin", "url"),
        findLocalPath(currentFile, git)};
  }

  public static boolean checkOwner(File currentFile) {
    try {
      waitForLogin();
      Git git = locateGit(currentFile);
      git.pull().setCredentialsProvider(cp).call();// updates to the latest version
      git.push().setCredentialsProvider(cp).call();
      git.close();
      return true;
    } catch (Exception e) {
      // just return false, the exception is it failing to push
    }

    return false;
  }

  public static GHGist fork(String currentGist) throws Exception {
    if (getGithub() != null) {
      waitForLogin();
      GHGist incoming = getGithub().getGist(currentGist);

      for (IGithubLoginListener listener : loginListeners) {
        listener.onLogin(loginID);
      }

      return incoming.fork();
    }

    return null;
  }

  public static String[] forkGitFile(String[] incoming) throws Exception {
    GitHub github = ScriptingEngine.getGithub();
    String id;

    if (incoming[0].endsWith(".git")) {
      id = urlToGist(incoming[0]);
    } else {
      id = incoming[0];
      incoming[0] = "https://gist.github.com/" + id + ".git";
    }

    GHGist incomingGist = github.getGist(id);
    File incomingFile = ScriptingEngine.fileFromGistID(id, incoming[1]);

    if (!ScriptingEngine.checkOwner(incomingFile)) {
      incomingGist = incomingGist.fork();
      incoming[0] = "https://gist.github.com/"
          + ScriptingEngine.urlToGist(incomingGist.getHtmlUrl()) + ".git";
      // sync the new file to the disk
      incomingFile = ScriptingEngine.fileFromGistID(id, incoming[1]); //TODO: Not used?
    }

    for (IGithubLoginListener listener : loginListeners) {
      listener.onLogin(loginID);
    }

    return incoming;
  }

  public static GitHub getGithub() {
    return github;
  }

  public static void setGithub(GitHub github) {
    ScriptingEngine.github = github;
    if (github == null) {
      setLoginSuccess(false);
    }
  }

  public static List<String> getAllLangauges() {
    ArrayList<String> langs = new ArrayList<>();
    langs.addAll(getLangaugesMap().keySet());
    return langs;
  }

  public static HashMap<String, IScriptingLanguage> getLangaugesMap() {
    return langauges;
  }

  public static boolean hasNetwork() {
    return hasnetwork;
  }

  public static boolean isLoginSuccess() {
    return loginSuccess;
  }

  public static void setLoginSuccess(boolean loginSuccess) {
    ScriptingEngine.loginSuccess = loginSuccess;
  }

}
