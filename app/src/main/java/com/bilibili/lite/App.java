package com.bilibili.lite;

import android.app.Application;
import androidx.multidex.MultiDex;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.data.remote.WbiSigner;
import com.bilibili.lite.util.DebugLogger;
import com.bilibili.lite.util.ImageLoader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends Application {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoader.init(this);
        RetrofitClient.init(this);
        WbiSigner.init();
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
