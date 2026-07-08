package com.bilibili.lite.data.repository;

import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.data.remote.WbiSigner;
import com.bilibili.lite.util.DebugLogger;
import java.util.HashMap;
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

    public void fetchPopular(CallbackImpl<List<VideoInfo>> callback) {
        api.getPopular().enqueue(new Callback<ApiService.BiliResponse<ApiService.PopularResult>>() {
            @Override public void onResponse(Call<ApiService.BiliResponse<ApiService.PopularResult>> call,
                                             Response<ApiService.BiliResponse<ApiService.PopularResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null)
                    callback.onSuccess(response.body().data.list);
                else callback.onError("API error");
            }
            @Override public void onFailure(Call<ApiService.BiliResponse<ApiService.PopularResult>> call, Throwable t) {
                DebugLogger.e("VideoRepo", "fetchPopular failed", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getVideoDetail(String bvid, CallbackImpl<VideoInfo> callback) {
        HashMap<String, String> p = new HashMap<>();
        p.put("bvid", bvid);
        api.getVideoInfo(p).enqueue(new Callback<ApiService.BiliResponse<VideoInfo>>() {
            @Override public void onResponse(Call<ApiService.BiliResponse<VideoInfo>> call,
                                             Response<ApiService.BiliResponse<VideoInfo>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null)
                    callback.onSuccess(response.body().data);
                else callback.onError("Video detail error");
            }
            @Override public void onFailure(Call<ApiService.BiliResponse<VideoInfo>> call, Throwable t) {
                DebugLogger.e("VideoRepo", "getVideoDetail failed", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getPlayUrl(String bvid, long cid, int qn, CallbackImpl<PlayUrlResult> callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("qn", String.valueOf(qn));
        params.put("fnval", "4048"); // DURL(1) + DASH(16) + HDR(64) + 4K
        params.put("fnver", "0");
        params.put("fourk", "1");
        params.put("otype", "json");
        api.getPlayUrl(WbiSigner.sign(params)).enqueue(new Callback<ApiService.BiliResponse<ApiService.PlayUrlData>>() {
            @Override public void onResponse(Call<ApiService.BiliResponse<ApiService.PlayUrlData>> call,
                                             Response<ApiService.BiliResponse<ApiService.PlayUrlData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    PlayUrlResult r = new PlayUrlResult();
                    ApiService.PlayUrlData data = response.body().data;
                    if (data.durl != null && data.durl.length > 0) {
                        r.url = data.durl[0].url;
                        // Collect all available backup URLs for fallback
                        java.util.ArrayList<String> backups = new java.util.ArrayList<>();
                        if (data.durl[0].backup_url != null) {
                            for (String bu : data.durl[0].backup_url) {
                                if (bu != null && !bu.isEmpty()) backups.add(bu);
                            }
                        }
                        // Add other durl entries as additional backups
                        for (int i = 1; i < data.durl.length; i++) {
                            if (data.durl[i].url != null && !data.durl[i].url.isEmpty()) {
                                backups.add(data.durl[i].url);
                            }
                        }
                        r.backupUrls = backups.toArray(new String[0]);
                    }
                    // Try DASH format as additional fallback
                    if (data.dash != null && data.dash.video != null) {
                        java.util.ArrayList<String> backups = new java.util.ArrayList<>();
                        if (r.backupUrls != null) {
                            java.util.Collections.addAll(backups, r.backupUrls);
                        }
                        for (ApiService.DashStream ds : data.dash.video) {
                            String base = ds.baseUrl;
                            if (base == null) base = ds.base_url;
                            if (base != null) backups.add(base);
                        }
                        r.backupUrls = backups.toArray(new String[0]);
                        // If no durl at all, use first dash URL as primary
                        if (r.url == null && data.dash.video.length > 0) {
                            String base = data.dash.video[0].baseUrl;
                            if (base == null) base = data.dash.video[0].base_url;
                            if (base != null) r.url = base;
                        }
                    }
                    r.acceptDesc = data.accept_description;
                    r.acceptQuality = data.accept_quality;
                    if (r.url != null) callback.onSuccess(r);
                    else callback.onError("No playable URL in response");
                } else callback.onError("API error: " + (response.body() != null ? response.body().message : "unknown"));
            }
            @Override public void onFailure(Call<ApiService.BiliResponse<ApiService.PlayUrlData>> call, Throwable t) {
                DebugLogger.e("VideoRepo", "getPlayUrl failed bvid=" + bvid + " cid=" + cid + " qn=" + qn, t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getComments(long aid, int page, CallbackImpl<List<com.bilibili.lite.data.model.CommentItem>> callback) {
        HashMap<String, String> p = new HashMap<>();
        p.put("oid", String.valueOf(aid));
        p.put("type", "1");
        p.put("pn", String.valueOf(page));
        p.put("ps", "20");
        p.put("sort", "2");
        api.getComments(WbiSigner.sign(p)).enqueue(new Callback<ApiService.BiliResponse<ApiService.CommentResult>>() {
            @Override public void onResponse(Call<ApiService.BiliResponse<ApiService.CommentResult>> call,
                                             Response<ApiService.BiliResponse<ApiService.CommentResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null)
                    callback.onSuccess(response.body().data.replies);
                else callback.onError("No comments");
            }
            @Override public void onFailure(Call<ApiService.BiliResponse<ApiService.CommentResult>> call, Throwable t) {
                DebugLogger.e("VideoRepo", "getComments failed aid=" + aid, t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getRelated(String bvid, CallbackImpl<List<VideoInfo>> callback) {
        HashMap<String, String> p = new HashMap<>();
        p.put("bvid", bvid);
        api.getRelated(p).enqueue(new Callback<List<VideoInfo>>() {
            @Override public void onResponse(Call<List<VideoInfo>> call, Response<List<VideoInfo>> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else callback.onError("No related");
            }
            @Override public void onFailure(Call<List<VideoInfo>> call, Throwable t) {
                DebugLogger.e("VideoRepo", "getRelated failed bvid=" + bvid, t);
                callback.onError(t.getMessage());
            }
        });
    }

    public static class PlayUrlResult {
        public String url;
        public String[] backupUrls;
        public String[] acceptDesc;
        public int[] acceptQuality;
    }

    public interface CallbackImpl<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
