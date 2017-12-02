package com.neuronrobotics.bowlerstudio.physics;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import eu.mihosoft.vrl.v3d.CSG;

import java.util.ArrayList;

import javafx.scene.transform.Affine;

public interface IPhysicsManager {

  /**
   * Run the update for this ridgid body. Run any controllers for links.
   */
  void update(float timeStep);

  /**
   * Return a RigidBody for the physics engine.
   */
  RigidBody getFallRigidBody();

  /**
   * Return the CSG that is being modeled.
   */
  ArrayList<CSG> getBaseCSG();

  /**
   * Return the current spatial location fo the rigid body.
   */
  Affine getRigidBodyLocation();

  /**
   * The Bullet version of the location.
   */
  Transform getUpdateTransform();

}
