package io.github.midwinter1993;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class $ {

    public static long getTsc() {
        return System.nanoTime();
    }

    public static long milliDelta(long beforeTsc, long afterTsc) {
		return (afterTsc - beforeTsc) / 1000000;
    }

    public static long nanoDelta(long beforeTsc, long afterTsc) {
		return afterTsc - beforeTsc ;
    }

    // ===============================================================

    /**
     * We put a static obj here to avoid frequent obj allocation
     */
    static Random rand = new Random();

	public static int randProb10000() {
		return rand.nextInt(10000);
    }

    // ===============================================================

    public static String getStackTrace() {
		StringBuilder builder = new StringBuilder();
		StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();

		int level = 0;
		for (int i = 1; i < stackTraces.length; ++i) {
			String currentTrace = stackTraces[i].toString();
			if (currentTrace.startsWith("io.github.midwinter1993") ||
				currentTrace.contains("__$rr")) {
				continue;
			}
			level += 1;

			builder.append(String.join("", Collections.nCopies(level, " ")));
			builder.append("-> ");
			builder.append(currentTrace);
			builder.append("\n");
		}
		//
		// Remove the last `\n`
		//
		builder.setLength(builder.length() - 1);

		return builder.toString();
    }

    public static long getTid() {
        return Thread.currentThread().getId();
    }

    public static Class<?>[] parseSignature(String signature) {
        List<Class<?>> klassList = new ArrayList<>();


        String parameters = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));

        if (!parameters.isEmpty()) {
            System.out.println(parameters);
        }

        Class<?>[] buf = new Class<?>[klassList.size()];
        buf = klassList.toArray(buf);
        return buf;
    }

    public static Method lookupMethod(Object obj, String methodName, String signature) {
        Class<?> klass = obj.getClass();
        Method method = null;

        try {
            method = klass.getDeclaredMethod(methodName, parseSignature(signature));
        } catch (NoSuchMethodException e) {
            // e.printStackTrace();
        }
        if (method != null) {
            return method;
        }

        try {
            method = klass.getMethod(methodName, parseSignature(signature));
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();

            for (Method m: klass.getMethods()) {
                System.out.println("  > " + m.toString());
            }
            System.out.println("" + methodName + " " + signature);
            System.exit(-1);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    // ===============================================================
    // private static final Logger logger = LogManager.getLogger("instrLog");

    public static void dumpClassLoader(ClassLoader loader) {
        if (loader == null) {
            System.err.println("bootstrap");
            return;
        }
        System.err.format("%s\n", loader.toString());
        dumpClassLoader(loader.getParent());
    }

    // ===============================================================
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void warn(String title, String format, Object ...args) {
        System.err.format("%s[ %s ]%s ", ANSI_RED, title, ANSI_RESET);
        System.err.format(format, args);
    }

    public static void info(String format, Object ...args) {
        System.err.format(format, args);
    }
}