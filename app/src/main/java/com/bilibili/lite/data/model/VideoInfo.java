package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;

public class VideoInfo {

    @SerializedName("bvid")
    private String bvid;

    @SerializedName("aid")
    private long aid;

    @SerializedName("cid")
    private long cid;

    @SerializedName("title")
    private String title;

    @SerializedName("pic")
    private String pic;

    /**
     * Duration in seconds.
     * Bilibili API may return it as a number (seconds) or a string ("mm:ss").
     * We store as String and parse to long via getDuration().
     */
    @SerializedName("duration")
    private String duration;

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
    public long getCid() { return cid; }
    public String getTitle() { return title; }
    public String getPic() { return pic; }

    /** Returns duration in seconds, parsing "mm:ss" format if needed. */
    public long getDuration() {
        if (duration == null) return 0;
        try {
            return Long.parseLong(duration);
        } catch (NumberFormatException e) {
            // Try parsing "mm:ss" format (e.g. "5:30" → 330 seconds)
            String[] parts = duration.split(":");
            if (parts.length == 2) {
                try {
                    return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

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
