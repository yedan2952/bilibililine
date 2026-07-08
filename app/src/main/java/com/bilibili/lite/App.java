package com.bilibili.lite;

import android.app.Application;
import com.bilibili.lite.util.ImageLoader;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoader.init(this);
    }
}
