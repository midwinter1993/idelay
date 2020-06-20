package io.github.midwinter1993;

import java.util.HashMap;

public class LogEntry {
    public long tsc;
    public int objId;
    public String opType;
    public String operand;
    public String location;

    public LogEntry(long tsc, Object obj, String opType, String operand, String location) {
        this.tsc = tsc;
        this.objId = System.identityHashCode(obj);
        this.opType = opType;
        this.operand = operand;
        this.location = location;
    }

    public int getObjId() {
        return objId;
    }

    public boolean isEnter() {
        return opType.equals("Enter");
    }

    public static LogEntry call(CallInfo callInfo, String opType) {
        return new LogEntry(callInfo.getTsc(), callInfo.getObject(), opType,
                callInfo.getCallee().getName(), callInfo.getLocation());
    }

    public static LogEntry access(Object obj, String opType, String fieldName, String location) {
        return new LogEntry($.getTsc(), obj, opType, fieldName, location);
    }

    public static LogEntry monitor(Object obj, String opType, String location) {
        return new LogEntry($.getTsc(), obj, opType, "Mark.Monitor", location);
    }

    public static LogEntry delay() {
        return new LogEntry($.getTsc(), null, "Enter", "Mark.Delay", null);
    }

    public String toString() {
        if (objId != 0) {
            return String.format("%d|%d|%s|%s|%s", tsc, objId, opType, operand, location);
        } else {
            return String.format("%d|null|%s|%s|%s", tsc, opType, operand, location);
        }
    }

    public String compactToString(HashMap<String, Integer> constantPool) {
        Integer operandUid = constantPool.get(operand);

        if (operandUid == null) {
            operandUid = constantPool.size() + 1;
            constantPool.put(operand, operandUid);
        }

        return String.format("%d|%d|%s|%d|", tsc, objId, opType, operandUid );
        // if (objId != 0) {
            // return String.format("%d|%d|%s|%d|", tsc, objId, opType, operandUid );
        // } else {
            // return String.format("%d|null|%s|%d|", tsc, opType, operandUid);
        // }
    }
}

