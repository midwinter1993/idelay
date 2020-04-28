package io.github.midwinter1993;

public abstract class Executor {
    public void threadStart() {
    }

    public void threadExit() {
    }

    public void vmDeath() {

    }

	public void methodEvent(CallInfo callInfo) {
    }

    public void beforeRead(Object target, String fieldName, String Location) {

    }

    public void beforeWrite(Object target, String fieldName, String Location) {

    }

    public void monitorEnter(Object target, String Location) {

    }

    public void monitorExit(Object target, String Location) {

    }
}