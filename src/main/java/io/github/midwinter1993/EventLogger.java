package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

class LogEntry {
    public long tsc;
    public int objId;
    public String opType;
    public String operand;
    public String location;

    public LogEntry(CallInfo callInfo) {
        tsc = callInfo.getTsc();
        objId = System.identityHashCode(callInfo.getObject());
        opType = "call";
        operand = callInfo.getCallee().getName();
        location = callInfo.getLocation();
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
    public void onMethodEvent(CallInfo callInfo) {
        tlLogBuffer.get().add(new LogEntry(callInfo));
    }

    @Override
    public void onThreadExit() {
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
    public void beforeRead(Object target) {
        System.err.print("READ\n");
    }

    @Override
    public void beforeWrite(Object target) {
        System.err.print("WRITE\n");
    }
}