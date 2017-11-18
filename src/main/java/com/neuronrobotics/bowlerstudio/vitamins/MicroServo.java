package com.neuronrobotics.bowlerstudio.vitamins;

import com.neuronrobotics.imageprovider.NativeResource;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.STL;
import eu.mihosoft.vrl.v3d.Transform;
import java.io.File;

public class MicroServo implements IVitamin {

  private static CSG servoModel;

  static {
    try {
      File stl = NativeResource.inJarLoad(IVitamin.class, "hxt900-servo.stl");
      servoModel = STL.file(stl.toPath());
      servoModel = servoModel.transformed(new Transform().translateZ(-19.3));
      servoModel = servoModel.transformed(new Transform().translateX(5.4));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public CSG toCSG() {
    return servoModel.clone();
  }

}
