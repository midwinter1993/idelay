package io.github.midwinter1993;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ConstantPool {
    private static HashMap<Integer, String> pool = new HashMap<>();
    private static InputStream inputStream = null;

    private static Integer readInt() {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; ++i) {
            try {
                bytes[i] = (byte) inputStream.read();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (bytes[i] == -1) {
                return null;
            }
        }
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static String readString() {
        Integer l = readInt();
        if (l == null) {
            return null;
        }

        byte[] bytes = new byte[l];
        for (int i = 0; i < l; ++i) {
            try {
                bytes[i] = (byte) inputStream.read();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (bytes[i] == -1) {
                return null;
            }
        }
        return new String(bytes);
    }

    public static void load(String filePath) {
        $.progress("Load Constant Pool");
        try {
            inputStream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            String s = readString();
            if (s == null) {
                break;
            }
            Integer strUid = readInt();
            pool.put(strUid, s);
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pool.put(-1, "Mark.Thread");
    }

    public static String get(int strUid) {
        return pool.get(strUid);
    }

    public static void print() {
        for (Map.Entry<Integer, String> e: pool.entrySet()) {
            System.out.format("[%d] %s\n", e.getKey(), e.getValue());
        }
    }
}