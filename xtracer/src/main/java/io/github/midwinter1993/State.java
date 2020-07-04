package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    private static AtomicInteger numOfThreads = new AtomicInteger(0);
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
        numOfThreads.getAndAdd(tlToken.get());
        tlToken.set(0);
    }

    public static void incNumOfThreads() {
        numOfThreads.incrementAndGet();
        System.err.println(getNumOfThreads());
    }

    public static void decNumOfThreads() {
        numOfThreads.decrementAndGet();
    }

    public static int getNumOfThreads() {
        return numOfThreads.get();
    }

    // ===============================================================

    private static AtomicBoolean workFlag = new AtomicBoolean(false);

    /**
     * Called by the jvmti agent when multiple threads created
     */
    public static void startWork() {
        System.out.println("[ ! ] Start working");
        workFlag.set(true);
    }

    public static boolean isWorking() {
        return workFlag.get();
    }

    public static void stopWorking() {
        workFlag.set(false);
    }

    // ===============================================================

    private static AtomicReference<DelayCallInfo> currentDelayCall = new AtomicReference<DelayCallInfo>();
    private static AtomicReference<DelayCallInfo> lastDelayCall = new AtomicReference<DelayCallInfo>();
    private static ConcurrentLinkedDeque<DelayCallInfo> delayCallHistory = new ConcurrentLinkedDeque<>();

    public static DelayCallInfo getCurrentDelayedCall() {
        return currentDelayCall.get();
    }

    public static boolean setCurrentDelayedCall(DelayCallInfo call) {
        return currentDelayCall.compareAndSet(null, call);
    }

    /**
     * This method is synchronized by the delay alg.
     */
    public static boolean clearCurrentDelayedCall(DelayCallInfo call) {
        lastDelayCall.set(call);

        if (delayCallHistory.size() == MagicNumber.DELAY_HISTORY_WINDOW) {
            delayCallHistory.pop();
        }
        delayCallHistory.add(call);

        return currentDelayCall.compareAndSet(call, null);
    }

    public static DelayCallInfo getLastDelayedCall() {
        return lastDelayCall.get();
    }
}