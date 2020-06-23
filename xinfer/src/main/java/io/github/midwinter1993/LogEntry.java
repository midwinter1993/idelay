package io.github.midwinter1993;

public class LogEntry {
    private long tsc = -1;
    private int objectId = -1;
    private String opType = null;
    private int operandId = -1;
    private String operandStr = null;
    private String location = null;
    private int threadId = -1;
    private long duration = 0;

    public LogEntry(long tsc) {
        this.tsc = tsc;
    }

    public LogEntry(String line) {
        String[] items = line.split("\\|");
        if (items.length != 4 && items.length != 5) {
            System.out.format("Parse log entry failure %s\n", line);
            for (String it: items) {
                System.out.println(it);
            }
        }

        tsc = Long.parseLong(items[0]);
        objectId = Integer.parseInt(items[1]);
        opType = items[2];
        if ($.isNumeric(items[3])) {
            operandId = Integer.parseInt(items[3]);
        } else {
            operandStr = items[3];
        }
        if (items.length == 5) {
            location = items[4];
        }
        threadId = -1;  // Fixed after log is loaded
    }

    private String operationCompactedStr = null;

    public String getOperationCompactedStr() {
        if (operationCompactedStr == null) {
            operationCompactedStr = String.format("%s:%d", opType, operandId);
        }
        return operationCompactedStr;
    }

    private String operationFullStr = null;

    public String getOperationFullStr() {
        if (operationFullStr == null) {
            operationFullStr =  String.format("%s:%s", opType, getOperandStr());
        }
        return operationFullStr;
    }

    public long getTsc() {
        return tsc;
    }

    public void setTsc(long tsc) {
        this.tsc = tsc;
    }

    public int getObjectId() {
        return objectId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public int getOperandId() {
        assert(operandId != -1);
        return operandId;
    }

    public String getOperandStr() {
        if (operandStr == null) {
            assert(operandId != -1);
            operandStr = ConstantPool.get(operandId);
        }
        return operandStr;
    }

    public String getOperandClassName() {
        //
        // X.Y.Z.foo(A.B.C)
        //          ^
        //          |-- leftParan
        //
        String operandStr = ConstantPool.get(operandId);
        if (operandStr == null) {
            return null;
        }

        int posLeftParan = operandStr.indexOf('(', 0);
        if (posLeftParan == -1) {
            posLeftParan = operandStr.length()-1;
        }

        int posDotBeforeMethodName = operandStr.lastIndexOf('.', posLeftParan);
        if (posDotBeforeMethodName == -1) {
            System.err.format("%sGet Class Name Failure: %s %s\n", Color.RED,
                                                                   operandStr,
                                                                   Color.RESET);
            return null;
        }

        return operandStr.substring(0, posDotBeforeMethodName);
    }

    public String getOpType() {
        return opType;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long d) {
        duration = d;
    }

    public boolean isWrite() {
        return opType.equals("W");
    }

    public boolean isRead() {
        return opType.equals("R");
    }

    public boolean isAccess() {
        return opType.equals("W") || opType.equals("R");
    }

    public boolean isEnter() {
        return opType.equals("Enter");
    }

    public boolean isExit()  {
        return opType.equals("Exit");
    }

    /**
     * The operand of Mark.Thread must be 1;
     * See @ConstantPool
     */
    public boolean isMonitorEnter() {
        // return isEnter() && operand == 1;
        return false;
    }

    public boolean isMonitorExit() {
        // return isExit() && operand == 1;
        return false;
    }

    /**
     * The operand of Mark.Delay must be 2;
     * See @ConstantPool
     */
    public boolean isDelay() {
        return opType.equals("Enter") &&  operandId == 2;
    }

    /**
     * The operand of Mark.Thread must be 3;
     * See @ConstantPool
     */
    public boolean isThreadStart() {
        return isEnter() && operandId == 3;
    }

    public boolean isThreadEnd() {
        return isExit() && operandId == 3;
    }

    public boolean isConflict(LogEntry another) {
        // There must be a write operation
        if (!(this.isWrite() || another.isWrite())) {
            return false;
        }

        // From different threads
        if (this.threadId == another.threadId) {
            return false;
        }

        // Access the same object
        if (this.objectId != another.objectId) {
            return false;
        }

        // Access the same field
        if (this.operandId != another.operandId) {
            return false;
        }

        return true;
    }

    public boolean isClose(LogEntry another) {
        final int DISTANCE = 1000000;

        if (tsc < another.tsc) {
            return another.tsc < tsc + DISTANCE;
        } else {
            return tsc < another.tsc + DISTANCE;
        }
    }

    @Override
    public String toString() {
        return String.format("%d|%d|%s|%d|", tsc, objectId, opType, operandId);
    }

    public String toFullString() {
        return String.format("%d|%d|%s|%s|", tsc, objectId, opType,
                                             getOperandStr());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LogEntry)) {
            return false;
        }
        LogEntry another = (LogEntry)o;
        if (tsc == another.tsc && objectId == another.objectId &&
            opType == another.opType && operandId == another.operandId &&
            location == another.location && threadId == another.threadId &&
            duration == another.duration) {
            return true;
        }
        return false;
    }
}