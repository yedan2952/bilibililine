package com.bilibili.lite;

import android.app.Application;
import com.bilibili.lite.util.ImageLoader;
import com.xuexiang.xui.XUI;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        XUI.init(this);
        ImageLoader.init(this);
    }
}
