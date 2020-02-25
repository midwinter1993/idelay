package io.github.midwinter1993;


public class InstrRuntime {
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

    public static void enterMethod(Object target,
                                   String methodName,
                                   String signature,
                                   String location) {

        CallInfo callInfo = tlBufferedCallInfo.get();
        callInfo.reinitialize(location);

        CallInfo lastCallInfo = tlLastCallInfo.get();

        Delay.onMethodEvent(lastCallInfo, callInfo);

        tlLastCallInfo.set(callInfo);
        tlBufferedCallInfo.set(lastCallInfo);
    }

    public static void exitMethod(Object target) {
        // System.out.println("Exit " + callLocation);
        if (target == null) {
            System.out.println("Exit ");
        } else {
            System.out.println("Exit ");
        }
    }
}