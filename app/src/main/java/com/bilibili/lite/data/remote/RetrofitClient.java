package com.bilibili.lite.data.remote;

import android.content.Context;

import com.bilibili.lite.util.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://api.bilibili.com/";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private static RetrofitClient instance;
    private final ApiService apiService;
    private final OkHttpClient okHttpClient;
    private static PersistentCookieJar persistentCookieJar;

    private static Context appContext;

    /**
     * Initialize with application context (call from Application.onCreate)
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        persistentCookieJar = new PersistentCookieJar();

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .dns(new RetryDns())
                .cookieJar(persistentCookieJar)
                .addInterceptor(logging)
                .addInterceptor(new UserAgentInterceptor())
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    // ──────── Custom DNS with retry ────────

    private static class RetryDns implements Dns {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            int attempts = 0;
            UnknownHostException lastException = null;
            while (attempts < MAX_RETRIES) {
                try {
                    return Arrays.asList(InetAddress.getAllByName(hostname));
                } catch (UnknownHostException e) {
                    lastException = e;
                    attempts++;
                    if (attempts < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempts);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            throw lastException;
        }
    }

    // ──────── User-Agent + Referer interceptor ────────

    private static class UserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
                    .header("Referer", "https://www.bilibili.com");
            // Add Origin header for API compatibility
            builder.header("Origin", "https://www.bilibili.com");
            return chain.proceed(builder.build());
        }
    }

    // ──────── Retry interceptor for transient failures ────────

    // ──────── Persistent cookie jar (in-memory + SharedPreferences) ────────

    private static class PersistentCookieJar implements CookieJar {
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<Cookie>> cookieStore
                = new java.util.concurrent.ConcurrentHashMap<>();

        PersistentCookieJar() {
            // Restore cookies from disk on construction
            if (appContext != null) {
                String saved = appContext.getSharedPreferences("cookies", 0)
                        .getString("cookies", "");
                if (!saved.isEmpty()) {
                    for (String entry : saved.split("\\|")) {
                        String[] parts = entry.split("=", 2);
                        if (parts.length == 2) {
                            Cookie cookie = new Cookie.Builder()
                                    .domain(parts[0].split("@")[0])
                                    .name(parts[0].split("@")[1])
                                    .value(parts[1])
                                    .build();
                            String host = "." + parts[0].split("@")[0];
                            java.util.List<Cookie> list = cookieStore.get(host);
                            if (list == null) {
                                list = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                                cookieStore.put(host, list);
                            }
                            list.add(cookie);
                        }
                    }
                }
            }
        }

        @Override
        public void saveFromResponse(HttpUrl url, java.util.List<Cookie> cookies) {
            String host = url.host();
            java.util.List<Cookie> existing = cookieStore.get(host);
            if (existing == null) {
                existing = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                cookieStore.put(host, existing);
            }
            for (Cookie c : cookies) {
                existing.removeIf(old -> old.name().equals(c.name()));
                existing.add(c);
            }
            persistToDisk();
        }

        @Override
        public java.util.List<Cookie> loadForRequest(HttpUrl url) {
            java.util.List<Cookie> cookies = cookieStore.get(url.host());
            if (cookies == null) {
                // Also check parent domains
                String host = url.host();
                while (host.contains(".")) {
                    host = host.substring(host.indexOf('.') + 1);
                    cookies = cookieStore.get("." + host);
                    if (cookies != null) return cookies;
                }
            }
            return cookies != null ? cookies : new java.util.ArrayList<>();
        }

        private void persistToDisk() {
            if (appContext == null) return;
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, java.util.List<Cookie>> entry : cookieStore.entrySet()) {
                for (Cookie c : entry.getValue()) {
                    if (sb.length() > 0) sb.append("|");
                    sb.append(entry.getKey()).append("@").append(c.name()).append("=").append(c.value());
                }
            }
            appContext.getSharedPreferences("cookies", 0)
                    .edit().putString("cookies", sb.toString()).apply();
        }
    }

    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            int retryCount = 0;

            while (true) {
                try {
                    Response response = chain.proceed(request);
                    if (response.isSuccessful() || retryCount >= maxRetries) {
                        return response;
                    }
                    response.close();
                } catch (SocketTimeoutException | UnknownHostException e) {
                    if (retryCount >= maxRetries) {
                        throw e;
                    }
                }

                retryCount++;
                try {
                    Thread.sleep(RETRY_DELAY_MS * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
            }
        }
    }
}
