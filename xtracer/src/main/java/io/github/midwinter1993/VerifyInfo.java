package io.github.midwinter1993;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class VerifyInfo {
    private static Set<String> acqMethodNames = new HashSet<>();
    private static Set<String> relMethodNames = new HashSet<>();

	public static boolean needDelay(String methodName) {
		if (acqContain(methodName)) {
            return true;
		} else {
			return false;
		}
	}

    public static void loadInfo(String verifyFile) {
        Set<String> currentSet = null;
        try {
			Scanner scanner = new Scanner(new File(verifyFile));
			while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("[RELEASE]")) {
                    currentSet = relMethodNames;
                } else if (line.equals("[ACQUIRE]")) {
                    currentSet = acqMethodNames;
                } else {
                    if (currentSet == null) {
                        System.err.println("Verify File Format Error");
                        System.exit(1);
                    }
                    currentSet.add(line);
                }
			}
			scanner.close();
		} catch (FileNotFoundException e) {
            System.err.format("Verify File `%s` Not Existing\n", verifyFile);
            System.exit(1);
		}
    }

    public static String relTarget() {
        return relMethodNames.iterator().next();
    }

    public static boolean acqContain(String methodName) {
        return acqMethodNames.contains(methodName);
    }
}