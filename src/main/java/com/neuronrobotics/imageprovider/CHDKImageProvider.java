package com.neuronrobotics.imageprovider;

import chdk.ptp.java.CameraFactory;
import chdk.ptp.java.ICamera;
import chdk.ptp.java.SupportedCamera;
import chdk.ptp.java.exception.CameraConnectionException;
import chdk.ptp.java.exception.GenericCameraException;
import chdk.ptp.java.exception.PTPTimeoutException;
import chdk.ptp.java.model.CameraMode;
import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Level;

public class CHDKImageProvider extends AbstractImageProvider {
  private ICamera cam;

  public CHDKImageProvider() throws PTPTimeoutException, GenericCameraException {
    cam = CameraFactory.getCamera(SupportedCamera.SX160IS);
    cam.connect();
    cam.setOperaionMode(CameraMode.RECORD);
  }

  @Override
  public boolean captureNewImage(BufferedImage imageData) {
    int failure = 0;
    while (failure < 5) {
      try {
        // Thread.sleep(3000);
        //bufferedImageToMat(cam.getPicture(), imageData);
        AbstractImageProvider.deepCopy(captureNewImage(), imageData);
        return true;
      } catch (RuntimeException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Exception while disconnecting.\n" + Throwables.getStackTraceAsString(e));

        failure++;
      }
    }
    return false;
  }


  @Override
  public void disconnectDeviceImp() {
    try {
      cam.disconnect();
    } catch (CameraConnectionException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Exception while disconnecting.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  @Override
  public boolean connectDeviceImp() {
    return false;
  }

  @Override
  public ArrayList<String> getNamespacesImp() {
    return null;
  }

  @Override
  public BufferedImage captureNewImage() {
    try {
      return cam.getPicture();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
