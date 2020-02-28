package io.github.midwinter1993;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
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

	public static int randProb10000() {
		Random rand = new Random();
		return rand.nextInt(10000);
    }

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

}