package com.bilibili.lite.data.repository;

import com.bilibili.lite.data.model.UserInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserRepository {

    private static UserRepository instance;
    private final ApiService api;
    private final OkHttpClient okHttp;

    private UserRepository() {
        api = RetrofitClient.getInstance().getApiService();
        okHttp = RetrofitClient.getInstance().getOkHttpClient();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    public void getNavInfo(CallbackImpl<UserInfo> callback) {
        api.getNavInfo().enqueue(new Callback<ApiService.BiliResponse<UserInfo>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<UserInfo>> call,
                                   retrofit2.Response<ApiService.BiliResponse<UserInfo>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    callback.onSuccess(response.body().data);
                } else {
                    callback.onError("Not logged in");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<UserInfo>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void generateQrCode(final QrCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                Response resp = okHttp.newCall(request).execute();
                String body = resp.body() != null ? resp.body().string() : "";
                JSONObject json = new JSONObject(body);
                JSONObject data = json.getJSONObject("data");
                String url = data.getString("url");
                String key = data.getString("qrcode_key");
                callback.onGenerated(url, key);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void pollQrLogin(String qrcodeKey, final QrPollCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                Response resp = okHttp.newCall(request).execute();
                String body = resp.body() != null ? resp.body().string() : "";
                JSONObject json = new JSONObject(body);
                int code = json.getJSONObject("data").getInt("code");
                callback.onResult(code);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public interface CallbackImpl<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface QrCallback {
        void onGenerated(String url, String key);
        void onError(String error);
    }

    public interface QrPollCallback {
        void onResult(int code);
        void onError(String error);
    }
}
