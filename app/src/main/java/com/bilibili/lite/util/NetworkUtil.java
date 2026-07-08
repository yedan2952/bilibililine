package com.bilibili.lite.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * Utility class for network status checks and diagnostics.
 */
public class NetworkUtil {

    /**
     * Check if the device has network connectivity.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Check if WiFi is connected.
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi != null && wifi.isConnected();
    }

    /**
     * Get a human-readable network type description for debugging.
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";
        NetworkInfo active = cm.getActiveNetworkInfo();
        if (active == null) return "No network";
        int type = active.getType();
        int subtype = active.getSubtype();
        switch (type) {
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_MOBILE:
                switch (subtype) {
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return "4G/LTE";
                    case TelephonyManager.NETWORK_TYPE_NR:
                        return "5G";
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        return "3G/HSPA+";
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        return "2G/EDGE";
                    default:
                        return "Mobile (" + subtype + ")";
                }
            case ConnectivityManager.TYPE_ETHERNET:
                return "Ethernet";
            default:
                return "Network type " + type;
        }
    }

    /**
     * Get a user-friendly error message for network-related exceptions.
     */
    public static String getNetworkErrorMessage(Throwable t, Context context) {
        if (t == null) return "未知错误";

        String msg = t.getMessage();
        if (msg == null) msg = t.getClass().getSimpleName();

        if (!isNetworkAvailable(context)) {
            return "无网络连接，请检查网络设置";
        }

        if (msg.contains("Unable to resolve host") || msg.contains("UnknownHost")) {
            return "DNS解析失败，请检查网络连接或切换网络后重试";
        }
        if (msg.contains("EAI_AGAIN")) {
            return "DNS临时故障，正在自动重试...";
        }
        if (msg.contains("timeout") || msg.contains("Timeout")) {
            return "连接超时，请检查网络后重试";
        }
        if (msg.contains("Connection refused")) {
            return "服务器拒绝连接，请稍后重试";
        }
        if (msg.contains("Network is unreachable")) {
            return "网络不可达，请检查网络连接";
        }
        if (msg.contains("SSL") || msg.contains("ssl")) {
            return "安全连接失败，请检查系统时间是否正确";
        }

        // Return original message for other errors (truncated for UI)
        if (msg.length() > 80) {
            msg = msg.substring(0, 80) + "...";
        }
        return "网络错误: " + msg;
    }
}
