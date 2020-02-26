package io.github.midwinter1993;

import java.time.Instant;

public class InstrRuntime {
        public static void methodEnter(Object target, String methodName, String location) {

		if (State.getNumberOfThreads() < 2 || $.randProb() < 10) {
			return;
        }
        System.out.format("%s\n", Instant.now().getNano());
        System.out.format("> %d\n", System.nanoTime());

        /*
        CallInfo callInfo = State.createThreadCallInfo();
        callInfo.reinitialize(location);

        CallInfo lastCallInfo = State.getThreadLastCallInfo();

        Delay.onMethodEvent(lastCallInfo, callInfo);

        State.swapThreadCallInfoBuffer();
        */
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
        State.incNumberOfThreads();
    }

    public static void threadJoin(Object target, String location) {
        System.err.format("Join thread \n %s\n", $.getStackTrace());
        State.decNumberOfThreads();
    }
}