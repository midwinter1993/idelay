package io.github.midwinter1993;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


final class MagicNumber {
	public static long DELAY_TIME_MS = 100;
	public static int DELAY_PROB = 5;
}

final public class Delay {
    private static final Logger logger = LogManager.getLogger("delayLog");

	private static AtomicReference<CallInfo> globalLastDelayedCall = new AtomicReference<CallInfo>();

	private static void setLastDelayedCall(CallInfo callInfo) {
        CallInfo lastDelayedCall = globalLastDelayedCall.get();

        if (lastDelayedCall == null ||
            lastDelayedCall.getTid() == Thread.currentThread().getId()) {
            callInfo.setLastDelayedCall(null);
		} else {
            callInfo.setLastDelayedCall(lastDelayedCall);
        }
	}

	private static boolean needDelay() {
		if ($.randProb() < MagicNumber.DELAY_PROB) {
			return true;
		} else {
			return false;
		}
	}

	private static void threadDelay(CallInfo callInfo) {
        // XLog.logf("Delay thread: %s  ", me.toString());

        /**
         * When putting the call info into global list,
         * we clone it and set the stack trace for it.
         * Thus, only delayed method calls has stack traces.
         */
        globalLastDelayedCall.set(callInfo.clone());

		try {
			Thread.sleep(MagicNumber.DELAY_TIME_MS); // 0.1 ms
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// XLog.logf("Ending delay %s %s\n", me.toString(), me.getInvokeInfo().getKey());
	}

	private static void mhbInfer(CallInfo lastCallInfo, CallInfo callInfo) {
		if (lastCallInfo == null) {
			return;
		}

        long milliSec = $.milliDelta(lastCallInfo.getTsc(), callInfo.getTsc());

        //
        // If the duration of two continuous method call is larger
        // than the threshold but not cause by a wait/sleep (approx.)
        //
        if (milliSec < MagicNumber.DELAY_TIME_MS ||
            milliSec > MagicNumber.DELAY_TIME_MS * 2) {
			return;
		}

		if (lastCallInfo.getLastDelayedCall() != null) {
			logger.info("===== May-HB (Delayed {}ms) =====\n{}\n----------\n{}\n",
                        milliSec,
                        lastCallInfo.getLastDelayedCall().toString(),
                        lastCallInfo.toString());
		}
	}

	public static void onMethodEvent(CallInfo lastCallInfo, CallInfo callInfo) {
        // System.out.println(callInfo.toString());
        // System.out.println("-----");
		if (State.getNumberOfThreads() < 2 || $.randProb() < 70) {
			return;
		}

		if (needDelay()) {
			threadDelay(callInfo);
		} else {
			mhbInfer(lastCallInfo, callInfo);
		}
		setLastDelayedCall(callInfo);
	}
}