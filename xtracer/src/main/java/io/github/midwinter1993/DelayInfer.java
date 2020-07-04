package io.github.midwinter1993;

import java.util.HashMap;

final public class DelayInfer extends Executor {
    private static final LiteLogger logger = new LiteLogger("delay.log");

    private static ThreadLocal<HashMap<String, Integer>> tlDelayInfo = new ThreadLocal<HashMap<String, Integer>>() {
        @Override
        protected HashMap<String, Integer> initialValue() {
            return new HashMap<String, Integer>();
        }
    };

	private static boolean needDelay(CallInfo callInfo) {
		if ($.randProb10000() < MagicNumber.DELAY_PROB) {

            String loc = callInfo.getLocation();
            int num_delay = tlDelayInfo.get().getOrDefault(loc, new Integer(0));
            //
            // If one location is delayed many times, we will not delay here any more.
            //
            if (num_delay > MagicNumber.DELAY_LOC_NUM) {
                return false;
            }

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
        DelayCallInfo clonedCallInfo = new DelayCallInfo(callInfo);

        String loc = callInfo.getLocation();
        int num_delay = tlDelayInfo.get().getOrDefault(loc, new Integer(0));
        tlDelayInfo.get().put(loc, num_delay + 1);

        if (State.setCurrentDelayedCall(clonedCallInfo)) {
            logger.info("Delay thread: %d\n%s", $.getTid(), clonedCallInfo.toString());

            try {
                Thread.sleep(MagicNumber.DELAY_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!State.clearCurrentDelayedCall(clonedCallInfo)) {
                logger.info("BUG");
            }

            // logger.info("Ending delay thread: {}", $.getTid());
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
            if (milliSec <= 0 || milliSec > MagicNumber.DELAY_TIME_MS) {
                return;
            }

            callInfo.fillInStackTrace();

			logger.info("===== May-HB (Delayed %dms) =====\n%s\n----------\n%s\n",
                        milliSec,
                        lastDelayCall.toString(),
                        callInfo.toString());
            // logger.info("[ current stack trace ] \n %s", $.getStackTrace());
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
                logger.info("BUG!!! Delayed thread %d; Current thread %d",
                             delayedCall.getTid(),
                             $.getTid());
            }
        } else {
            if (needDelay(callInfo)) {
                threadDelay(callInfo);
            }
        }
	}
}