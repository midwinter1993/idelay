package io.github.midwinter1993;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.ortools.linearsolver.MPVariable;

public class Infer {

    private static final String XINFER_RESULT_DIR = "./xinfer-result";

    private ArrayList<SyncWindow> acqWindowSet = new ArrayList<>();
    private ArrayList<SyncWindow> relWindowSet = new ArrayList<>();

    private HashSet<MPVariablePair> equalPairs = new HashSet<>();
    private HashSet<MPVariablePair> oppositePairs = new HashSet<>();

    public void lpEncode(LogPool pool) {
        $.progress("Encode");

        $.run("  |_ Data Race Encode", () -> dataRaceEncode(pool));

        //
        // Heuristic must be encoded first
        //
        $.run("  |_ LP Encode Heuristic", () -> lpEncodeHeuristic());

        $.run("  |_ LP Encode RelWindow", () -> lpEncodeRelWindow());
        $.run("  |_ LP Encode AcqWindow", () -> lpEncodeAcqWindow());

        $.run("  |_ LP Encode Vars",      () -> lpEncodeVars());
        $.run("  |_ LP Encode Class",     () -> lpEncodeClassVars());

        $.run("  |_ LP Encode Pairs",     () -> lpEncodePairs());

        $.run("  |_ LP Encode Object",    () -> lpEncodeObject());
    }

    public void lpSolve() {
        $.progress("Solve");
        LpSolver.solve();
    }

    public void saveResult() {
        $.mkdir(XINFER_RESULT_DIR);
        $.progress("Save Result");
        LpSolver.save($.pathJoin(XINFER_RESULT_DIR, "problem.lp"));
        saveInferResult(true);
        saveWindowInfo();
        saveVarInfo();
    }

    // ===========================================

