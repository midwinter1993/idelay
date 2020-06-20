package io.github.midwinter1993;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class LogEntryWindow extends LinkedList<LogEntry> {
    /**
     *
     */
    private static final long serialVersionUID = 6753551678670062603L;

    public void removeReplication() {
        HashMap<LogEntry, Integer> counter = new HashMap<>();

        for (LogEntry logEntry: this) {
            Integer v = counter.get(logEntry);
            if (v == null) {
                counter.put(logEntry, 1);
            } else {
                counter.put(logEntry, v + 1);
            }
        }

        Iterator<LogEntry> iter = this.iterator();
        while (iter.hasNext()) {
            LogEntry logEntry = iter.next();
            if (counter.get(logEntry) > 1) {
                iter.remove();
            }
        }
    }

    public void truncateByFirstDelay() {
    }

    public Set<Integer> getObjectIds() {
        HashSet<Integer> objectIds = new HashSet<>();

        for (LogEntry logEntry: this) {
            objectIds.add(logEntry.getObjectId());
        }

        return objectIds;
    }

    public void filterBy(Set<Integer> objectIds) {
        Iterator<LogEntry> iter = this.iterator();
        while (iter.hasNext()) {
            LogEntry logEntry = iter.next();
            if (!objectIds.contains(logEntry.getObjectId())) {
                iter.remove();
            }
        }
    }
}