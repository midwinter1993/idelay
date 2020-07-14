package io.github.midwinter1993;

public class VarAccess {
    long lastTid = 0;
    LogEntry lastLogEntry = new LogEntry();

    Long accessBitMap = 0L;

    public long getLastTid() {
        return lastTid;
    }

    public LogEntry getLastLogEntry() {
        return lastLogEntry;
    }

    public void setAccess(long tsc, long tid, Object obj, String opType, String operand, String location) {
        lastTid = tid;
        lastLogEntry.setInfo(tsc, obj, opType, operand, location);
    }

    public boolean isAccessedByMultiThread(long tid) {
        //
        // Has been accessed by multiple threads
        //
        if ((accessBitMap & (accessBitMap - 1)) != 0) {
            return true;
        }

        //
        // Set `1` bit for current thread
        // And update accessBitMap for object
        //
        accessBitMap = accessBitMap | (1 << (tid % 63));

        if ((accessBitMap & (accessBitMap - 1)) == 0) {
            //
            // There is only one `1` set in the accessBitMap
            //
            return false;
        } else {
            return true;
        }
    }

    public boolean isThreadLocal() {
        //
        // Whether there is only one `1` set in the bitmap
        //
        if ((accessBitMap & (accessBitMap - 1)) == 0) {
            return true;
        } else {
            return false;
        }
    }
}