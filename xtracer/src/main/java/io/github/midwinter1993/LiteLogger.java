package io.github.midwinter1993;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class LiteLogger {

    PrintWriter writer = null;

    public LiteLogger(String name) {
        try {
            writer = new PrintWriter(String.format("./%s", name), "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void info(String format, Object ...args) {
        writer.format(format, args);
        writer.print('\n');
        writer.flush();
    }
}