package io.github.midwinter1993;

import java.lang.instrument.Instrumentation;

public class Agent {
    enum Mode {
        LOG,
        VERIFY,
    }

    private static Mode mode = Mode.LOG;
    private static String verifyFile = null;

    private static void parseArgs(String args) {
        String[] argArray = args.split("\\s*,\\s*");
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i].equals("-log")) {
                mode = Mode.LOG;
                return;
            }
            if (argArray[i].equals("-verify")) {
                mode = Mode.VERIFY;
                i += 1;

                if (i < argArray.length) {
                    verifyFile = argArray[i];
                    return;
                } else {
                    $.warn("Error", "Verify File Not Specified");
                    System.exit(1);
                }
            }
        }
    }

    public static boolean isLogging () {
        return mode == Mode.LOG;
    }

    public static boolean isVerifying() {
        return mode == Mode.VERIFY;
    }

    public static String getVerifyFile() {
        return verifyFile;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        parseArgs(agentArgs);
        InstrRuntime.init();
        inst.addTransformer(new InstrTransformer());
    }
}

