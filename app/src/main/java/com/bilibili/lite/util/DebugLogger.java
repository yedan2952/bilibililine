package com.bilibili.lite.util;

import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebugLogger {

    private static final int MAX_LINES = 2000;
    private static final List<String> buffer = new ArrayList<>(MAX_LINES);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final Object lock = new Object();
    private static File logFile;

    public static void init(File dir) {
        logFile = new File(dir, "debug_log.txt");
        i("DebugLogger", "Logger initialized, log file: " + logFile.getAbsolutePath());
    }

    public static void v(String tag, String msg) { add('V', tag, msg); Log.v(tag, msg); }
    public static void d(String tag, String msg) { add('D', tag, msg); Log.d(tag, msg); }
    public static void i(String tag, String msg) { add('I', tag, msg); Log.i(tag, msg); }
    public static void w(String tag, String msg) { add('W', tag, msg); Log.w(tag, msg); }
    public static void e(String tag, String msg) { add('E', tag, msg); Log.e(tag, msg); }
    public static void e(String tag, String msg, Throwable tr) {
        StringWriter sw = new StringWriter();
        tr.printStackTrace(new PrintWriter(sw));
        add('E', tag, msg + "\n" + sw.toString());
        Log.e(tag, msg, tr);
    }

    public static String getLogs() {
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();
            for (String line : buffer) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    public static void clear() {
        synchronized (lock) { buffer.clear(); }
    }

    public static void flushToFile() {
        if (logFile == null) return;
        new Thread(() -> {
            synchronized (lock) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, false))) {
                    for (String line : buffer) { bw.write(line); bw.newLine(); }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private static void add(char level, String tag, String msg) {
        String line = sdf.format(new Date()) + " " + level + "/" + tag + ": " + msg;
        synchronized (lock) {
            if (buffer.size() >= MAX_LINES) buffer.remove(0);
            buffer.add(line);
        }
    }
}
