package com.neuronrobotics.bowlerstudio.physics;

import eu.mihosoft.vrl.v3d.CSG;
import java.util.List;

public class PhysicsEngine {
  private static PhysicsCore mainEngine;

  public static void startPhysicsThread(int ms) {
    get().startPhysicsThread(ms);
  }

  public static void stopPhysicsThread() {
    get().stopPhysicsThread();
  }

  public static void step(float timeStep) {
    get().step(timeStep);
  }

  public static void stepMs(double timeStep) {
    get().stepMs(timeStep);
  }

  public static void add(IPhysicsManager manager) {
    get().add(manager);
  }

  public static void remove(IPhysicsManager manager) {
    get().remove(manager);
  }

  /**
   * Delete the main physics engine.
   */
  public static void clear() {
    get().clear();
    mainEngine = null;
  }

  /**
   * Return the main physics engine.
   *
   * @return Main physics engine
   */
  public static PhysicsCore get() {
    if (mainEngine == null) {
      try {
        mainEngine = new PhysicsCore();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return mainEngine;
  }

  public static List<CSG> getCsgFromEngine() {
    return mainEngine.getCsgFromEngine();
  }

}
