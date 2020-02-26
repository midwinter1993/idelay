package io.github.midwinter1993;

import java.time.Instant;

class CallInfo {
	private long tsc;
    private long tid;
	private CallInfo lastDelayedCall;
    private String location;
    private String stackTrace;

    public CallInfo() {
		tsc = $.getTsc();
		tid = Thread.currentThread().getId();
        lastDelayedCall = null;
        location = null;
        stackTrace = null;
    }

	// public CallInfo(String loc) {
	// 	tsc = Instant.now();
	// 	tid = Thread.currentThread().getId();
    //     lastDelayedCall = null;
    //     location = loc;
    //     stackTrace = null;
	// 	// stackTrace = $.getStackTrace();
    // }

    public void reinitialize(String loc) {
		tsc = $.getTsc();
		tid = Thread.currentThread().getId();
        lastDelayedCall = null;
        location = loc;
        stackTrace = null;
    }

    /**
     * Get stacktrace is heavy; we only compute it when it is cloned
     */
    public CallInfo cloneWithStackTrace() {
        CallInfo callInfo = new CallInfo();

		callInfo.tsc = tsc;
		callInfo.tid = tid;
        callInfo.lastDelayedCall = lastDelayedCall;
        callInfo.location = location;
        callInfo.stackTrace = $.getStackTrace();

        return callInfo;
    }

    public void fillInStackTrace() {
        stackTrace = $.getStackTrace();
    }

	public String toString() {
        return String.format("[ thread info ] %d\n[ stack info ]\n Next call: %s\n%s",
                             tid,
                             location,
                             stackTrace);
    }

    public void setLastDelayedCall(CallInfo last) {
        lastDelayedCall = last;
    }

	public long getTid() {
		return tid;
    }

    public long getTsc() {
        return tsc;
    }

	public CallInfo getLastDelayedCall() {
        return lastDelayedCall;
    }

    public String getLocation() {
        return location;
    }
}