package io.github.midwinter1993;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Filter {

    private static Set<String> WHITELIST = new HashSet<>(Arrays.asList("other/Stuff"));
    private static Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
        "java/", "javax/", "sun/",
        "io/github/midwinter1993/",
        "org/apache/logging/",
        "org/apache/commons",
        "org/dacapo/harness/",
        "org/dacapo/parser/",
        "Harness"
    ));

    public static boolean filterClass(String className) {
        //
        // Normalize class name
        //
        String name = className.replace('.', '/');

        if (WHITELIST.contains(name)) {
            return false;
        }
        for (String prefix: BLACKLIST) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean filterMethod(String methodName) {
        //
        // Method name starts with class name
        //
        return filterClass(methodName);
    }
}