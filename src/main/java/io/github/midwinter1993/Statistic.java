package io.github.midwinter1993;

import java.util.ArrayList;

final public class Statistic {
    private static ThreadLocal<ArrayList<Long>> tlCallTsc = new ThreadLocal<ArrayList<Long>>() {
        @Override
        protected ArrayList<Long> initialValue() {
            return new ArrayList<Long>();
        }
    };

	public static void onMethodEvent(CallInfo callInfo) {
        tlCallTsc.get().add(callInfo.getTsc());
    }

    public static void onThreadExit() {
        System.err.format(" | %s id %d: %d\n", Thread.currentThread().toString(),
        Thread.currentThread().getId(),
        tlCallTsc.get().size());
    }
}