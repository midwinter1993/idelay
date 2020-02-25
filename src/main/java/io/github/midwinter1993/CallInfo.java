package io.github.midwinter1993;

import java.time.Instant;

class CallInfo {
	public Instant tsc;
	public long tid;
	public CallInfo lastDelayedCall;
	public String stackTrace;

	public CallInfo() {
		tsc = Instant.now();
		tid = -1;
		lastDelayedCall = null;
		// stackTrace = null;
		stackTrace = $.getStackTrace();
	}

    public CallInfo(CallInfo last) {
		tsc = Instant.now();
		tid = Thread.currentThread().getId();
		lastDelayedCall = last;
		stackTrace = $.getStackTrace();
	}

	public String toString() {
        return String.format("thread info [%d]\nstack info [%s]",
                             tid,
                             stackTrace);
	}

	public long getTid() {
		return tid;
	}
}