package io.github.midwinter1993;

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
        executor.onMethodEvent(callInfo);

        State.putThreadCallTsc(callInfo);
    }

    /**
     * Called from JVMTI
     */
    public static void threadExit() {
        executor.onThreadExit();
    }

    public static void beforeRead(Object target) {
        executor.beforeRead(target);
    }

    public static void beforeWrite(Object target) {
        executor.beforeWrite(target);
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