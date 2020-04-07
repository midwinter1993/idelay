package io.github.midwinter1993;

public abstract class Executor {
	public void onMethodEvent(CallInfo callInfo) {
    }

    public void onThreadExit() {
    }

    public void beforeRead(Object target) {

    }

    public void beforeWrite(Object target) {

    }

    public void monitorEnter(Object target) {

    }

    public void monitorExit(Object target) {

    }
}