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

	private static boolean needDelay() {
		if ($.randProb() < MagicNumber.DELAY_PROB) {
			return true;
		} else {
			return false;
		}
	}

	private static void threadDelay(CallInfo callInfo) {
        // System.out.format("Delay thread: %d \n%s\n", callInfo.getTid(), $.getStackTrace());

        /**
         * When putting the call info into global list,
         * we clone it and set the stack trace for it.
         * Since there may be multiple threads entering this method concurrently,
         * we use CAS in setCurrentDelayedCall.
         */
        CallInfo clonedCallInfo = callInfo.cloneWithStackTrace();

        if (State.setCurrentDelayedCall(clonedCallInfo)) {
            logger.info("Delay thread: {}", callInfo.getTid());

            try {
                Thread.sleep(MagicNumber.DELAY_TIME_MS); // 0.1 ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!State.clearCurrentDelayedCall(clonedCallInfo)) {
                logger.error("BUG");
            }

            logger.info("Ending delay thread: {}", $.getTid());
        }
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
            System.out.format("Duration %dms\n", milliSec);
            System.out.format("Duration %dns\n", $.nanoDelta(lastCallInfo.getTsc(), callInfo.getTsc()));
            System.out.format("---\n%s\n---\n%s\n", lastCallInfo.toString(), callInfo.toString());
			return;
		}

		if (lastCallInfo.getLastDelayedCall() != null) {
			logger.info("===== May-HB (Delayed {}ms) =====\n{}\n----------\n{}\n",
                        milliSec,
                        lastCallInfo.getLastDelayedCall().toString(),
                        lastCallInfo.toString());
            // logger.info("[ current stack trace ] \n {}", $.getStackTrace());
		}
	}

	public static void onMethodEvent(CallInfo lastCallInfo, CallInfo callInfo) {
        // CallInfo delayedCall = State.getCurrentDelayedCall();

        //
        // We maintain that there is at most one thread being delay
        // When there is a delayed thread, we do inference and
        // compute the stack trace for c2
        // T1: | Delay...| c1
        // T2:     X: |         | c2 Y:c3
        //
        // if (delayedCall != null) {
        //     if (delayedCall.getTid() == Thread.currentThread().getId()) {
        //         logger.error("BUG!!! Delayed thread {}; Current thread {}",
        //                      delayedCall.getTid(),
        //                      $.getTid());
        //     }
        //     callInfo.setLastDelayedCall(delayedCall);
        //     callInfo.fillInStackTrace();
        //     System.out.format("Try to check %s\n", callInfo.toString());
        // }

        mhbInfer(lastCallInfo, callInfo);

        // if (delayedCall == null && needDelay()) {
            // threadDelay(callInfo);
        // }
	}
}