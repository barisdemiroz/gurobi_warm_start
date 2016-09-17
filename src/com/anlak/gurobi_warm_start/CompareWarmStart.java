package com.anlak.gurobi_warm_start;

import static gurobi.GRB.DoubleAttr.DStart;
import static gurobi.GRB.DoubleAttr.PStart;
import static gurobi.GRB.DoubleAttr.Runtime;
import static gurobi.GRB.IntAttr.CBasis;
import static gurobi.GRB.IntAttr.VBasis;
import gurobi.GRB;
import gurobi.GRB.DoubleAttr;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.Random;

public class CompareWarmStart {

  private static GRBModel MODEL;

  private static double[] PSTARTS, DSTARTS;
  private static int[] VBASES, CBASES;

  private static void saveStateWarmValues(GRBVar[] vars, GRBConstr[] constrs) throws GRBException {
    PSTARTS = new double[vars.length];
    for (int i = 0; i < vars.length; i++)
      PSTARTS[i] = vars[i].get(DoubleAttr.X);

    DSTARTS = new double[constrs.length];
    for (int i = 0; i < constrs.length; i++)
      DSTARTS[i] = constrs[i].get(DoubleAttr.Pi);
  }

  private static void loadStateWarmValues(GRBVar[] vars, GRBConstr[] constrs) throws GRBException {
    for (int i = 0; i < vars.length; i++)
      vars[i].set(PStart, PSTARTS[i]);
    for (int i = 0; i < constrs.length; i++)
      constrs[i].set(DStart, DSTARTS[i]);
  }

  private static void saveStateWarmBasis(GRBVar[] vars, GRBConstr[] constrs) throws GRBException {
    VBASES = new int[vars.length];
    for (int i = 0; i < vars.length; i++)
      VBASES[i] = vars[i].get(VBasis);

    CBASES = new int[constrs.length];
    for (int i = 0; i < constrs.length; i++)
      CBASES[i] = constrs[i].get(CBasis);
  }

  private static void loadStateWarmBasis(GRBVar[] vars, GRBConstr[] constrs) throws GRBException {
    for (int i = 0; i < vars.length; i++)
      vars[i].set(VBasis, VBASES[i]);
    for (int i = 0; i < constrs.length; i++)
      constrs[i].set(CBasis, CBASES[i]);
  }


  // create a new variable and associate it with 10 constraints
  private static GRBVar addNewVar(GRBConstr[] constrs) throws GRBException {
    GRBVar var = MODEL.addVar(0, GRB.INFINITY, -10, GRB.CONTINUOUS, "new_x");
    MODEL.update();
    Random rng = new Random();
    for (int i = 0; i < 1000; i++) {
      GRBConstr constr = constrs[rng.nextInt(constrs.length)];
      MODEL.chgCoeff(constr, var, 1);
    }
    MODEL.update();
    return var;
  }

  public static void main(String[] args) throws GRBException {
    GRBEnv env = new GRBEnv();
    MODEL = new GRBModel(env, "bigmodel.lp");

    MODEL.optimize();


    GRBVar[] vars = MODEL.getVars();
    GRBConstr[] constrs = MODEL.getConstrs();

    saveStateWarmValues(vars, constrs);
    saveStateWarmBasis(vars, constrs);


    GRBVar var = addNewVar(constrs);


    // PStart/DStart method
    MODEL.reset();

    loadStateWarmValues(vars, constrs);
    var.set(PStart, 0); // Don't forget to set a value for the new variable or warm-start won't work

    MODEL.optimize();
    double warmValuesTime = MODEL.get(Runtime);


    // VBasis/CBasis method
    MODEL.reset();

    loadStateWarmBasis(vars, constrs);
    var.set(VBasis, -3); // Don't forget to set VBasis for the new variable
    MODEL.optimize();
    double warmBasisTime = MODEL.get(Runtime);


    // Cold start
    MODEL.reset();
    MODEL.optimize();
    double coldTime = MODEL.get(Runtime);

    System.out.println("\n\n    ============================\n");
    System.out.format("Cold start: %.3f secs.\n", coldTime);
    System.out.format("Warm start with PStart/Dstart: %.3f secs.\n", warmValuesTime);
    System.out.format("Warm start with VBasis/CBasis: %.3f secs.\n", warmBasisTime);
  }
}
