package com.bilibili.lite.data.repository;

import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoRepository {

    private static VideoRepository instance;
    private final ApiService api;

    private VideoRepository() {
        api = RetrofitClient.getInstance().getApiService();
    }

    public static synchronized VideoRepository getInstance() {
        if (instance == null) {
            instance = new VideoRepository();
        }
        return instance;
    }

    public void fetchPopular(Callback<List<VideoInfo>> callback) {
        api.getPopular().enqueue(new retrofit2.Callback<ApiService.BiliResponse<ApiService.PopularResult>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<ApiService.PopularResult>> call,
                                   Response<ApiService.BiliResponse<ApiService.PopularResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    callback.onSuccess(response.body().data.list);
                } else {
                    callback.onError("empty response");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<ApiService.PopularResult>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