    public void saveInferResult(boolean needPrint) {
        PrintWriter out = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "infer.result"));
        PrintWriter out2 = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "infer.verify"));

        if (out == null || out2 == null) {
            return;
        }

        if (needPrint) {
            $.progress("--- Release Operation ---");
        }
        out.format("--- Release Operation ---\n");
        out2.println("[RELEASE]");
        for (Map.Entry<String, SyncVar> e: SyncVar.getPool().entrySet()) {
            String varKey = e.getKey();
            SyncVar var = e.getValue();
            int operand = var.asLogEntry().getOperand();

            if (var.asLpRel().solutionValue() >= var.getThreshold()) {
                out.format("[ %s ] %s => %s\n",
                                    varKey,
                                    ConstantPool.get(operand),
                                    var.asStrRel());
                out2.println(ConstantPool.get(operand));

                if (needPrint) {
                    System.out.format("[ %s ] %s => %s\n",
                                      varKey,
                                      ConstantPool.get(operand),
                                      var.asStrRel());
                }
            }
        }

        if (needPrint) {
            $.progress("--- Acquire Operation ---");
        }
        out.format("\n--- Acquire Operation ---\n");
        out2.println("[ACQUIRE]");

        for (Map.Entry<String, SyncVar> e: SyncVar.getPool().entrySet()) {
            String varKey = e.getKey();
            SyncVar var = e.getValue();
            int operand = var.asLogEntry().getOperand();

            if (var.asLpAcq().solutionValue() >= var.getThreshold()) {
                out.format("[ %s ] %s => %s\n",
                                    varKey,
                                    ConstantPool.get(operand),
                                    var.asStrAcq());
                out2.println(ConstantPool.get(operand));

                if (needPrint) {
                    System.out.format("[ %s ] %s => %s\n",
                                      varKey,
                                      ConstantPool.get(operand),
                                      var.asStrAcq());
                }
            }
        }
        out.close();
        out2.close();
    }

    private void saveWindowInfo() {
        saveRelWindowInfo();
        saveAcqWindowInfo();
    }

    private void saveRelWindowInfo() {
        PrintWriter out = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "rel-window.info"));
        if (out == null) {
            return;
        }

        for (SyncWindow window: relWindowSet) {
            if (window.stream().anyMatch(v -> v.isRel())) {
                continue;
            }

            out.format("==============\n");
            for (SyncVar var: window) {
                if (!var.isAcq()) {
                    String op = var.asLogEntry().getOpType();
                    int operand = var.asLogEntry().getOperand();
                    out.format("%s %s => %s\n", op, ConstantPool.get(operand), var.asStrRel());
                }
            }
        }
        out.close();
    }

    private void saveAcqWindowInfo() {
        PrintWriter out = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "acq-window.info"));
        if (out == null) {
            return;
        }

        for (SyncWindow window: acqWindowSet) {
            if (window.stream().anyMatch(v -> v.isAcq())) {
                continue;
            }
            out.format("==============\n");
            for (SyncVar var: window) {
                if (!var.isRel()) {
                    String op = var.asLogEntry().getOpType();
                    int operand = var.asLogEntry().getOperand();
                    out.format("%s %s => %s\n", op, ConstantPool.get(operand), var.asStrAcq());
                }
            }
        }
        out.close();
    }

    private void saveVarInfo() {
        PrintWriter out = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "var.def"));
        if (out == null) {
            return;
        }

        PrintWriter outTmp = $.openWriter($.pathJoin(XINFER_RESULT_DIR, "duration.dist"));
        if (outTmp == null) {
            return;
        }

        for (Map.Entry<String, SyncVar> e: SyncVar.getPool().entrySet()) {
            String varKey = e.getKey();
            SyncVar var = e.getValue();

            int operand = var.asLogEntry().getOperand();
            out.format("[ %s ] %s | %s = %f | %f X %s = %f\n",
                       varKey,
                       ConstantPool.get(operand),
                       var.asStrRel(),
                       var.asLpRel().solutionValue(),
                       var.getAcqCoff(),
                       var.asStrAcq(),
                       var.asLpAcq().solutionValue());

            outTmp.format("%f\n", $.standardDeviation(var.getDurations()));
            // System.out.format("%f\n", $.standardDeviation(var.getDurations()));
        }
        out.close();
        outTmp.close();
    }

    // ===========================================

    private void dataRaceEncode(LogPool pool) {
        int nrWindow = 0;

        HashMap<Integer, LogEntry> objLastWrite = new HashMap<>();
        HashMap<Integer, ArrayList<LogEntry>> objLastReadSet = new HashMap<>();

        Iterator<LogEntry> iter = pool.iterator();
        while (iter.hasNext()) {
            LogEntry currEntry = iter.next();
            if (!(currEntry.isRead() || currEntry.isWrite())) {
                continue;
            }

            int objectId = currEntry.getObjectId();

            if (currEntry.isRead()) {
                LogEntry lastWrite = objLastWrite.get(objectId);
                if (lastWrite != null) {
                    encodeRacePair(pool, lastWrite, currEntry);
                    nrWindow += 1;
                }

                ArrayList<LogEntry> readSet = objLastReadSet.get(objectId);
                if (readSet == null) {
                    readSet = new ArrayList<>();
                    objLastReadSet.put(objectId, readSet);
                }

                readSet.add(currEntry);
            } else if (currEntry.isWrite()) {
                LogEntry lastWrite = objLastWrite.get(objectId);
                if (lastWrite != null) {
                    encodeRacePair(pool, lastWrite, currEntry);
                    nrWindow += 1;
                }

                ArrayList<LogEntry> readSet = objLastReadSet.get(objectId);
                if (readSet != null) {
                    for (LogEntry read: readSet) {
                        encodeRacePair(pool, read, currEntry);
                        nrWindow += 1;
                    }
                    readSet.clear();
                }

                objLastWrite.put(objectId, currEntry);
            }
        }
        System.out.format("    |_ #Window: %d\n", nrWindow);
    }

    private void encodeRacePair(LogPool pool,
                                LogEntry firstLogEntry,
                                LogEntry secondLogEntry) {
        if (firstLogEntry.isClose(secondLogEntry) &&
            firstLogEntry.isConflict(secondLogEntry)) {
            long start_tsc = firstLogEntry.getTsc();
            long end_tsc = secondLogEntry.getTsc();

            encodeSyncWindow(pool.getThreadLog(firstLogEntry.getThreadId()),
                             pool.getThreadLog(secondLogEntry.getThreadId()),
                             start_tsc,
                             end_tsc);
        }
    }

    private void encodeSyncWindow(ArrayList<LogEntry> threadLog1,
                                  ArrayList<LogEntry> threadLog2,
                                  long startTsc,
                                  long endTsc) {
        LogEntryWindow relLogWindow = LogList.rangeOf(threadLog1,
                                                      startTsc,
                                                      endTsc,
                                                      true);
        //
        // Heuristic: remove operations (repeated >=2) in the window
        //
        relLogWindow.removeReplication();

        //
        // For acquiring sites, implementing window + 1
        // Add a log whose tsc < start_tsc
        //
        LogEntryWindow acqLogWindow = LogList.rangeOf(threadLog2,
                                                      startTsc,
                                                      endTsc,
                                                     false);
        //
        // Heuristic: remove operations (repeated >=2) in the window
        //
        acqLogWindow.removeReplication();

        //
        // Heuristic: consider only shared objects in both windows
        //
        Set<Integer> relObjectIds = relLogWindow.getObjectIds();
        Set<Integer> acqObjectIds = acqLogWindow.getObjectIds();

        relObjectIds.retainAll(acqObjectIds);
        Set<Integer> sharedObjectIds = relObjectIds;

        relLogWindow.filterBy(sharedObjectIds);
        acqLogWindow.filterBy(sharedObjectIds);

        //
        // Heuristic: cut off by the first Delay
        // Only operations before the first Delay may be real releases
        // This only works for delay logs because normal logs does not contain delay events
        //
        relLogWindow.truncateByFirstDelay();

        //
        // Heuristic: favour the last operation in the releasing window
        //
        if (!relLogWindow.isEmpty()) {
            SyncVar.relVar(relLogWindow.getLast()).incRelProb();
        }

        //
        // Heuristic: just encode constraints for call operations now
        // i.e., exit for releasing & enter for acquiring
        //
        SyncWindow relWindow = new SyncWindow();
        for (LogEntry logEntry: relLogWindow) {
            if (logEntry.isExit()) {
                relWindow.add(SyncVar.relVar(logEntry));
            }
        }
        addRelWindow(relWindow);

        SyncWindow acqWindow = new SyncWindow();
        for (LogEntry logEntry: acqLogWindow) {
            if (logEntry.isEnter()) {
                acqWindow.add(SyncVar.acqVar(logEntry));
            }
        }
        addAcqWindow(acqWindow);

        //
        // Heuristic:
        // For each operation (whose releasing probability V_rel) in the release window,
        // it is opposite to operations (whose acquiring probability V_acq)
        // in the acquire window) where (tsc of V_acq) < (tsc of V_rel).
        // I.e., if V_rel is true, then V_acq must be false.
        //
        for (LogEntry relLogEntry: relLogWindow) {
            for (LogEntry acqLogEntry: acqLogWindow) {
                if (acqLogEntry.getTsc() < relLogEntry.getTsc()) {
                    oppositePairs.add(new MPVariablePair(
                        SyncVar.acqVar(acqLogEntry), SyncType.ACQUIRE,
                        SyncVar.relVar(relLogEntry), SyncType.RELEASE
                    ));
                }
            }
        }
    }

    private void addAcqWindow(SyncWindow window) {
        if (!window.isEmpty()) {
            acqWindowSet.add(window);
        }
    }

    private void addRelWindow(SyncWindow window) {
        if (!window.isEmpty()) {
            relWindowSet.add(window);
        }
    }

    // ===========================================

    private void lpEncodeHeuristic() {
        for (SyncVar var: SyncVar.getPool().values()) {
            LogEntry logEntry = var.asLogEntry();

            if (logEntry.isThreadStart()) {
                var.markAsAcq();
                LpSolver.eqOne(var.asLpAcq());
            } else if (logEntry.isThreadEnd()) {
                var.markAsRel();
                LpSolver.eqOne(var.asLpRel());
            } else if (logEntry.isRead()) {
                var.markAsAcq();
            } else if (logEntry.isWrite()) {
                var.markAsRel();
            }
        }
    }

    private void lpEncodeRelWindow() {
        for (SyncWindow window: relWindowSet) {
            //
            // If there has been a release operations
            //
            if (window.stream().anyMatch(v -> v.isRel())) {
                continue;
            }

            ArrayList<MPVariable> lpVarList = new ArrayList<>();
            for (SyncVar var: window) {
                if (!var.isAcq()) {
                    lpVarList.add(var.asLpRel());
                }
            }

            if (lpVarList.isEmpty()) {
                continue;
            }

            //
            // There is only one release operation
            //
            MPVariable penalty = LpSolver.makePenalty();
            lpVarList.add(penalty);

            LpSolver.sumGeOne(lpVarList);
        }
    }

    private void lpEncodeAcqWindow() {
        for (SyncWindow window: acqWindowSet) {
            //
            // If there has been a acquire operations
            //
            if (window.stream().anyMatch(v -> v.isAcq())) {
                continue;
            }

            ArrayList<MPVariable> lpVarList = new ArrayList<>();
            for (SyncVar var: window) {
                if (!var.isRel()) {
                    lpVarList.add(var.asLpAcq());
                }
            }

            if (lpVarList.isEmpty()) {
                continue;
            }

            //
            // There is only one acquire operation
            //
            MPVariable penalty = LpSolver.makePenalty();
            lpVarList.add(penalty);

            LpSolver.sumGeOne(lpVarList);
        }
    }

    private void lpEncodeVars() {
        //
        // For each variable/location, P_rel + P_acq <= 100
        //
        for (SyncVar var: SyncVar.getPool().values()) {
            ArrayList<MPVariable> lpVarList = new ArrayList<>();
            lpVarList.add(var.asLpAcq());
            lpVarList.add(var.asLpRel());
            LpSolver.sumLeOne(lpVarList);
        }
    }

    private void lpEncodeClassVars() {
        //
        // For each class, #release methods are close to #acquire methods
        //
        HashMap<String, ArrayList<SyncVar>> classVarList = new HashMap<>();

        for (SyncVar var: SyncVar.getPool().values()) {
            String className = var.asLogEntry().getOperandClassName();
            if (className == null) {
                continue;
            }

            ArrayList<SyncVar> varList = classVarList.get(className);
            if (varList == null) {
                varList = new ArrayList<SyncVar>();
                classVarList.put(className, varList);
            }

            varList.add(var);
        }

        for (ArrayList<SyncVar> varList: classVarList.values()) {
            ArrayList<MPVariable> lpRelVarList = new ArrayList<>();
            ArrayList<MPVariable> lpAcqVarList = new ArrayList<>();

            for (SyncVar var: varList) {
                lpRelVarList.add(var.asLpRel());
                lpAcqVarList.add(var.asLpAcq());
            }

            LpSolver.sumClose(lpRelVarList, lpAcqVarList);
        }
    }

    private void lpEncodePairs() {
        for (MPVariablePair pair: equalPairs) {
            LpSolver.equal(pair.getFirst(), pair.getSecond());
        }

        for (MPVariablePair pair: oppositePairs) {
            LpSolver.opposite(pair.getFirst(), pair.getSecond());
        }
    }

    private void lpEncodeObject() {
        //
        // Object function: min(penalty + all variables)
        //
        // ArrayList<MPVariable> lpVarList = new ArrayList<>();

        // for (SyncVar var: SyncVar.getPool().values()) {
            // lpVarList.add(var.asLpAcq());
            // lpVarList.add(var.asLpRel());
        // }
        LpSolver.object();
    }
}
