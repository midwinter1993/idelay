package io.github.midwinter1993;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.ortools.linearsolver.MPVariable;

enum SyncType {
    RELEASE,
    ACQUIRE,
}

public class SyncVar {
    private static HashMap<String, SyncVar> varPool = new HashMap<>();

    public static HashMap<String, SyncVar> getPool() {
        return varPool;
    }

    public static SyncVar getVar(LogEntry logEntry) {
        String key = logEntry.getOperationFullStr();

        if (!varPool.containsKey(key)) {
            varPool.put(key, new SyncVar(key));
        }

        SyncVar var = varPool.get(key);
        var.logEntries.add(logEntry);
        return var;
    }

    public static SyncVar relVar(LogEntry logEntry) {
        return getVar(logEntry);
    }

    public static SyncVar acqVar(LogEntry logEntry) {
        return getVar(logEntry);
    }

    private String key = null;
    private HashSet<LogEntry> logEntries = new HashSet<>();

    MPVariable lpRelVar = null;
    MPVariable lpAcqVar = null;

    //
    // When the probability > threshold_, acq/rel is True
    //
    double threshold = 0.95;

    //
    // Used by heuristic
    //
    boolean isRel = false;
    boolean isAcq = false;

    private SyncVar(String key) {
        this.key = key;

        lpRelVar = LpSolver.makeVar(asStrRel());
        lpAcqVar = LpSolver.makeVar(asStrAcq());
    }

    public LogEntry asLogEntry() {
        //
        // Just pick the first item
        //
        return logEntries.iterator().next();
    }

    public double getThreshold() {
        return threshold;
    }

    public String asStrRel() {
        return "R_" + key;
    }

    public String asStrAcq() {
        return "A_" + key;
    }

    public MPVariable asLpRel()  {
        return lpRelVar;
    }

    public MPVariable asLpAcq()  {
        return lpAcqVar;
    }

    public boolean isAcq()  {
        return isAcq;
    }

    public boolean isRel()  {
        return isRel;
    }

    public void markAsAcq() {
        assert !isRel;
        isAcq = true;
    }

    public void markAsRel() {
        assert !isAcq;
        isRel = true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SyncVar)) {
            return false;
        }
        return key.equals(((SyncVar)o).key);
    }

    public ArrayList<Long> getDurations() {
        ArrayList<Long> durations = new ArrayList<>();
        for (LogEntry logEntry: logEntries) {
            durations.add(logEntry.getDuration());
        }
        return durations;
    }

    public double getAcqCoff() {
        double standardDeviation = $.standardDeviation(getDurations());
        double x = 1.0 - (standardDeviation / 2000.0);
        if (x < 0.2) {
            return 0.2;
        } else {
            return x;
        }
    }

    int nrRelOccurrence = 1;

    public void incRelProb() {
        nrRelOccurrence += 1;
    }

    public double getRelCoff() {
        return 1.0 / Math.pow(2, nrRelOccurrence);
    }
}