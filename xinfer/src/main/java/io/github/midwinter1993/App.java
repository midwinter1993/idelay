package io.github.midwinter1993;

public class App {
    static {
        System.loadLibrary("jniortools");
    }

    private static String parseArgument(String[] args) {
        String logDir = null;
        for (int i = 0; i < args.length; ++i) {
            String option = args[i];
            if (option.equals("-d") && i < args.length-1) {
                logDir = args[i+1];
                i += 1;
            } else {
                System.err.format("Unknown argument `%s`\n", option);
            }
        }

        return logDir;
    }

    public static void main(String[] args) throws Exception {
        String logDir = parseArgument(args);
        if (logDir == null) {
            System.err.format("No log directory\n");
            System.exit(0);
        }

        ConstantPool.load($.pathJoin(logDir, "map.cp"));

        LogPool pool = new LogPool(logDir);
        Infer infer = new Infer();
        infer.lpEncode(pool);
        infer.lpSolve();
        infer.saveResult();
    }
}
