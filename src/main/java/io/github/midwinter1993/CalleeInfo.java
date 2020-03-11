package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicInteger;

class CalleeInfo {
    private static AtomicInteger uidCount = new AtomicInteger(1);

    private String name;
    private int uid;
    private boolean isSync = false;

    public CalleeInfo(String name) {
        this.name = name;
        this.uid = uidCount.getAndIncrement();
    }

    public String getName() {
        return name;
    }

    public int getUid() {
        return uid;
    }

    public void setSynchronized() {
        isSync = true;
    }

    public boolean isSynchronized() {
        return isSync;
    }
}