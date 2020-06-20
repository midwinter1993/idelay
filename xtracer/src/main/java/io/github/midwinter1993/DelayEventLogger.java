package io.github.midwinter1993;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class inserts delay before potential release method calls and log
 * events.
 */
public class DelayEventLogger extends EventLogger {
    /**
     * Reuse the logger of DelayInfer
     */
    private static final Logger logger = LogManager.getLogger("delayLog");

    public DelayEventLogger(String verifyFile, String logDir) {
        super(logDir);
        VerifyInfo.loadInfo(verifyFile);
    }

    @Override
	public void methodEnter(CallInfo callInfo) {
        if (VerifyInfo.needDelay(callInfo.getCallee().getName())) {
            // Add delay event
            addThreadLogEntry(LogEntry.delay());

            // Insert delay
            try {
                logger.info("Delay thread: {}\n{}", $.getTid(), callInfo.toString());
                Thread.sleep(MagicNumber.DELAY_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Logging events
        super.methodEnter(callInfo);
    }
}