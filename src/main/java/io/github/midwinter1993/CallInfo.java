package io.github.midwinter1993;

import java.time.Instant;

class CallInfo {
	private Instant tsc;
    private long tid;
	private CallInfo lastDelayedCall;
    private String location;
    private String stackTrace;

    public CallInfo() {
		tsc = Instant.now();
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
		tsc = Instant.now();
		tid = Thread.currentThread().getId();
        lastDelayedCall = null;
        location = loc;
        stackTrace = null;
    }

    /**
     * Get stacktrace is heavy; we only compute it when it is cloned
     */
    public CallInfo clone() {
        CallInfo callInfo = new CallInfo();

		callInfo.tsc = tsc;
		callInfo.tid = tid;
        callInfo.lastDelayedCall = lastDelayedCall;
        callInfo.location = location;
        callInfo.stackTrace = $.getStackTrace();

        return callInfo;
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

    public Instant getTsc() {
        return tsc;
    }

	public CallInfo getLastDelayedCall() {
        return lastDelayedCall;
    }

    public String getLocation() {
        return location;
    }
}