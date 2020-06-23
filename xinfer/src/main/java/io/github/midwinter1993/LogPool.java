package io.github.midwinter1993;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class LogPool implements Iterable<LogEntry> {

    private HashMap<Integer, ArrayList<LogEntry>> threadLogMap = new HashMap<>();
    private String logDir = null;

    public LogPool(String logDir) {
        this.logDir = logDir;
        load(logDir);
    }

    private void load(String logDir) {
        HashSet<Integer> tlOperands = loadTlLog(logDir);

        $.progress("Load Log");

        File f = new File(logDir);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".litelog");
            }
        };

        //
        // Load the lite log by thread ID
        // TODO: paralleled
        //
        for (String name: f.list(filter)) {
            String threadId = name.replaceFirst("[.][^.]+$", "");
            String logPath = $.pathJoin(logDir, name);
            threadLogMap.put(Integer.parseInt(threadId),
                             LogList.loadLogFile(logPath, tlOperands));
        }

        int nrTot = 0;
        int nrRead = 0;
        int nrWrite = 0;
        int nrCall = 0;

        for (Map.Entry<Integer, ArrayList<LogEntry>> e: threadLogMap.entrySet()) {
            Integer threadId = e.getKey();
            ArrayList<LogEntry> logEntries = e.getValue();

            Iterator<LogEntry> iter = logEntries.iterator();

            LogEntry prevLogEntry = null;
            while (iter.hasNext()) {
                LogEntry logEntry = iter.next();
                //
                // Patch thread id for each log entry
                //
                logEntry.setThreadId(threadId);

                //
                // Count durations for each SyncVar
                //
                if (prevLogEntry != null) {
                    long d = logEntry.getTsc() - prevLogEntry.getTsc();
                    prevLogEntry.setDuration(d);
                }
                prevLogEntry = logEntry;

                if (logEntry.isRead()) {
                    nrRead += 1;
                } else if (logEntry.isWrite()) {
                    nrWrite += 1;
                } else if (logEntry.isEnter() || logEntry.isExit()) {
                    nrCall += 1;
                }
            }
            nrTot += logEntries.size();
            System.out.format("  |_ Thread %d; log size: %d\n", threadId, logEntries.size());
        }

        System.out.format("  |_ #Total log entries: %d\n", nrTot);
        System.out.format("  |_ #Read log entries: %d\n", nrRead);
        System.out.format("  |_ #Write log entries: %d\n", nrWrite);
        System.out.format("  |_ #Enter/Exit log entries: %d\n", nrCall);
    }

    private HashSet<Integer> loadTlLog(String logDir) {
        $.progress("Load Thread Local Log");

        HashSet<Integer> tlOperands = new HashSet<>();

        File f = new File(logDir);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".tl-litelog");
            }
        };

        for (String name: f.list(filter)) {
            String logPath = $.pathJoin(logDir, name);
            for (LogEntry logEntry: LogList.loadLogFile(logPath, null)) {
                tlOperands.add(logEntry.getOperandId());
            }
        }

        return tlOperands;
    }

    public String getLogDir() {
        return logDir;
    }

    public ArrayList<LogEntry> getThreadLog(int threadId) {
        return threadLogMap.get(threadId);
    }

    @Override
    public Iterator<LogEntry> iterator() {
        return new OrderIterator(this);
    }

    class OrderIterator implements Iterator<LogEntry> {
        private LogPool pool = null;
        private HashMap<Integer, Integer> threadIndexMap = new HashMap<>();

        public OrderIterator(LogPool pool) {
            this.pool = pool;
            for (Integer threadId: pool.threadLogMap.keySet()) {
                threadIndexMap.put(threadId, 0);
            }
        }

        private Map.Entry<Integer, Integer> entryWithMinTsc() {
            Map.Entry<Integer, Integer> minEntry = null;
            long minTsc = Long.MAX_VALUE;

            for (Map.Entry<Integer, Integer> e: threadIndexMap.entrySet()) {
                int threadId = e.getKey();
                int index = e.getValue();
                long tsc = pool.getThreadLog(threadId).get(index).getTsc();
                if (tsc < minTsc) {
                    minEntry = e;
                    minTsc = tsc;
                }
            }

            return minEntry;
        }

        @Override
        public boolean hasNext() {
            return !threadIndexMap.isEmpty();
        }

        @Override
        public LogEntry next() {
            Map.Entry<Integer, Integer> e = entryWithMinTsc();
            int threadId = e.getKey();
            int index = e.getValue();

            if (index == pool.getThreadLog(threadId).size()-1) {
                threadIndexMap.remove(threadId);
            } else {
                threadIndexMap.put(threadId, index + 1);
            }

            return pool.getThreadLog(threadId).get(index);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}