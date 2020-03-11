package io.github.midwinter1993;


class CallInfo {
	protected long tsc;
    protected long tid;
    protected String location;
    protected CalleeInfo callee;
    protected String stackTrace;

    public CallInfo() {
		tsc = $.getTsc();
		tid = Thread.currentThread().getId();
        location = null;
        stackTrace = null;
    }

    public void reinitialize(String loc, CalleeInfo method) {
		tsc = $.getTsc();
		tid = Thread.currentThread().getId();
        location = loc;
        callee = method;
        stackTrace = null;
    }

    /**
     * Get stacktrace is heavy; we only compute it when it is cloned
     */
    public void fillInStackTrace() {
        stackTrace = $.getStackTrace();
    }

	public String toString() {
        return String.format("[ thread info ] %d\n[ stack info ]\n Next call: %s\n%s",
                             tid,
                             location,
                             stackTrace);
    }

	public long getTid() {
		return tid;
    }

    public long getTsc() {
        return tsc;
    }

    public String getLocation() {
        return location;
    }

    public CalleeInfo getCallee() {
        return callee;
    }
}