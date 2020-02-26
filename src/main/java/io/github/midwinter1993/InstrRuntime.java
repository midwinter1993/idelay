package io.github.midwinter1993;

import java.lang.management.ManagementFactory;

public class InstrRuntime {

    public static void methodEnter(Object target, String methodName, String location) {
        //
        // Only there are more than one thread, we to stuffs
        // Note that each thread can only merge one token
        //
		if (State.getNumberOfThreads() < 2) {
            State.mergeThreadToken();
            return;
        }

        if ($.randProb() < 10) {
			return;
        }
        CallInfo callInfo = State.createThreadCallInfo();
        callInfo.reinitialize(location);

        CallInfo lastCallInfo = State.getThreadLastCallInfo();

        Delay.onMethodEvent(lastCallInfo, callInfo);

        State.swapThreadCallInfoBuffer();
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