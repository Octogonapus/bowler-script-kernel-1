package com.neuronrobotics.bowlerstudio.physics;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Vertex;

import java.util.ArrayList;

import javafx.scene.transform.Affine;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class CSGPhysicsManager implements IPhysicsManager {

  private final Affine ballLocation = new Affine();
  protected ArrayList<CSG> baseCSG = null;
  private RigidBody fallRigidBody;
  private Transform updateTransform = new Transform();
  private IPhysicsUpdate updateManager = null;
  private PhysicsCore core;

  public CSGPhysicsManager(ArrayList<CSG> baseCSG,
                           Vector3f start,
                           double mass,
                           PhysicsCore core) {
    this(baseCSG,
        new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), start, 1.0f)),
        mass,
        true,
        core);
  }

  public CSGPhysicsManager(ArrayList<CSG> baseCSG,
                           Transform pose,
                           double mass,
                           boolean adjustCenter,
                           PhysicsCore core) {
    this.setBaseCSG(baseCSG);// force a hull of the shape to simplify physics


    ObjectArrayList<Vector3f> arg0 = new ObjectArrayList<>();
    for (int i = 0; i < baseCSG.size(); i++) {

      CSG back = loadCSGToPoints(baseCSG.get(i), adjustCenter, pose, arg0);
      back.setManipulator(baseCSG.get(i).getManipulator());
      baseCSG.set(i, back);
    }
    CollisionShape fallShape = new com.bulletphysics.collision.shapes.ConvexHullShape(arg0);
    setup(fallShape, pose, mass, core);
  }

  protected CSG loadCSGToPoints(CSG baseCSG,
                                boolean adjustCenter,
                                Transform pose,
                                ObjectArrayList<Vector3f> arg0) {
    CSG finalCSG = baseCSG;

    if (adjustCenter) {
      double xcenter = baseCSG.getMaxX() / 2 + baseCSG.getMinX() / 2;
      double ycenter = baseCSG.getMaxY() / 2 + baseCSG.getMinY() / 2;
      double zcenter = baseCSG.getMaxZ() / 2 + baseCSG.getMinZ() / 2;
      TransformNR poseToMove = TransformFactory.bulletToNr(pose);

      if (baseCSG.getMaxX() < 1 || baseCSG.getMinX() > -1) {
        finalCSG = finalCSG.movex(-xcenter);
        poseToMove.translateX(xcenter);
      }

      if (baseCSG.getMaxY() < 1 || baseCSG.getMinY() > -1) {
        finalCSG = finalCSG.movey(-ycenter);
        poseToMove.translateY(ycenter);
      }

      if (baseCSG.getMaxZ() < 1 || baseCSG.getMinZ() > -1) {
        finalCSG = finalCSG.movez(-zcenter);
        poseToMove.translateZ(zcenter);
      }

      TransformFactory.nrToBullet(poseToMove, pose);
    }

    for (Polygon p : finalCSG.getPolygons()) {
      for (Vertex v : p.vertices) {
        arg0.add(new Vector3f((float) v.getX(), (float) v.getY(), (float) v.getZ()));
      }
    }

    return finalCSG;
  }

  public void setup(CollisionShape fallShape, Transform pose, double mass, PhysicsCore core) {
    this.setCore(core);

    // setup the motion state for the ball
    System.out.println("Starting Object at " + TransformFactory.bulletToNr(pose));
    DefaultMotionState fallMotionState = new DefaultMotionState(pose);

    // This we're going to give mass so it responds to gravity
    Vector3f fallInertia = new Vector3f(0, 0, 0);
    fallShape.calculateLocalInertia((float) mass, fallInertia);
    RigidBodyConstructionInfo fallRigidBodyCI =
        new RigidBodyConstructionInfo((float) mass, fallMotionState, fallShape, fallInertia);
    fallRigidBodyCI.additionalDamping = true;
    setFallRigidBody(new RigidBody(fallRigidBodyCI));
  }

  public void update(float timeStep) {
    fallRigidBody.getMotionState().getWorldTransform(updateTransform);

    if (getUpdateManager() != null) {
      try {
        getUpdateManager().update(timeStep);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public RigidBody getFallRigidBody() {
    return fallRigidBody;
  }

  public void setFallRigidBody(RigidBody fallRigidBody) {
    this.fallRigidBody = fallRigidBody;
  }

  public ArrayList<CSG> getBaseCSG() {
    return baseCSG;
  }

  public void setBaseCSG(ArrayList<CSG> baseCSG) {
    for (CSG c : baseCSG) {
      c.setManipulator(getRigidBodyLocation());
    }

    this.baseCSG = baseCSG;
  }

  public Transform getUpdateTransform() {
    return updateTransform;
  }

  public Affine getRigidBodyLocation() {
    return ballLocation;
  }

  public IPhysicsUpdate getUpdateManager() {
    return updateManager;
  }

  public void setUpdateManager(IPhysicsUpdate updateManager) {
    this.updateManager = updateManager;
  }

  public PhysicsCore getCore() {
    return core;
  }

  public void setCore(PhysicsCore core) {
    this.core = core;
  }

}
