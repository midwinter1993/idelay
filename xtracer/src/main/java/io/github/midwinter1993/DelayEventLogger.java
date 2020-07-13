package io.github.midwinter1993;

/**
 * This class inserts delay before potential release method calls and log
 * events.
 */
public class DelayEventLogger extends EventLogger {
    /**
     * Reuse the logger of DelayInfer
     */
    private static final LiteLogger logger = LiteLogger.getLogger("delayLog");

    public DelayEventLogger(String verifyFile, String logDir) {
        super(logDir);
        VerifyInfo.loadInfo(verifyFile);
    }

    @Override
    public void methodEnter(Object target, String methodName, String location) {
        //
        // FIXME!
        //
        CallInfo callInfo = new CallInfo();

        if (VerifyInfo.needDelay(callInfo.getCallee().getName())) {
            // Add delay event
            addThreadLogEntry(LogEntry.delay());

            // Insert delay
            try {
                logger.info("Delay thread: %d\n%s", $.getTid(), callInfo.toString());
                Thread.sleep(MagicNumber.DELAY_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Logging events
        super.methodEnter(target, methodName, location);
    }
}