package com.bilibili.lite.util;

import android.app.Activity;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.bilibili.lite.BuildConfig;

public class DarkThemeHelper {

    public static void apply(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("bili", Activity.MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark_mode", false);
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static boolean isDark(Activity activity) {
        return activity.getSharedPreferences("bili", Activity.MODE_PRIVATE)
                .getBoolean("dark_mode", false);
    }

    public static void toggle(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("bili", Activity.MODE_PRIVATE);
        boolean dark = !prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", dark).apply();
        activity.recreate();
    }
}
