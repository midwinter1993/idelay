package io.github.midwinter1993;

import java.util.Collections;
import java.util.Random;

class $ {

	public static int randProb() {
		Random rand = new Random();
		return rand.nextInt(100);
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
}