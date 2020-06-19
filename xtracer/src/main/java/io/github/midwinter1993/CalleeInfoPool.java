package io.github.midwinter1993;

import java.util.concurrent.ConcurrentHashMap;

class CalleeInfoPool {
    private static ConcurrentHashMap<String, CalleeInfo> calleeNameMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, CalleeInfo> calleeUidMap = new ConcurrentHashMap<>();

    public static void addCalleeInfo(CalleeInfo info) {
        calleeNameMap.put(info.getName(), info);
        calleeUidMap.put(info.getUid(), info);
    }

    public static CalleeInfo getByName(String name) {
        return calleeNameMap.get(name);
    }

    public static CalleeInfo getByUid(int uid) {
        return calleeUidMap.get(uid);
    }
}