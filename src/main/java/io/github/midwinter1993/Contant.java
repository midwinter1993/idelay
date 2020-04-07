package io.github.midwinter1993;

class Constant {
    final public static String PACKAGE_NAME = "io.github.midwinter1993";

    final public static String METHOD_ENTER_SIGNATURE = "io.github.midwinter1993.InstrRuntime.methodEnter($0, %d, \"%s\");";

    final public static String BEFORE_READ_SIGNATURE = "io.github.midwinter1993.InstrRuntime.beforeRead($0);";

    final public static String BEFORE_WRITE_SIGNATURE = "io.github.midwinter1993.InstrRuntime.beforeWrite($0);";

    final public static String MONITOR_ENTER_SIGNATURE = "io.github.midwinter1993.InstrRuntime.monitorEnter($0);";

    final public static String MONITOR_EXIT_SIGNATURE = "io.github.midwinter1993.InstrRuntime.monitorExit($0);";

    final public static boolean IS_LOG_INSTRUMENT = true;

    final public static boolean IS_SKIP_SMALL_METHOD = false;

    final public static boolean IS_INSTRUMENT_ACCESS = true;

    final public static boolean IS_INSTRUMENT_MONITOR = true;

    final public static String LITE_LOG_DIR = "./lite-logs";
}