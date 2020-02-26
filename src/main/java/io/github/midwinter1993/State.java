package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class State {
    /**
     * To avoid frequent memory allocation and object init,
     * we reuse two thread local variables, which records the last method call
     * and is used for current call.
     */
    private static ThreadLocal<CallInfo> tlLastCallInfo = new ThreadLocal<CallInfo>() {
        @Override
        protected CallInfo initialValue() {
            return new CallInfo();
        }
    };

    private static ThreadLocal<CallInfo> tlBufferedCallInfo = new ThreadLocal<CallInfo>() {
        @Override
        protected CallInfo initialValue() {
            return new CallInfo();
        }
    };

    public static CallInfo createThreadCallInfo() {
        return tlBufferedCallInfo.get();
    }

    public static CallInfo getThreadLastCallInfo() {
        return tlLastCallInfo.get();
    }

    public static void swapThreadCallInfoBuffer() {
        CallInfo lastCallInfo = tlLastCallInfo.get();
        tlLastCallInfo.set(tlBufferedCallInfo.get());
        tlBufferedCallInfo.set(lastCallInfo);
    }

    // ===============================================================

    private static AtomicInteger numberOfThreads = new AtomicInteger(1);

    public static void incNumberOfThreads() {
        numberOfThreads.incrementAndGet();
    }

    public static void decNumberOfThreads() {
        numberOfThreads.decrementAndGet();
    }

    public static int getNumberOfThreads() {
        return numberOfThreads.get();
    }

    // ===============================================================

    private static AtomicReference<CallInfo> delayedCall = new AtomicReference<CallInfo>();

    public static CallInfo getCurrentDelayedCall() {
        return delayedCall.get();
    }

    public static boolean setCurrentDelayedCall(CallInfo call) {
        return delayedCall.compareAndSet(null, call);
    }

    public static boolean clearCurrentDelayedCall(CallInfo call) {
        return delayedCall.compareAndSet(call, null);
    }
}