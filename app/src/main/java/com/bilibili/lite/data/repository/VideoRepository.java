package com.bilibili.lite.data.repository;

import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.data.remote.WbiSigner;
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
        if (instance == null) instance = new VideoRepository();
        return instance;
    }

    public void fetchPopular(CallbackImpl callback) {
        api.getPopular().enqueue(new Callback<ApiService.BiliResponse<ApiService.PopularResult>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<ApiService.PopularResult>> call,
                                   Response<ApiService.BiliResponse<ApiService.PopularResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    callback.onSuccess(response.body().data.list);
                } else {
                    int code = response.body() != null ? response.body().code : -1;
                    callback.onError("API error: code=" + code);
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<ApiService.PopularResult>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getVideoDetail(String bvid, CallbackImpl<VideoInfo> callback) {
        api.getVideoInfo(new java.util.HashMap<String, String>() {{
            put("bvid", bvid);
        }}).enqueue(new Callback<ApiService.BiliResponse<VideoInfo>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<VideoInfo>> call,
                                   Response<ApiService.BiliResponse<VideoInfo>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    callback.onSuccess(response.body().data);
                } else {
                    callback.onError("Video detail error");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<VideoInfo>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getPlayUrl(String bvid, long cid, CallbackImpl<String> callback) {
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("qn", "32");
        params.put("fnval", "1");
        params.put("fnver", "0");
        params.put("fourk", "1");
        params.put("otype", "json");
        api.getPlayUrl(WbiSigner.sign(params)).enqueue(new Callback<ApiService.BiliResponse<ApiService.PlayUrlData>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<ApiService.PlayUrlData>> call,
                                   Response<ApiService.BiliResponse<ApiService.PlayUrlData>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.durl != null
                        && response.body().data.durl.length > 0) {
                    callback.onSuccess(response.body().data.durl[0].url);
                } else {
                    callback.onError("No playable URL");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<ApiService.PlayUrlData>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface CallbackImpl<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
