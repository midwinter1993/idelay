package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.linearsolver.MPSolver.ResultStatus;

public class LpSolver {

    // Create the linear solver with the GLOP backend.
    private static MPSolver solver = new MPSolver("SimpleLpProgram",
            MPSolver.OptimizationProblemType.GLOP_LINEAR_PROGRAMMING);

    private static int nrConstraint = 0;
    private static ArrayList<MPVariable> penaltyList = new ArrayList<>();

    public static MPVariable makeVar(String name) {
        return solver.makeNumVar(0.0, 1.0, name);
    }

    public static MPVariable makePenalty() {
        MPVariable p = solver.makeNumVar(0.0, 0.5, String.format("Penalty%d", penaltyList.size()));
        penaltyList.add(p);
        return p;
    }

    public static void sumGeOne(ArrayList<MPVariable> lpVarList) {
        double infinity = java.lang.Double.POSITIVE_INFINITY;

        MPConstraint ct = solver.makeConstraint(1.0, infinity, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        for (MPVariable v : lpVarList) {
            ct.setCoefficient(v, 1);
        }
    }

    public static void sumLeOne(ArrayList<MPVariable> lpVarList) {
        MPConstraint ct = solver.makeConstraint(0.0, 1.0, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        for (MPVariable v : lpVarList) {
            ct.setCoefficient(v, 1);
        }
    }

    public static void eqOne(MPVariable lpVar) {
        MPConstraint ct = solver.makeConstraint(1.0, 1.0, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        ct.setCoefficient(lpVar, 1);
    }

    public static void equal(MPVariable lpVar1, MPVariable lpVar2) {
        //
        // 0 <= lpVar1 - lpVar2 <= 0
        //
        MPConstraint ct = solver.makeConstraint(0.0, 0.0, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        ct.setCoefficient(lpVar1, 1);
        ct.setCoefficient(lpVar2, -1);
    }

    public static void opposite(MPVariable lpVar1, MPVariable lpVar2) {
        //
        // if lpVar1 = 1 then lpVar2 = 0, or if lpVar2 = 1 then lpVar1 = 0
        // Relaxing the constraint as: lpVar1 + lpVar2 <= 1
        //
        MPConstraint ct = solver.makeConstraint(0.0, 1.0, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        ct.setCoefficient(lpVar1, 1);
        ct.setCoefficient(lpVar2, 1);
    }

    public static void sumClose(ArrayList<MPVariable> lpVarList1,
                                ArrayList<MPVariable> lpVarList2) {
        //
        // Sum(list1) == Sum(list2)
        // Relaxing the constraint as: -1.0 <= Sum(list1) - Sum(list2) <= 1.0
        //
        MPConstraint ct = solver.makeConstraint(-1.0, 1.0, String.format("C%d", nrConstraint));
        nrConstraint += 1;

        for (MPVariable v : lpVarList1) {
            ct.setCoefficient(v, 1);
        }
        for (MPVariable v : lpVarList2) {
            ct.setCoefficient(v, -1);
        }
    }

    public static void object() {
        MPObjective objective = solver.objective();

        for (SyncVar var: SyncVar.getPool().values()) {
            objective.setCoefficient(var.asLpAcq(), var.getAcqCoff());
            objective.setCoefficient(var.asLpRel(), var.getRelCoff());
        }

        for (MPVariable v: penaltyList) {
            objective.setCoefficient(v, 1);
        }

        objective.setMinimization();
    }

    public static void solve() {
        ResultStatus status =  solver.solve();
        System.out.format("  |_ Status: %s\n", status.toString());
    }

    public static void save(String filePath) {
        try {
            PrintWriter out = new PrintWriter(filePath);
            out.println(solver.exportModelAsLpFormat());
            // out.println(solver.exportModelAsMpsFormat());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}