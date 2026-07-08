package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;

public class UserInfo {

    @SerializedName("mid")
    private long mid;

    @SerializedName("name")
    private String name;

    @SerializedName("face")
    private String face;

    @SerializedName("sign")
    private String sign;

    @SerializedName("level_info")
    private LevelInfo levelInfo;

    @SerializedName("vip")
    private Vip vip;

    public long getMid() { return mid; }
    public String getName() { return name; }
    public String getFace() { return face; }
    public String getSign() { return sign; }
    public int getLevel() { return levelInfo != null ? levelInfo.currentLevel : 0; }
    public boolean isVip() { return vip != null && vip.status == 1; }

    public static class LevelInfo {
        @SerializedName("current_level")
        public int currentLevel;
    }

    public static class Vip {
        @SerializedName("status")
        public int status;
    }
}
