package com.neuronrobotics.imageprovider;

import com.neuronrobotics.bowlerstudio.LoggerUtilities;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Level;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class OpenCVImageProvider extends AbstractImageProvider {
  private Mat mat = new Mat();
  private VideoCapture vc;
  private int camerIndex;

  public OpenCVImageProvider(int camerIndex) {
    this.camerIndex = camerIndex;
    setVc(new VideoCapture(camerIndex));

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      LoggerUtilities.getLogger().log(Level.INFO,
          "OpenCVImageProvider constructor sleep interrupted.");
    }
    if (!getVc().isOpened()) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Camera error.");
    } else {
      //boolean wset = getVc().set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 320);
      //boolean hset = getVc().set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);
      LoggerUtilities.getLogger().log(Level.INFO,
          "Camera OK at "
              + " width: " + getVc().get(Highgui.CV_CAP_PROP_FRAME_WIDTH)
              + " height: " + getVc().get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));
    }
  }

  @Override
  public String toString() {
    return "OpenCVImageProvider on camera " + camerIndex + " " + getVc().toString();
  }

  @Override
  public boolean captureNewImage(BufferedImage imageData) {
    if (!getVc().isOpened()) {
      return false;
    }


    try {
      AbstractImageProvider.deepCopy(captureNewImage(), imageData);
    } catch (Exception ex) {
      if (InterruptedException.class.isInstance(ex)) {
        throw new RuntimeException(ex);
      }
    }
    return true;
  }

  @Override
  public BufferedImage captureNewImage() {
    getVc().read(mat);
    return OpenCVImageConversionFactory.matToBufferedImage(mat);
  }

  private VideoCapture getVc() {
    return vc;
  }

  private void setVc(VideoCapture vc) {
    this.vc = vc;
  }

  @Override
  public void disconnectDeviceImp() {
    if (vc != null) {
      vc.release();
    }
    setVc(null);

  }

  @Override
  public boolean connectDeviceImp() {
    return false;
  }

  @Override
  public ArrayList<String> getNamespacesImp() {
    return null;
  }

}
