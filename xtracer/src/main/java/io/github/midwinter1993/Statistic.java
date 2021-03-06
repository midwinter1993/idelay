package io.github.midwinter1993;

import java.util.ArrayList;

final public class Statistic extends Executor {
    private static ThreadLocal<ArrayList<Long>> tlCallTsc = new ThreadLocal<ArrayList<Long>>() {
        @Override
        protected ArrayList<Long> initialValue() {
            return new ArrayList<Long>();
        }
    };

    @Override
    public void methodEnter(Object target, String methodName, String location) {
        tlCallTsc.get().add($.getTsc());
    }

    @Override
    public void threadExit() {
        System.err.format(" | %s id %d: %d\n", Thread.currentThread().toString(),
        Thread.currentThread().getId(),
        tlCallTsc.get().size());
    }
}