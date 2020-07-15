package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class EventLogger extends Executor {

    private AtomicBoolean needLogging = new AtomicBoolean(true);
    private AtomicBoolean stop = new AtomicBoolean(false);

    private ConcurrentHashMap<Long, ArrayList<LogEntry>> threadLogBuffer =
            new ConcurrentHashMap<Long, ArrayList<LogEntry>>();

    private String logDir = Constant.LITE_LOG_DIR;

    public EventLogger(String logDir) {
        this.logDir = logDir;
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
                        Thread.sleep(3000);

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
        return threadLogBuffer.computeIfAbsent(tid, k -> new ArrayList<>());
    }

    protected void addThreadLogEntry(LogEntry entry) {
        getThreadLogBuffer().add(entry);
    }

    private ConstantPool constantPool = new ConstantPool();

    private void saveAllThreadLog() {
        $.mkdir(logDir);
        $.info("#Threads: %d \n", threadLogBuffer.size());

        //
        // Merge all "first access" of object to threads
        //
        HashMap<Long, ArrayList<LogEntry>> threadObjFirstLog = new HashMap<>();
        for (Map.Entry<Integer, VarAccess> e: objectAccess.entrySet()) {
            VarAccess access = e.getValue();
            long tid = access.getLastTid();
            LogEntry logEntry = access.getLastLogEntry();
            //
            // For some variables, we don't record the first access for it,
            // so its logEntry is invalid.
            //
            if (logEntry.isValid()) {
                threadObjFirstLog.computeIfAbsent(tid, k -> new ArrayList<>()).add(logEntry);
            }
        }

        //
        // Sorting
        //
        threadObjFirstLog.forEach((tid, threadLog) -> Collections.sort(threadLog, LogEntry.getCmpByTsc()));

        for (Map.Entry<Long, ArrayList<LogEntry>> e: threadLogBuffer.entrySet()) {
            long tid = e.getKey();
            ArrayList<LogEntry> threadLog = e.getValue();
            saveThreadLog(tid, threadLog, threadObjFirstLog.get(tid));
        }

        for (Map.Entry<Long, ArrayList<LogEntry>> e: threadObjFirstLog.entrySet()) {
            long tid = e.getKey();
            if (!threadLogBuffer.containsKey(tid)){
                saveThreadLog(tid, null, threadObjFirstLog.get(tid));
            }
        }

        Dumper.dumpMap($.pathJoin(logDir, "map.cp"), constantPool);
    }

    private void saveLogEntry(PrintWriter liteLogWriter,
                              PrintWriter tlLogWriter,
                              LogEntry logEntry) {
        if (!isThreadLocal(logEntry.getObjId())) {
            liteLogWriter.println(logEntry.compactToString(constantPool));
        } else {
            if (logEntry.isEnter()) {
                tlLogWriter.println(logEntry.compactToString(constantPool));
            }
        }
    }

    private void saveThreadLog(long tid, ArrayList<LogEntry> threadLog1,
                                         ArrayList<LogEntry> threadLog2) {
        int logSize1  = threadLog1 != null? threadLog1.size():0;
        int logSize2  = threadLog2 != null? threadLog2.size():0;

        if (logSize1 == 0 && logSize2 == 0) {
            $.warn("[ LOGGER ]", "Thread: `%d` log empty", tid);
            return;
        } else {
            $.info("[ Thread %d log size %d ]\n", tid, logSize1 + logSize2);
        }

        String liteLogName = String.format("%d.litelog", tid);
        String liteLogPath = $.pathJoin(logDir, liteLogName);

        String tlLogName = String.format("%d.tl-litelog", tid);
        String tlLogPath = $.pathJoin(logDir, tlLogName);

        try {
            PrintWriter liteLogWriter = new PrintWriter(liteLogPath, "UTF-8");
            PrintWriter tlLogWriter = new PrintWriter(tlLogPath, "UTF-8");

            int pos1 = 0;
            int pos2 = 0;
            while (pos1 < logSize1 && pos2 < logSize2) {
                LogEntry logEntry1 = threadLog1.get(pos1);
                LogEntry logEntry2 = threadLog2.get(pos2);
                if (logEntry1.getTsc() < logEntry2.getTsc()) {
                    saveLogEntry(liteLogWriter, tlLogWriter, logEntry1);
                    pos1 += 1;
                } else {
                    saveLogEntry(liteLogWriter, tlLogWriter, logEntry2);
                    pos2 += 1;
                }
            }

            while (pos1 < logSize1) {
                saveLogEntry(liteLogWriter, tlLogWriter, threadLog1.get(pos1));
                pos1 += 1;
            }
            while (pos2 < logSize2) {
                saveLogEntry(liteLogWriter, tlLogWriter, threadLog2.get(pos2));
                pos2 += 1;
            }

            liteLogWriter.close();
            tlLogWriter.close();
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
        needLogging.set(false);
        saveAllThreadLog();
    }

    // ===========================================

    private ConcurrentHashMap<Integer, VarAccess> objectAccess = new ConcurrentHashMap<>();

    /**
     * This method is not fully thread safe, though there is no data races,
     * it contains atomicity violation.
     */
    private boolean isAccessedByMultiThread(long tid, Object obj) {
        int objId = System.identityHashCode(obj);

        VarAccess access = objectAccess.computeIfAbsent(objId, k -> new VarAccess());
        return access.isAccessedByMultiThread(tid);
    }

    private boolean isThreadLocal(int objId) {
        VarAccess access = objectAccess.get(objId);

        if (access == null) {
            return true;
        } else {
            return access.isThreadLocal();
        }
    }

    @Override
    public void methodEnter(Object target, String methodName, String location) {
        if (!needLogging.get()) {
            return;
        }

        //if (isAccessedByMultiThread($.getTid(), target)) {
        getThreadLogBuffer().add(LogEntry.call(target, "Enter", methodName, location));
        //}
    }

    @Override
    public void methodExit(Object target, String methodName, String location) {
	/*
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.call(target, "Exit", methodName, location));
        }
	*/
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
        } else {
            int objId = System.identityHashCode(target);
            //
            // We reuse the logEntry here instead of creating a new one
            // to avoid frequent "new" operation
            //
            VarAccess access = objectAccess.get(objId);
            access.setAccess($.getTsc(), $.getTid(), target, "W", fieldName, location);
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
