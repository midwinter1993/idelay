package io.github.midwinter1993;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class SyncWindow extends LinkedList<SyncVar> {

    /**
     *
     */
    private static final long serialVersionUID = 1540009578034438356L;

    public void removeReplication() {
        HashMap<SyncVar, Integer> counter = new HashMap<>();
        for (SyncVar var: this) {
            Integer v = counter.get(var);
            if (v == null) {
                counter.put(var, 1);
            } else {
                counter.put(var, v + 1);
            }
        }

        Iterator<SyncVar> iter = this.iterator();
        while (iter.hasNext()) {
            SyncVar var = iter.next();
            if (counter.get(var) > 1) {
                iter.remove();
            }
        }
    }
}