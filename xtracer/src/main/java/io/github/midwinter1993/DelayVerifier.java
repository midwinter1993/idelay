package io.github.midwinter1993;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final public class DelayVerifier extends Executor {
    private static final Logger logger = LogManager.getLogger("verifyLog");

    public DelayVerifier(String verifyFile) {
        VerifyInfo.loadInfo(verifyFile);
    }

	private static void threadDelay(CallInfo callInfo) {
        /**
         * When putting the call info into global list,
         * we clone it and set the stack trace for it.
         * Since there may be multiple threads entering this method concurrently,
         * we use CAS in setCurrentDelayedCall.
         */
        DelayCallInfo clonedCallInfo = new DelayCallInfo(callInfo);

        if (State.setCurrentDelayedCall(clonedCallInfo)) {
            // logger.info("Delay method: {}\n{}", callInfo.getCallee().getName(),
                                                // clonedCallInfo.toString());
            logger.info("Delay method: {}", callInfo.getCallee().getName());

            try {
                Thread.sleep(MagicNumber.DELAY_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!State.clearCurrentDelayedCall(clonedCallInfo)) {
                logger.error("BUG");
            }
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
			return;
		}

        DelayCallInfo lastDelayCall = State.getLastDelayedCall();
        if (lastDelayCall != null &&
            lastDelayCall.getTid() != $.getTid() &&
            lastDelayCall.canInfer()) {

            lastDelayCall.setInferred();
            //
            // Current call happens after the last delayed call
            // and is also close to the last delayed call within a time interval
            //
            milliSec = $.milliDelta(lastDelayCall.getTsc(), callInfo.getTsc());
            if (milliSec <= 0 || milliSec > MagicNumber.DELAY_TIME_MS*2) {
                // System.err.println(milliSec);
                return;
            }

            callInfo.fillInStackTrace();

            logger.info("===== May-HB (Delayed {}ms) =====", milliSec);
            logger.info("Delay method: {}\n{}\n-------\nCurrent method: {}\n{}\n",
                        lastDelayCall.getCallee().getName(),
                        lastDelayCall.toString(),
                        callInfo.getCallee().getName(),
                        callInfo.toString());
            // logger.info("[ current stack trace ] \n {}", $.getStackTrace());
		}
	}

    @Override
	public void methodEnter(CallInfo callInfo) {

        mhbInfer(callInfo);

        DelayCallInfo delayedCall = State.getCurrentDelayedCall();

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
            String methodName = callInfo.getCallee().getName();
            if (VerifyInfo.needDelay(methodName)) {
                threadDelay(callInfo);
            }
        }
	}
}