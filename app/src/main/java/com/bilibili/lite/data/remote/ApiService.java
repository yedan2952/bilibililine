package com.bilibili.lite.data.remote;

import com.bilibili.lite.data.model.VideoInfo;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ApiService {

    @GET("x/web-interface/view")
    Call<BiliResponse<VideoInfo>> getVideoInfo(@Query("bvid") String bvid);

    @GET("x/web-interface/popular")
    Call<BiliResponse<PopularResult>> getPopular();

    @GET("x/web-interface/wbi/search/type")
    Call<BiliResponse<SearchResultData>> search(@QueryMap Map<String, String> params);

    @GET("x/v2/reply/wbi/main")
    Call<BiliResponse<CommentResult>> getComments(@QueryMap Map<String, String> params);

    class BiliResponse<T> {
        public int code;
        public String message;
        public T data;
    }

    class PopularResult {
        public java.util.List<VideoInfo> list;
    }

    class SearchResultData {
        public java.util.List<VideoInfo> result;
    }

    class CommentResult {
        public CommentCursor cursor;
        public java.util.List<com.bilibili.lite.data.model.CommentItem> replies;
    }

    class CommentCursor {
        public int next;
        public boolean is_end;
    }
}
