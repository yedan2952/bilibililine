package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;

public class LiveRoomInfo {

    @SerializedName("roomid")
    private long roomid;

    @SerializedName("uid")
    private long uid;

    @SerializedName("title")
    private String title;

    @SerializedName("cover")
    private String cover;

    @SerializedName("online")
    private long online;

    @SerializedName("live_status")
    private int liveStatus;

    @SerializedName("uname")
    private String uname;

    public long getRoomid() { return roomid; }
    public long getUid() { return uid; }
    public String getTitle() { return title; }
    public String getCover() { return cover; }
    public long getOnline() { return online; }
    public boolean isLiving() { return liveStatus == 1; }
    public String getUname() { return uname; }
}
