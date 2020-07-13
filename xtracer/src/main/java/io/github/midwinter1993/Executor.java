package io.github.midwinter1993;

public abstract class Executor {
    public void threadStart() {
    }

    public void threadExit() {
    }

    public void vmDeath() {
    }

	public void methodEnter(Object target, String methodName, String location) {
    }

	public void methodExit(Object target, String methodName, String location) {
    }

    public void beforeRead(Object target, String fieldName, String location) {
    }

    public void beforeWrite(Object target, String fieldName, String location) {
    }

    public void monitorEnter(Object target, String location) {
    }

    public void monitorExit(Object target, String location) {

    }
}