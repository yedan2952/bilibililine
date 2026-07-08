package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;

public class CommentItem {

    @SerializedName("rpid")
    private long rpid;

    @SerializedName("oid")
    private long oid;

    @SerializedName("ctime")
    private long ctime;

    @SerializedName("like")
    private long like;

    @SerializedName("rcount")
    private long rcount;

    @SerializedName("member")
    private Member member;

    @SerializedName("content")
    private Content content;

    public long getRpid() { return rpid; }
    public long getOid() { return oid; }
    public long getCtime() { return ctime; }
    public long getLike() { return like; }
    public long getRcount() { return rcount; }
    public String getUserName() { return member != null ? member.uname : ""; }
    public String getUserAvatar() { return member != null ? member.avatar : ""; }
    public String getMessage() { return content != null ? content.message : ""; }

    public static class Member {
        @SerializedName("mid")
        public String mid;
        @SerializedName("uname")
        public String uname;
        @SerializedName("avatar")
        public String avatar;
    }

    public static class Content {
        @SerializedName("message")
        public String message;
    }
}
