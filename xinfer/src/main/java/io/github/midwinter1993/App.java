package io.github.midwinter1993;

import java.util.Arrays;

public class App {
    static {
        System.loadLibrary("jniortools");
    }

    private static String logDir = null;
    private static String[] windowFiles = null;

    private static void parseArgument(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            String option = args[i];
            if (option.equals("-d") && i < args.length-1) {
                logDir = args[i+1];
                return;
            } if (option.equals("-cross") && i < args.length-1) {
                windowFiles = Arrays.copyOfRange(args, i+1, args.length);
                return;
            } else {
                System.err.format("Unknown argument `%s`\n", option);
                System.err.format("--- Usage ---\n");
                System.err.format("xinfer -d <path-log-dir> \n");
                System.err.format("xinfer -cross <path-window-file> <path-window-file> ... \n");
                return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        parseArgument(args);
        if (logDir == null && windowFiles == null) {
            System.err.format("No log directory or window files\n");
            System.exit(0);
        }

        Infer infer = new Infer();

        if (logDir != null) {
            ConstantPool.load($.pathJoin(logDir, "map.cp"));
            LogPool pool = new LogPool(logDir);
            infer.encode(pool);
        } else if (windowFiles != null) {
            infer.encode(windowFiles);
        }

        infer.solve();
        infer.saveResult();
    }
}
