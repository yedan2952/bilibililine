package com.bilibili.lite.data.remote;

import com.bilibili.lite.data.model.CommentItem;
import com.bilibili.lite.data.model.UserInfo;
import com.bilibili.lite.data.model.VideoInfo;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface ApiService {

    @GET("x/web-interface/view")
    Call<BiliResponse<VideoInfo>> getVideoInfo(@QueryMap Map<String, String> params);

    @GET("x/web-interface/popular")
    Call<BiliResponse<PopularResult>> getPopular();

    @GET("x/web-interface/wbi/search/type")
    Call<BiliResponse<SearchResultData>> search(@QueryMap Map<String, String> params);

    @GET("x/v2/reply/wbi/main")
    Call<BiliResponse<CommentResult>> getComments(@QueryMap Map<String, String> params);

    @GET("x/player/playurl")
    Call<BiliResponse<PlayUrlData>> getPlayUrl(@QueryMap Map<String, String> params);

    @GET("x/web-interface/nav")
    Call<BiliResponse<UserInfo>> getNavInfo();

    @GET("x/web-interface/related")
    Call<List<VideoInfo>> getRelated(@QueryMap Map<String, String> params);

    class BiliResponse<T> {
        public int code;
        public String message;
        public T data;
    }

    class PopularResult {
        public List<VideoInfo> list;
    }

    class SearchResultData {
        public List<VideoInfo> result;
    }

    class CommentResult {
        public CommentCursor cursor;
        public List<CommentItem> replies;
    }

    class CommentCursor {
        public int next;
        public boolean is_end;
    }

    class PlayUrlData {
        public long totalSize;
        public String format;
        public String[] accept_description;
        public int[] accept_quality;
        public DUrlInfo[] durl;
        public DashData dash;
    }

    class DUrlInfo {
        public int order;
        public long length;
        public long size;
        public String url;
        public String[] backup_url;
    }

    class DashData {
        public DashStream[] video;
        public DashStream[] audio;
    }

    class DashStream {
        public int id;
        public String baseUrl;
        public String base_url;
        public String mimeType;
        public String mime_type;
        public String codecs;
        public int bandwidth;
        public int width;
        public int height;
        public String[] backupUrl;
        public String[] backup_url;
    }
}
