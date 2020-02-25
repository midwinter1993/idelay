package io.github.midwinter1993;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


final class MagicNumber {
	public static long DELAY_TIME_MS = 100;
	public static int DELAY_PROB = 5;
}

final public class Delay {

	private static AtomicReference<CallInfo> lastDelayedCall = new AtomicReference<CallInfo>();
    private static AtomicInteger numberOfThreads = new AtomicInteger(1);

    private static ThreadLocal<CallInfo> threadLocalCall = new ThreadLocal<>();

	private static CallInfo getThreadLastCall() {
		return threadLocalCall.get();
	}

	private static void putThreadCall() {
		CallInfo lastCall = lastDelayedCall.get();

		if (lastCall == null || lastCall.getTid() == Thread.currentThread().getId()) {
			threadLocalCall.set(null);
		} else {
			threadLocalCall.set(new CallInfo(lastCall));
		}
	}

	private static boolean needDelay() {

		if ($.randProb() < MagicNumber.DELAY_PROB) {
			return true;
		} else {
			return false;
		}
	}

	private static void threadDelay() {
		// XLog.logf("Delay thread: %s  ", me.toString());
		lastDelayedCall.set(new CallInfo());
		try {
			Thread.sleep(MagicNumber.DELAY_TIME_MS); // 0.1 ms
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// XLog.logf("Ending delay %s %s\n", me.toString(), me.getInvokeInfo().getKey());
	}

	private static void mbrInfer() {
		CallInfo threadLocalLastCall = getThreadLastCall();

		if (threadLocalLastCall == null) {
			return;
		}

		Instant currentTsc = Instant.now();
		Duration duration = Duration.between(threadLocalLastCall.tsc, currentTsc);

		long milliSec = duration.getSeconds() * 1000 + duration.getNano() / 1000000;

		if (milliSec < MagicNumber.DELAY_TIME_MS) {
			return;
		}

		if (threadLocalLastCall.lastDelayedCall != null) {
			System.out.printf("===== May-HB (Delayed %dms) =====\n%s\n----------\n%s\n",
						milliSec,
						threadLocalLastCall.lastDelayedCall.toString(),
						threadLocalLastCall.toString());
		}
	}

	public static void onMethodEvent() {
		// if (me.getInvokeInfo() != null) {
			// Util.printf(">>> %s", me.getInvokeInfo().toString());
		// }
		if (numberOfThreads.get() < 2) {
			return;
		}

		if ($.randProb() < 70) {
			return;
		}
		if (needDelay()) {
			threadDelay();
		} else {
			mbrInfer();
		}
		putThreadCall();
	}
}