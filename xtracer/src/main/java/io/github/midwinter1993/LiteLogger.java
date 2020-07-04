package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class LiteLogger {

    PrintWriter writer = null;

    private static HashMap<String, LiteLogger> map = new HashMap<>();

    public static LiteLogger getLogger(String name) {
        LiteLogger logger = map.get(name);
        if (logger == null) {
            logger = new LiteLogger(name);
        }
        return logger;
    }

    private LiteLogger(String name) {
        try {
            writer = new PrintWriter(String.format("./%s", name), "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    synchronized public void info(String format, Object ...args) {
        writer.format(format, args);
        writer.print('\n');
        writer.flush();
    }
}