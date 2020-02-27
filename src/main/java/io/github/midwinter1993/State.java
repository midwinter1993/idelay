package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class State {

    private static ThreadLocal<CallInfo> tlBufferedCallInfo = new ThreadLocal<CallInfo>() {
        @Override
        protected CallInfo initialValue() {
            return new CallInfo();
        }
    };

    public static CallInfo getThreadCallInfo() {
        return tlBufferedCallInfo.get();
    }

    // ===============================================================

    private static ThreadLocal<Long> tlLastCallTsc = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return new Long(System.nanoTime());
        }
    }; 

    public static long getThreadLastCallTsc() {
        return tlLastCallTsc.get();
    }

    public static void putThreadCallTsc(CallInfo callInfo) {
        tlLastCallTsc.set(callInfo.getTsc());
    }

    // ===============================================================

    private static AtomicInteger numberOfThreads = new AtomicInteger(0);
    private static ThreadLocal<Integer> tlToken = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return new Integer(1);
        }
    };

    public static void mergeThreadToken() {
        //
        // Each thread can only merge one token
        //
        numberOfThreads.getAndAdd(tlToken.get());
        tlToken.set(0);
    }

    public static int getNumberOfThreads() {
        return numberOfThreads.get();
    }

    // ===============================================================

    private static AtomicReference<CallInfo> currentDelayedCall = new AtomicReference<CallInfo>();
    private static AtomicReference<CallInfo> lastDelayedCall = new AtomicReference<CallInfo>();

    public static CallInfo getCurrentDelayedCall() {
        return currentDelayedCall.get();
    }

    public static boolean setCurrentDelayedCall(CallInfo call) {
        return currentDelayedCall.compareAndSet(null, call);
    }

    public static boolean clearCurrentDelayedCall(CallInfo call) {
        lastDelayedCall.set(call);
        return currentDelayedCall.compareAndSet(call, null);
    }

    public static CallInfo getLastDelayedCall() {
        return lastDelayedCall.get();
    }
}