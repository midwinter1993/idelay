package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class LogEntry {
    public long seqId;
    public long tsc;
    public int objId;
    public String opType;
    public String operand;
    public String location;

    private static final AtomicLong seqCount = new AtomicLong();

    public LogEntry(long tsc, Object obj, String opType, String operand, String location) {
        this.seqId = seqCount.incrementAndGet();
        this.tsc = tsc;
        this.objId = System.identityHashCode(obj);
        this.opType = opType;
        this.operand = operand;
        this.location = location;
    }

    public static LogEntry call(CallInfo callInfo, String opType) {
        return new LogEntry(callInfo.getTsc(), callInfo.getObject(), opType,
                callInfo.getCallee().getName(), callInfo.getLocation());
    }

    public static LogEntry access(Object obj, String opType, String fieldName, String location) {
        return new LogEntry($.getTsc(), obj, opType, fieldName, location);
    }

    public static LogEntry monitor(Object obj, String opType, String location) {
        return new LogEntry($.getTsc(), obj, opType, "Monitor", location);
    }

    public String toString() {
        if (objId != 0) {
            return String.format("%d|%d|%s|%s|%s", tsc, objId, opType, operand, location);
        } else {
            return String.format("%d|null|%s|%s|%s", tsc, opType, operand, location);
        }
    }
}


class EventLogger extends Executor {
    private ConcurrentHashMap<Long, ArrayList<LogEntry>> threadLogBuffer =
            new ConcurrentHashMap<Long, ArrayList<LogEntry>>();

    private ArrayList<LogEntry> getThreadLogBuffer() {
        Long tid = $.getTid();

        if (!threadLogBuffer.containsKey(tid)) {
            threadLogBuffer.put(tid, new ArrayList<LogEntry>());
        }
        return threadLogBuffer.get(tid);
    }

    private void saveAllThreadLog() {
        $.mkdir(Constant.LITE_LOG_DIR);
        threadLogBuffer.forEach((tid, threadLog) -> saveThreadLog(tid, threadLog));
    }

    private void saveThreadLog(long tid, ArrayList<LogEntry> threadLog) {
        if (threadLog.size() == 0) {
            $.warn("[ LOGGER ]", "Thread: `%d` log empty", tid);
            return;
        } else {
            $.info("[ Thread %d log size %d ]\n", tid, threadLog.size());
        }

        String fileName = String.format("%d.litelog", tid);
        String filePath = $.pathJoin(Constant.LITE_LOG_DIR, fileName);

        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");

            for (LogEntry logEntry: threadLog) {
                writer.println(logEntry.toString());
            }

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void vmDeath() {
        System.err.println("VM EXIT");
        saveAllThreadLog();
    }

    // ===========================================

    @Override
    public void methodEnter(CallInfo callInfo) {
        getThreadLogBuffer().add(LogEntry.call(callInfo, "Enter"));
    }

    @Override
    public void methodExit(CallInfo callInfo) {
        getThreadLogBuffer().add(LogEntry.call(callInfo, "Exit"));
    }
    @Override
    public void beforeRead(Object target, String fieldName, String location) {
        getThreadLogBuffer().add(LogEntry.access(target, "R", fieldName, location));
    }

    @Override
    public void beforeWrite(Object target, String fieldName, String location) {
        getThreadLogBuffer().add(LogEntry.access(target, "W", fieldName, location));
    }

    @Override
    public void monitorEnter(Object target, String location) {
        getThreadLogBuffer().add(LogEntry.monitor(target, "Enter", location));
    }

    @Override
    public void monitorExit(Object target, String location) {
        getThreadLogBuffer().add(LogEntry.monitor(target, "Exit", location));
    }
}
