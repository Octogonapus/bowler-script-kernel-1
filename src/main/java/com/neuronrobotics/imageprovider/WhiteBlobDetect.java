package com.neuronrobotics.imageprovider;

import com.neuronrobotics.bowlerstudio.LoggerUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

public class WhiteBlobDetect implements IObjectDetector {
  private MatOfKeyPoint matOfKeyPoints = new MatOfKeyPoint();
  private Mat prethresh = new Mat();
  private Mat postthresh = new Mat();
  private Scalar colorKey = new Scalar(0, 0, 255, 0);
  private FeatureDetector RGBblobdetector =
      FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);
  private int minSize;
  private int maxSize;


  public WhiteBlobDetect(int minSize, int maxSize, Scalar lower) {
    this.minSize = minSize;
    this.maxSize = maxSize;
    colorKey = lower.clone();
  }

  public List<Detection> getObjects(BufferedImage in, BufferedImage disp) {
    Mat inputImage = new Mat();
    OpenCVImageConversionFactory.bufferedImageToMat(in, inputImage);
    Mat displayImage = new Mat();
    ArrayList<Detection> ret = new ArrayList<>();
    KeyPoint[] detects = getObjects(inputImage, displayImage);
    for (KeyPoint detect : detects) {
      ret.add(new Detection(detect.pt.x, detect.pt.y, detect.size));
    }
    return ret;
  }

  private KeyPoint[] getObjects(Mat inputImage, Mat displayImage) {
    Imgproc.cvtColor(inputImage, prethresh, Imgproc.COLOR_RGB2GRAY);
    Imgproc.threshold(prethresh,
        postthresh,
        colorKey.val[1],
        colorKey.val[0],
        Imgproc.THRESH_BINARY);

    Mat invertcolormatrix = new Mat(postthresh.rows(),
        postthresh.cols(),
        postthresh.type(),
        new Scalar(255, 255, 255));
    Core.subtract(invertcolormatrix, postthresh, postthresh);

    RGBblobdetector.detect(postthresh, matOfKeyPoints);


    postthresh.copyTo(displayImage);
    Features2d.drawKeypoints(postthresh,
        matOfKeyPoints,
        displayImage,
        new Scalar(0, 0, 255, 0), 0);

    // Prepare to display data
    int useful = 0;
    KeyPoint[] keyPoints = matOfKeyPoints.toArray();

    if (keyPoints.length > 0) {

      for (KeyPoint keyPoint : keyPoints) {
        if (keyPoint.size > minSize && keyPoint.size < maxSize) {
          useful++;
        }
      }

      KeyPoint[] usefulKeyPoints = new KeyPoint[useful];
      Point center = null;

      useful = 0;
      for (KeyPoint keyPoint : keyPoints) {
        if (keyPoint.size > minSize && keyPoint.size < maxSize) {
          center = keyPoint.pt;

          Size objectSize = new Size(keyPoint.size, keyPoint.size);
          Core.ellipse(displayImage,
              center,
              objectSize,
              0,
              0,
              360,
              new Scalar(255, 0, 255),
              4,
              8,
              0);

          usefulKeyPoints[useful++] = keyPoint;
        }
      }

      if (center != null) {
        for (KeyPoint keyPoint : usefulKeyPoints) {
          if (keyPoint != null) {
            Core.line(displayImage, new Point(150, 50), keyPoint.pt,
                new Scalar(100, 10, 10)/* CV_BGR(100,10,10) */, 3);
            Core.circle(displayImage, keyPoint.pt, 10, new Scalar(100, 10, 10),
                3);
          }
        }
      }

      return usefulKeyPoints;
    }

    LoggerUtilities.getLogger().log(Level.INFO,
        "Got: " + matOfKeyPoints.size());

    return new KeyPoint[0]; //TODO: ?
  }

}
