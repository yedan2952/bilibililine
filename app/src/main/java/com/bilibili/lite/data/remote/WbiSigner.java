package com.bilibili.lite.data.remote;

import com.bilibili.lite.util.DebugLogger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * WBI签名工具。
 *
 * 注意：refreshKey() 内部使用同步网络请求 (execute())，
 * 必须确保在后台线程调用。sign() 方法会自动在后台刷新密钥，
 * 调用方无需关心线程问题。
 */
public class WbiSigner {

    private static String mixinKey = "";
    private static long lastFetch = 0;
    private static volatile boolean isRefreshing = false;
    private static final Object KEY_LOCK = new Object();

    private static final long KEY_EXPIRY_MS = 86400000L; // 24 hours

    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    /**
     * Pre-warm the WBI key on app startup (call from background thread).
     */
    public static void init() {
        if (mixinKey.isEmpty()) {
            new Thread(() -> refreshKey()).start();
        }
    }

    /**
     * Refresh the WBI signing key.
     * WARNING: Uses synchronous execute() — must be called from a background thread.
     */
    public static void refreshKey() {
        synchronized (KEY_LOCK) {
            if (isRefreshing) {
                DebugLogger.d("WbiSigner", "Key refresh already in progress, skipping");
                return;
            }
            isRefreshing = true;
        }

        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .dns(hostname -> {
                        int attempts = 0;
                        while (attempts < 3) {
                            try {
                                return java.util.Arrays.asList(java.net.InetAddress.getAllByName(hostname));
                            } catch (java.net.UnknownHostException e) {
                                attempts++;
                                if (attempts >= 3) throw e;
                                try { Thread.sleep(1000 * attempts); } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    throw e;
                                }
                            }
                        }
                        throw new java.net.UnknownHostException("Unable to resolve " + hostname);
                    })
                    .build();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://api.bilibili.com/x/web-interface/nav")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            String body = response.body() != null ? response.body().string() : "";
            org.json.JSONObject json = new org.json.JSONObject(body);
            if (json.has("data") && !json.isNull("data")) {
                org.json.JSONObject data = json.getJSONObject("data");
                if (data.has("wbi_img") && !data.isNull("wbi_img")) {
                    org.json.JSONObject wbi = data.getJSONObject("wbi_img");
                    String imgUrl = wbi.getString("img_url");
                    String subUrl = wbi.getString("sub_url");
                    String imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1, imgUrl.lastIndexOf('.'));
                    String subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1, subUrl.lastIndexOf('.'));
                    String raw = imgKey + subKey;
                    StringBuilder sb = new StringBuilder(32);
                    for (int i = 0; i < 32; i++) {
                        sb.append(raw.charAt(MIXIN_KEY_ENC_TAB[i]));
                    }
                    mixinKey = sb.toString();
                    lastFetch = System.currentTimeMillis();
                    DebugLogger.i("WbiSigner", "Key refreshed, mixinKey=" + mixinKey);
                } else {
                    DebugLogger.w("WbiSigner", "No wbi_img in nav response");
                }
            } else {
                DebugLogger.w("WbiSigner", "Invalid nav response");
            }
        } catch (Exception e) {
            DebugLogger.e("WbiSigner", "Failed to refresh key", e);
        } finally {
            isRefreshing = false;
        }
    }

    /**
     * Sign parameters with WBI signature.
     * If the key is not yet loaded, triggers an async background refresh
     * and returns the params unsigned (best-effort). The next call will
     * have the key ready.
     */
    public static Map<String, String> sign(Map<String, String> params) {
        // ── Key is empty: trigger async refresh, return unsigned ──
        if (mixinKey.isEmpty()) {
            if (!isRefreshing) {
                new Thread(() -> refreshKey()).start();
            }
            // Return params unsigned — the API may still work without WBI
            // on some endpoints, or the key might arrive before the request
            // completes (race with enqueue).
            DebugLogger.w("WbiSigner", "Key not ready, returning unsigned params");
            return params;
        }

        // ── Key is expired: refresh in background, keep using current key ──
        if (System.currentTimeMillis() - lastFetch > KEY_EXPIRY_MS && !isRefreshing) {
            new Thread(() -> refreshKey()).start();
        }

        // ── Sign with current key ──
        long wts = System.currentTimeMillis() / 1000;
        params.put("wts", String.valueOf(wts));

        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (query.length() > 0) query.append("&");
            String value = e.getValue().replaceAll("[!'()*]", "");
            query.append(encode(e.getKey())).append("=").append(encode(value));
        }

        String wRid = md5(query.toString() + mixinKey);
        params.put("w_rid", wRid);
        return params;
    }

    private static String encode(String s) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes;
        try { bytes = s.getBytes("UTF-8"); } catch (java.io.UnsupportedEncodingException e) { return s; }
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append((char) c);
            } else if (c == ' ') {
                sb.append("%20");
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
