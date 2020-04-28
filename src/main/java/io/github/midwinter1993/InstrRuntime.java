package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicInteger;

public class InstrRuntime {
    static Executor executor = new EventLogger();

    public static void methodEnter(Object target, int methodUid, String location) {
        //
        // Only there are more than one thread, we to stuffs
        // Note that each thread can only merge one token
        //
		if (!State.isWorking()) {
            return;
        }
        // System.err.println(State.getNumOfThreads());

        // if ($.randProb10000() < MagicNumber.INSTR_PROB) {
			// return;
        // }
        CallInfo callInfo = State.getThreadCallInfo();
        CalleeInfo callee = CalleeInfoPool.getByUid(methodUid);

        callInfo.reinitialize(target, location, callee);

        // Delay.onMethodEvent(callInfo);
        // Statistic.onMethodEvent(callInfo);
        executor.methodEvent(callInfo);

        State.putThreadCallTsc(callInfo);
    }

    /**
     * Called from JVMTI
     */
    private static final AtomicInteger numOfThreads = new AtomicInteger(0);

    public static void threadStart() {
        //
        // An approximated for multiple thread created by applications
        // Main thread, Signal Dispatcher, new thread
        //
        if (numOfThreads.incrementAndGet() >= 3) {
            State.startWork();
        }
        executor.threadStart();
    }

    public static void threadExit() {
        executor.threadExit();
    }

    public static void vmDeath() {
        executor.vmDeath();
    }

    // ===========================================

    public static void beforeRead(Object target, String fieldName, String location) {
        executor.beforeRead(target, fieldName, location);
    }

    public static void beforeWrite(Object target, String fieldName, String location) {
        executor.beforeWrite(target, fieldName, location);
    }

    public static void monitorEnter(Object target, String location) {
        executor.monitorEnter(target, location);
    }

    public static void monitorExit(Object target, String location) {
        executor.monitorExit(target, location);
    }

    /*
    public static void methodExit(Object target) {
        // System.out.println("Exit " + callLocation);
        if (target == null) {
            System.out.println("Exit ");
        } else {
            System.out.println("Exit ");
        }
    }

    public static void threadStart(Object target, String location) {
        System.err.format("Create thread \n %s\n", $.getStackTrace());
        // State.incNumberOfThreads();
    }

    public static void threadJoin(Object target, String location) {
        System.err.format("Join thread \n %s\n", $.getStackTrace());
        // State.decNumberOfThreads();
    }
    */
}