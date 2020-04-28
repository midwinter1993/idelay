package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

    public static LogEntry call(CallInfo callInfo) {
        return new LogEntry(
            callInfo.getTsc(),
             callInfo.getObject(),
             "C",
             callInfo.getCallee().getName(),
             callInfo.getLocation()
        );
    }

    public static LogEntry access(Object obj, String opType, String fieldName, String location) {
        return new LogEntry(
            $.getTsc(),
            obj,
            opType,
            fieldName,
            location
        );
    }

    public static LogEntry monitor(Object obj, String operand, String location) {
        return new LogEntry(
            $.getTsc(),
            obj,
            "C",
            operand,
            location
        );
    }

    public String toString() {
        return String.format("%d,%d,%s,%s,%s", tsc, objId, opType, operand, location);
    }
}


class EventLogger extends Executor {
    private static ThreadLocal<ArrayList<LogEntry>> tlLogBuffer =
            new ThreadLocal<ArrayList<LogEntry>>() {
                @Override
                protected ArrayList<LogEntry> initialValue() {
                    return new ArrayList<LogEntry>();
                }
            };

    @Override
    public void methodEvent(CallInfo callInfo) {
        tlLogBuffer.get().add(LogEntry.call(callInfo));
    }

    @Override
    public void threadStart() {
        System.out.format("Start %d\n", $.getTid());;
    }

    @Override
    public void threadExit() {
        System.out.format("End %d %d\n", $.getTid(), tlLogBuffer.get().size());;
        $.mkdir(Constant.LITE_LOG_DIR);

        String fileName = String.format("%d.litelog", $.getTid());
        String filePath = $.pathJoin(Constant.LITE_LOG_DIR, fileName);

        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");

            // ArrayList<LogEntry> logBuffer = tlLogBuffer.get();
            for (LogEntry logEntry : tlLogBuffer.get()) {
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
    }

    @Override
    public void beforeRead(Object target, String fieldName, String location) {
        // System.err.print("READ\n");

        tlLogBuffer.get().add(LogEntry.access(target, "R", fieldName, location));
    }

    @Override
    public void beforeWrite(Object target, String fieldName, String location) {
        // System.err.print("WRITE\n");
        tlLogBuffer.get().add(LogEntry.access(target, "W", fieldName, location));
    }

    @Override
    public void monitorEnter(Object target, String location) {
        tlLogBuffer.get().add(LogEntry.monitor(target, "monitor.enter", location));
        System.err.format("Monitor ENTER %d %d\n", $.getTid(), tlLogBuffer.get().size());
    }

    @Override
    public void monitorExit(Object target, String location) {
        // System.err.print("Monitor EXIT\n");
        tlLogBuffer.get().add(LogEntry.monitor(target, "monitor.exit", location));
    }
}