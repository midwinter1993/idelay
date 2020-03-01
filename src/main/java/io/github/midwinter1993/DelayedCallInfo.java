package io.github.midwinter1993;

class DelayCallInfo extends CallInfo {
    private ThreadLocal<Boolean> token = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

    public DelayCallInfo(CallInfo callInfo) {
		tsc = callInfo.tsc;
		tid = callInfo.tid;
        location = callInfo.location = location;
        stackTrace = $.getStackTrace();

        token.set(true);
    }

    public boolean canInfer() {
        return token.get();
    }

    public void setInferred() {
        token.set(false);
    }
}