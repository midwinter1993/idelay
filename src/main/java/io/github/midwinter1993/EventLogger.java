package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javassist.bytecode.ConstPool;

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

    public String compactToString(HashMap<String, Integer> constantPool) {
        Integer operandUid = constantPool.get(operand);

        if (operandUid == null) {
            operandUid = constantPool.size() + 1;
            constantPool.put(operand, operandUid);
        }

        if (objId != 0) {
            return String.format("%d|%d|%s|%d|", tsc, objId, opType, operandUid );
        } else {
            return String.format("%d|null|%s|%d|", tsc, opType, operandUid);
        }
    }
}


class EventLogger extends Executor {

    private AtomicBoolean needLogging = new AtomicBoolean(true);
    private AtomicBoolean stop = new AtomicBoolean(false);

    private ConcurrentHashMap<Long, ArrayList<LogEntry>> threadLogBuffer =
            new ConcurrentHashMap<Long, ArrayList<LogEntry>>();

    public EventLogger() {
        startWindowThread();
    }

    private void startWindowThread() {
        needLogging.set(false);

        Thread thread = new Thread() {
            public void run() {
                while (!stop.get()) {
                    try {
                        needLogging.set(true);
                        Thread.sleep(1000);

                        if (stop.get()) {
                            break;
                        }

                        needLogging.set(false);
                        Thread.sleep(5000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }

    private ArrayList<LogEntry> getThreadLogBuffer() {
        Long tid = $.getTid();

        if (!threadLogBuffer.containsKey(tid)) {
            threadLogBuffer.put(tid, new ArrayList<LogEntry>());
        }
        return threadLogBuffer.get(tid);
    }

    private HashMap<String, Integer> constantPool = new HashMap<>();

    private void saveAllThreadLog() {
        $.mkdir(Constant.LITE_LOG_DIR);
        threadLogBuffer.forEach((tid, threadLog) -> saveThreadLog(tid, threadLog));

        Dumper.dumpMap($.pathJoin(Constant.LITE_LOG_DIR, "map.cp"), constantPool);
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
                // writer.println(logEntry.toString());
                writer.println(logEntry.compactToString(constantPool));

                // System.out.println(logEntry.toString());
                // System.out.println(logEntry.compactToString(constantPool));
                // System.out.println("---");
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
        stop.set(true);
        saveAllThreadLog();
    }

    // ===========================================

    private ConcurrentHashMap<Integer, Long> objectAccess = new ConcurrentHashMap<>();

    /**
     * This method is not fully thread safe, though there is no data races,
     * it contains atomicity violation.
     */
    boolean isAccessedByMultiThread(long tid, Object obj) {
        int objId = System.identityHashCode(obj);
        Long bitMap = objectAccess.get(objId);

        if (bitMap != null) {
            //
            // Set `1` bit for current thread
            //
            bitMap = bitMap | (1 << (tid % 63));

            if ((bitMap & (bitMap - 1)) == 0) {
                //
                // There is only one `1` set in the bitmap
                //
                return false;
            } else {
                objectAccess.put(objId, bitMap);
                return true;
            }
        } else {
            long newBitMap = 1 << (tid % 63);
            objectAccess.put(objId, newBitMap);
            return false;
        }
    }

    @Override
    public void methodEnter(CallInfo callInfo) {
        if (!needLogging.get()) {
            return;
        }

        if (isAccessedByMultiThread($.getTid(), callInfo.getObject())) {
            getThreadLogBuffer().add(LogEntry.call(callInfo, "Enter"));
        }
    }

    @Override
    public void methodExit(CallInfo callInfo) {
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), callInfo.getObject())) {
            getThreadLogBuffer().add(LogEntry.call(callInfo, "Exit"));
        }
    }

    @Override
    public void beforeRead(Object target, String fieldName, String location) {
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.access(target, "R", fieldName, location));
        }
    }

    @Override
    public void beforeWrite(Object target, String fieldName, String location) {
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.access(target, "W", fieldName, location));
        }
    }

    @Override
    public void monitorEnter(Object target, String location) {
        if (!needLogging.get()) {
            return;
        }
        getThreadLogBuffer().add(LogEntry.monitor(target, "Enter", location));
    }

    @Override
    public void monitorExit(Object target, String location) {
        if (!needLogging.get()) {
            return;
        }
        getThreadLogBuffer().add(LogEntry.monitor(target, "Exit", location));
    }
}