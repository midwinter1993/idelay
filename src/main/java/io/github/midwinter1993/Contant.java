package io.github.midwinter1993;

class Constant {
    final public static String PACKAGE_NAME = "io.github.midwinter1993";

    final public static boolean logInstrument = true;

    final public static boolean isInstrumentAccess = false;

    final public static String METHOD_ENTER_SIGNATURE = "io.github.midwinter1993.InstrRuntime.methodEnter($0, %d, \"%s\");";

    final public static String THREAD_START_SIGNATURE = "io.github.midwinter1993.InstrRuntime.threadStart($0, \"%s\");";

    final public static String THREAD_JOIN_SIGNATURE = "io.github.midwinter1993.InstrRuntime.threadJoin($0, \"%s\");";

    final public static String BEFORE_READ_SIGNATURE = "io.github.midwinter1993.InstrRuntime.beforeRead($0);";

    final public static String BEFORE_WRITE_SIGNATURE = "io.github.midwinter1993.InstrRuntime.beforeWrite($0);";
}