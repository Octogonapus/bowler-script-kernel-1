package com.neuronrobotics.bowlerstudio.creature;

import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;

import eu.mihosoft.vrl.v3d.CSG;

import java.util.ArrayList;

public interface ICadGenerator {

  /**
   * This function should use the D-H parameters to generate cad objects to build this configuration
   * the user should attach any listeners from the DH link for simulation.
   *
   * @param dh the list of DH configurations
   * @return simulatable CAD objects
   */
  ArrayList<CSG> generateCad(DHParameterKinematics dh, int linkIndex);

  /**
   * This function should generate the body and any limbs of a given base. The user should attach
   * any listeners from the DH link for simulation.
   *
   * @param base the base to generate
   * @return simulatable CAD objects
   */
  ArrayList<CSG> generateBody(MobileBase base);

}
