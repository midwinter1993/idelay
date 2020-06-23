package io.github.midwinter1993;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class LogList {

    public static ArrayList<LogEntry> loadLogFile(String logPath,
                                                  Set<Integer> tlOperands) {
        ArrayList<LogEntry> logEntries = new ArrayList<>();

        logEntries.add(new LogEntry("0|0|Enter|-1|null"));

        try {
            BufferedReader reader = new BufferedReader(new FileReader(logPath));

            String line = reader.readLine();
            while (line != null) {
                LogEntry e = new LogEntry(line.trim());
                if (tlOperands == null || !tlOperands.contains(e.getOperandId())) {
                    logEntries.add(e);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        logEntries.add(new LogEntry("0|0|Exit|-1|null"));

        logEntries.get(0).setTsc(logEntries.get(1).getTsc() - 1);
        int sz = logEntries.size();
        logEntries.get(sz - 1).setTsc(logEntries.get(sz - 2).getTsc() + 1);

        Comparator<LogEntry> cmp = new Comparator<LogEntry>() {
            public int compare(LogEntry l1, LogEntry l2) {
                return Long.compare(l1.getTsc(), l2.getTsc());
            }
        };

        logEntries.sort(cmp);

        return logEntries;
    }

    public static LogEntryWindow rangeOf(ArrayList<LogEntry> logEntries, long startTsc,
            long endTsc, boolean leftOneMore) {
        // Find log entries whose tsc: start_tsc < tsc < end_tsc
        // When left_one_more is True, add one more log whose tsc may be less then start_tsc
        Comparator<LogEntry> cmp = new Comparator<LogEntry>() {
            public int compare(LogEntry l1, LogEntry l2) {
                return Long.compare(l1.getTsc(), l2.getTsc());
            }
        };

        int leftIndex = Collections.binarySearch(logEntries, new LogEntry(startTsc), cmp);
        int rightIndex = Collections.binarySearch(logEntries, new LogEntry(endTsc), cmp);

        if (leftIndex < 0) {
            leftIndex = -leftIndex + 1;
        } else {
            leftIndex += 1;
        }
        if (leftOneMore && leftIndex > 0) {
            leftIndex -= 1;
        }

        if (rightIndex < 0) {
            rightIndex = -rightIndex + 1;
        }
        if (rightIndex >= logEntries.size()) {
            rightIndex = logEntries.size();
        }

        LogEntryWindow results = new LogEntryWindow();
        for (int i = leftIndex; i < rightIndex; ++i) {
            results.add(logEntries.get(i));
        }

        return results;
    }
}
