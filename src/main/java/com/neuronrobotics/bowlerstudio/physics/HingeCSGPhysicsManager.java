package com.neuronrobotics.bowlerstudio.physics;

import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import com.bulletphysics.linearmath.Transform;
import com.neuronrobotics.sdk.common.IClosedLoopController;
import eu.mihosoft.vrl.v3d.CSG;
import java.util.ArrayList;
import javafx.scene.paint.Color;

public class HingeCSGPhysicsManager extends CSGPhysicsManager {
  private static float muscleStrength = (float) 1000;
  private boolean flagBroken = false;
  private HingeConstraint constraint = null;
  private IClosedLoopController controller = null;
  private double target = 0;

  public HingeCSGPhysicsManager(ArrayList<CSG> baseCSG,
                                Transform pose,
                                double mass,
                                PhysicsCore core) {
    super(baseCSG, pose, mass, false, core);
  }

  public static float getMuscleStrength() {
    return muscleStrength;
  }

  public void setMuscleStrength(double muscleStrength) {
    setMuscleStrength((float) muscleStrength);
  }

  public static void setMuscleStrength(float ms) {
    muscleStrength = ms;
  }

  @Override
  public void update(float timeStep) {
    super.update(timeStep);
    if (constraint != null && getController() != null && !flagBroken) {
      double velocity = getController().compute(constraint.getHingeAngle(), getTarget(), timeStep);
      constraint.enableAngularMotor(true, (float) velocity, getMuscleStrength());

      if (constraint.getAppliedImpulse() > getMuscleStrength()) {
        for (CSG c1 : baseCSG) {
          c1.setColor(Color.WHITE);
        }

        flagBroken = true;
        getCore().remove(this);
        setConstraint(null);
        getCore().add(this);

        System.out.println(
            "ERROR Link Broken, Strength: "
                + getMuscleStrength()
                + " applied impluse "
                + constraint.getAppliedImpulse());
      }
    } else if (constraint != null && flagBroken) {
      constraint.enableAngularMotor(false, 0, 0);
    }
  }

  public HingeConstraint getConstraint() {
    return constraint;
  }

  public void setConstraint(HingeConstraint constraint) {
    this.constraint = constraint;
  }

  public double getTarget() {
    return target;
  }

  public void setTarget(double target) {
    this.target = target;
  }

  public IClosedLoopController getController() {
    return controller;
  }

  public void setController(IClosedLoopController controller) {
    this.controller = controller;
  }

}
