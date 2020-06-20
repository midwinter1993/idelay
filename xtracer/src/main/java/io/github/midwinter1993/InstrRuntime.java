package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicInteger;

public class InstrRuntime {
    static Executor executor = null;

    public static void init() {
        if (Agent.isLogging()) {
            executor = new EventLogger(Agent.getLogDir());
        } else if (Agent.isVerifying()) {
            executor = new DelayVerifier(Agent.getVerifyFile());
        } else if (Agent.isDelayLogging()) {
            executor = new DelayEventLogger(Agent.getVerifyFile(),
                                            Agent.getLogDir());
        }
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
        if (numOfThreads.incrementAndGet() >= 3 && executor != null) {
            State.startWork();
        }
        if (executor != null) {
            executor.threadStart();
        }
    }

    public static void threadExit() {
        executor.threadExit();
    }

    public static void vmDeath() {
        executor.vmDeath();
    }

    // ===========================================

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

        executor.methodEnter(callInfo);

        State.putThreadCallTsc(callInfo);
    }

    public static void methodExit(Object target, int methodUid, String location) {
		if (!State.isWorking()) {
            return;
        }

        CallInfo callInfo = State.getThreadCallInfo();
        CalleeInfo callee = CalleeInfoPool.getByUid(methodUid);

        callInfo.reinitialize(target, location, callee);

        executor.methodExit(callInfo);
    }

    public static void beforeRead(Object target, String fieldName, String location) {
		if (!State.isWorking()) {
            return;
        }

        executor.beforeRead(target, fieldName, location);
    }

    public static void beforeWrite(Object target, String fieldName, String location) {
		if (!State.isWorking()) {
            return;
        }

        executor.beforeWrite(target, fieldName, location);
    }

    public static void monitorEnter(Object target, String location) {
		if (!State.isWorking()) {
            return;
        }

        executor.monitorEnter(target, location);
    }

    public static void monitorExit(Object target, String location) {
		if (!State.isWorking()) {
            return;
        }

        executor.monitorExit(target, location);
    }
}