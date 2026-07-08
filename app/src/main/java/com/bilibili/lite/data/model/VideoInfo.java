package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;

public class VideoInfo {

    @SerializedName("bvid")
    private String bvid;

    @SerializedName("aid")
    private long aid;

    @SerializedName("title")
    private String title;

    @SerializedName("pic")
    private String pic;

    @SerializedName("duration")
    private long duration;

    @SerializedName("pubdate")
    private long pubdate;

    @SerializedName("desc")
    private String description;

    @SerializedName("owner")
    private Owner owner;

    @SerializedName("stat")
    private Stat stat;

    public String getBvid() { return bvid; }
    public long getAid() { return aid; }
    public String getTitle() { return title; }
    public String getPic() { return pic; }
    public long getDuration() { return duration; }
    public long getPubdate() { return pubdate; }
    public String getDescription() { return description; }
    public String getOwnerName() { return owner != null ? owner.name : ""; }
    public long getOwnerMid() { return owner != null ? owner.mid : 0; }
    public String getOwnerFace() { return owner != null ? owner.face : ""; }
    public long getPlayCount() { return stat != null ? stat.view : 0; }
    public long getDanmakuCount() { return stat != null ? stat.danmaku : 0; }
    public long getLikeCount() { return stat != null ? stat.like : 0; }
    public long getCoinCount() { return stat != null ? stat.coin : 0; }
    public long getFavoriteCount() { return stat != null ? stat.favorite : 0; }

    public static class Owner {
        @SerializedName("mid")
        public long mid;
        @SerializedName("name")
        public String name;
        @SerializedName("face")
        public String face;
    }

    public static class Stat {
        @SerializedName("view")
        public long view;
        @SerializedName("danmaku")
        public long danmaku;
        @SerializedName("like")
        public long like;
        @SerializedName("coin")
        public long coin;
        @SerializedName("favorite")
        public long favorite;
    }
}
