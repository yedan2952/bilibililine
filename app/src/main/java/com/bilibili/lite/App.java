package com.bilibili.lite;

import android.app.Application;
import com.bilibili.lite.util.DebugLogger;
import com.bilibili.lite.util.ImageLoader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoader.init(this);
        DebugLogger.init(getCacheDir());
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            DebugLogger.e("CRASH", "Uncaught exception on " + thread, ex);
            DebugLogger.flushToFile();
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}
