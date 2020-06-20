package io.github.midwinter1993;

import java.lang.instrument.Instrumentation;

public class Agent {
    enum Mode {
        LOG,
        VERIFY,
        DELAY_LOG,
    }

    private static Mode mode = Mode.LOG;
    private static String verifyFile = null;
    private static String logDir = null;

    private static String argArrayGet(String[] argArray, int idx, String errMsg) {
        if (idx < argArray.length) {
            return argArray[idx];
        } else {
            $.warn("Error", errMsg);
            System.exit(1);
            return null;
        }
    }

    private static void parseArgs(String args) {
        String[] argArray = args.split("\\s*,\\s*");
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i].equals("-log")) {
                mode = Mode.LOG;

                i += 1;
                logDir = argArrayGet(argArray, i, "Log Directory Not Specified");
                return;
            } else if (argArray[i].equals("-verify")) {
                mode = Mode.VERIFY;

                i += 1;
                verifyFile = argArrayGet(argArray, i, "Verify File Not Specified");
                return;
            } else if (argArray[i].equals("-delayLog")) {
                mode = Mode.DELAY_LOG;

                i += 1;
                verifyFile = argArrayGet(argArray, i, "Verify File Not Specified");

                i += 1;
                logDir = argArrayGet(argArray, i, "Log Directory Not Specified");
                return;
            }
        }
    }

    public static boolean isLogging () {
        return mode == Mode.LOG;
    }

    public static boolean isVerifying() {
        return mode == Mode.VERIFY;
    }

    public static boolean isDelayLogging() {
        return mode == Mode.DELAY_LOG;
    }

    public static String getVerifyFile() {
        assert(isVerifying() || isDelayLogging());
        return verifyFile;
    }

    public static String getLogDir() {
        assert(isLogging() || isDelayLogging());
        return logDir;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        parseArgs(agentArgs);
        InstrRuntime.init();
        inst.addTransformer(new InstrTransformer());
    }
}

