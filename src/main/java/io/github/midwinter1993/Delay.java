package io.github.midwinter1993;

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
            logger.info("Delay thread: {}\n{}", $.getTid(), clonedCallInfo.toString());

            try {
                Thread.sleep(MagicNumber.DELAY_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!State.clearCurrentDelayedCall(clonedCallInfo)) {
                logger.error("BUG");
            }

            logger.info("Ending delay thread: {}", $.getTid());
        }
	}

	private static void mhbInfer(CallInfo callInfo) {
        long milliSec = $.milliDelta(State.getThreadLastCallTsc(), callInfo.getTsc());

        //
        // If the duration of two continuous method call is larger
        // than the threshold but not cause by a wait/sleep, i.e., too long (approx.)
        //
        if (milliSec < MagicNumber.DELAY_TIME_MS / 2 ||
            milliSec > MagicNumber.DELAY_TIME_MS * 2) {
            // System.out.format("Duration %dns\n", $.nanoDelta(lastCallInfo.getTsc(), callInfo.getTsc()));
            // System.out.format("---\n%s\n---\n%s\n", lastCallInfo.toString(), callInfo.toString());
			return;
		}

        CallInfo lastDelayedCall = State.getLastDelayedCall();
		if (lastDelayedCall != null && lastDelayedCall.getTid() != $.getTid()) {
            //
            // Current call happens after the last delayed call
            // and is also close to the last delayed call within a time interval
            //
            milliSec = $.milliDelta(lastDelayedCall.getTsc(), callInfo.getTsc());
            if (milliSec <= 0 || milliSec > MagicNumber.DELAY_TIME_MS) {
                return;
            }

            callInfo.fillInStackTrace();

			logger.info("===== May-HB (Delayed {}ms) =====\n{}\n----------\n{}\n",
                        milliSec,
                        lastDelayedCall.toString(),
                        callInfo.toString());
            // logger.info("[ current stack trace ] \n {}", $.getStackTrace());
		}
	}

	public static void onMethodEvent(CallInfo callInfo) {

        mhbInfer(callInfo);

        CallInfo delayedCall = State.getCurrentDelayedCall();

        //
        // We maintain that there is at most one thread being delay
        // When there is a delayed thread, we do inference and
        // compute the stack trace for c2
        // T1: | Delay...| c1
        // T2:     X: |         | Y:c2
        //  c1 ->HB c2
        //
        if (delayedCall != null) {
            if (delayedCall.getTid() == Thread.currentThread().getId()) {
                logger.error("BUG!!! Delayed thread {}; Current thread {}",
                             delayedCall.getTid(),
                             $.getTid());
            }
        } else {
            if (needDelay()) {
                threadDelay(callInfo);
            }
        }
	}
}