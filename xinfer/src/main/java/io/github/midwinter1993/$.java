package io.github.midwinter1993;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;

public class $ {
    public static void error(String format, Object ...args) {
        System.err.format("%s[ Error ]%s ", Color.RED, Color.RESET);
        System.err.format(format, args);
    }

    public static String pathJoin(String prefix, String suffix) {
        return Paths.get(prefix, suffix).toString();
    }

    public static void progress(String title) {
        System.out.format(">>> %s%s%s...\n", Color.GREEN, title, Color.RESET);
    }

    public static void mkdir(String dirPath) {
        File directory = new File(dirPath);
        if (! directory.exists()){
            directory.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
    }

    public static PrintWriter openWriter(String filePath) {
        try {
            PrintWriter out = new PrintWriter(filePath);
            return out;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public static double standardDeviation(ArrayList<Long> data) {
        if (data.isEmpty()) {
            return 0.0;
        }
        //
        // The mean average
        //
        long sum = 0;
        for (int i = 0; i < data.size(); i++) {
                sum += data.get(i);
        }
        double mean = (double)sum / data.size();

        //
        // The variance
        //
        double variance = 0.0;
        for (int i = 0; i < data.size(); i++) {
            long v = data.get(i);
            variance += (v - mean) * (v - mean);
        }
        variance /= data.size();

        //
        // Standard Deviation
        //
        double std = Math.sqrt(variance);
        return std;
    }

    public static void run(String prompt, Runnable func) {
        System.out.format("%s...\n", prompt);
        System.out.flush();

        long startTime = System.nanoTime();
        func.run();
		long endTime = System.nanoTime();

        long timeElapsed = endTime - startTime;

        int pos = prompt.indexOf("|_");
        if (pos != -1) {
            prompt = prompt.substring(0, pos) + "  |_ ";
        }
        System.out.format("%s%sDONE [%d ms]%s\n", prompt,
                                                       Color.CYAN,
                                                       timeElapsed / 1000000,
                                                       Color.RESET);
    }

    /**
     * match a number with optional '-' and decimal.
     */
    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
}