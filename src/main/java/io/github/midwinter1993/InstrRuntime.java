package io.github.midwinter1993;

public class InstrRuntime {

    public static void methodEnter(Object target, int methodUid, String location) {
        //
        // Only there are more than one thread, we to stuffs
        // Note that each thread can only merge one token
        //
		if (!State.isWorking()) {
            return;
        }
        // System.err.println(State.getNumOfThreads());

        if ($.randProb10000() < MagicNumber.INSTR_PROB) {
			return;
        }
        CallInfo callInfo = State.getThreadCallInfo();
        CalleeInfo callee = CalleeInfoPool.getByUid(methodUid);

        callInfo.reinitialize(location, callee);

        Delay.onMethodEvent(callInfo);

        State.putThreadCallTsc(callInfo);
    }

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
}