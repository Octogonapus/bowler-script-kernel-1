package com.neuronrobotics.bowlerstudio.assets;

import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;

public class AssetFactory {

  private static final String repo = "BowlerStudioImageAssets";
  private static String gitSource = "https://github.com/madhephaestus/" + repo + ".git";
  private static Map<String, Image> cache = new HashMap<>();
  private static Map<String, FXMLLoader> loaders = new HashMap<>();
  private static String assetRepoBranch = "";

  private AssetFactory() {
  }

  /**
   * Load an FXML layout from a file without refreshing and return the loader.
   *
   * @param file File to load from
   * @return The FXMLLoader
   * @throws Exception Loading the file could throw
   */
  public static FXMLLoader loadLayout(String file) throws Exception {
    return loadLayout(file, false);
  }

  /**
   * Load an FXML layout from a file and return the loader.
   * @param file File to load from
   * @param refresh Whether the re-load a file even if it's already loaded
   * @return The FXMLLoader
   * @throws Exception Loading the file could throw
   */
  public static FXMLLoader loadLayout(String file, boolean refresh) throws Exception {
    File fxmlFIle = loadFile(file);
    URL fileURL = fxmlFIle.toURI().toURL();

    if (loaders.get(file) == null || refresh) {
      loaders.put(file, new FXMLLoader(fileURL));
    }

    loaders.get(file).setLocation(fileURL);
    return loaders.get(file);
  }

  public static File loadFile(String file) throws Exception {
    return ScriptingEngine.fileFromGit(
            getGitSource(),// git repo, change this if you fork this demo
            getAssetRepoBranch(),
            file// File from within the Git repo
        );
  }

  /**
   * Load either an FXML file or an Image.
   *
   * @param file File to load from
   * @return The Image or null if loading an FXML file
   * @throws Exception Loading the file could throw
   */
  public static Image loadAsset(String file) throws Exception {
    if (cache.get(file) == null) {
      File assetFile = loadFile(file);
      if (assetFile.getName().endsWith(".fxml")) {
        loadLayout(file);
        return null;
      } else if (!assetFile.exists() && assetFile.getName().endsWith(".png")) {
        WritableImage obj_img = new WritableImage(30, 30);
        byte alpha = (byte) 0;

        for (int cx = 0; cx < obj_img.getWidth(); cx++) {
          for (int cy = 0; cy < obj_img.getHeight(); cy++) {
            int color = obj_img.getPixelReader().getArgb(cx, cy);
            int mc = (alpha << 24) | 0x00ffffff;
            int newColor = color & mc;
            obj_img.getPixelWriter().setArgb(cx, cy, newColor);
          }
        }

        cache.put(file, obj_img);
        LoggerUtilities.getLogger().log(Level.INFO,
            "No image at " + file);

        try {
          File imageFile = ScriptingEngine.createFile(getGitSource(), file, "create file");
          try {
            String fileName = imageFile.getName();
            ImageIO.write(SwingFXUtils.fromFXImage(obj_img, null),
                fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(), imageFile);
          } catch (IOException e) {
            LoggerUtilities.getLogger().log(Level.WARNING,
                "Could not write image file.\n" + Throwables.getStackTraceAsString(e));
          }
          ScriptingEngine.createFile(getGitSource(), file, "saving new content");
        } catch (Exception e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not create file.\n" + Throwables.getStackTraceAsString(e));
        }
      } else {
        cache.put(file, new Image(assetFile.toURI().toString()));
      }
    }
    return cache.get(file);
  }

  /**
   * Load an icon from a file.
   *
   * @param file File to load from
   * @return Icon
   */
  public static ImageView loadIcon(String file) {
    try {
      return new ImageView(loadAsset(file));
    } catch (Exception e) {
      return new ImageView();
    }
  }

  public static String getGitSource() {
    return gitSource;
  }

  public static void setGitSource(String gitSource, String assetRepoBranch) throws Exception {
    LoggerUtilities.getLogger().log(Level.INFO,
        "Assets from " + gitSource + "#" + assetRepoBranch);
    setAssetRepoBranch(assetRepoBranch);
    AssetFactory.gitSource = gitSource;
    cache.clear();
    loadAllAssets();
  }

  public static void loadAllAssets() throws Exception {
    List<String> files = ScriptingEngine.filesInGit(gitSource,
        StudioBuildInfo.getVersion(),
        null);
    for (String file : files) {
      loadAsset(file);
    }
  }

  public static String getAssetRepoBranch() {
    return assetRepoBranch;
  }

  public static void setAssetRepoBranch(String assetRepoBranch) {
    AssetFactory.assetRepoBranch = assetRepoBranch;
  }

}
