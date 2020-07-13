package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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

        if (!threadLogBuffer.containsKey(tid)) {
            threadLogBuffer.put(tid, new ArrayList<LogEntry>());
        }
        return threadLogBuffer.get(tid);
    }

    protected void addThreadLogEntry(LogEntry entry) {
        getThreadLogBuffer().add(entry);
    }

    private ConstantPool constantPool = new ConstantPool();

    private void saveAllThreadLog() {
        $.mkdir(logDir);
        $.info("#Threads: %d \n", threadLogBuffer.size());
        threadLogBuffer.forEach((tid, threadLog) -> saveThreadLog(tid, threadLog));

        Dumper.dumpMap($.pathJoin(logDir, "map.cp"), constantPool);
    }

    private void saveThreadLog(long tid, ArrayList<LogEntry> threadLog) {
        if (threadLog.size() == 0) {
            $.warn("[ LOGGER ]", "Thread: `%d` log empty", tid);
            return;
        } else {
            $.info("[ Thread %d log size %d ]\n", tid, threadLog.size());
        }

        String liteLogName = String.format("%d.litelog", tid);
        String liteLogPath = $.pathJoin(logDir, liteLogName);

        String tlLogName = String.format("%d.tl-litelog", tid);
        String tlLogPath = $.pathJoin(logDir, tlLogName);

        try {
            PrintWriter liteLogWriter = new PrintWriter(liteLogPath, "UTF-8");
            PrintWriter tlLogWriter = new PrintWriter(tlLogPath, "UTF-8");

            for (LogEntry logEntry: threadLog) {
                if (!isThreadLocal(logEntry.getObjId())) {
                    liteLogWriter.println(logEntry.compactToString(constantPool));
                } else {
                    if (logEntry.isEnter()) {
                        tlLogWriter.println(logEntry.compactToString(constantPool));
                    }
                }
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

    private ConcurrentHashMap<Integer, Long> objectAccess = new ConcurrentHashMap<>();

    /**
     * This method is not fully thread safe, though there is no data races,
     * it contains atomicity violation.
     */
    private boolean isAccessedByMultiThread(long tid, Object obj) {
        int objId = System.identityHashCode(obj);
        Long bitMap = objectAccess.get(objId);

        if (bitMap != null) {
            //
            // Has been accessed by multiple threads
            //
            if ((bitMap & (bitMap - 1)) != 0) {
                return true;
            }

            //
            // Set `1` bit for current thread
            // And update bitmap for object
            //
            bitMap = bitMap | (1 << (tid % 63));
            objectAccess.put(objId, bitMap);

            if ((bitMap & (bitMap - 1)) == 0) {
                //
                // There is only one `1` set in the bitmap
                //
                return false;
            } else {
                return true;
            }
        } else {
            long newBitMap = 1 << (tid % 63);
            objectAccess.put(objId, newBitMap);
            return false;
        }
    }

    private boolean isThreadLocal(int objId) {
        Long bitMap = objectAccess.get(objId);

        if (bitMap == null) {
            return true;
        }

        //
        // Whether there is only one `1` set in the bitmap
        //
        if ((bitMap & (bitMap - 1)) == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void methodEnter(Object target, String methodName, String location) {
        if (!needLogging.get()) {
            return;
        }

        if (methodName.contains("iterate")) {
            System.out.println(methodName);
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.call(target, "Enter", methodName, location));
        }
    }

    @Override
    public void methodExit(Object target, String methodName, String location) {
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.call(target, "Exit", methodName, location));
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