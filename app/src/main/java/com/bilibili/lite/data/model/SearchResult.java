package com.bilibili.lite.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResult {

    @SerializedName("result")
    private List<VideoInfo> result;

    @SerializedName("page")
    private int page;

    @SerializedName("pagesize")
    private int pagesize;

    @SerializedName("numResults")
    private int numResults;

    public List<VideoInfo> getResult() { return result; }
    public int getPage() { return page; }
    public int getPagesize() { return pagesize; }
    public int getNumResults() { return numResults; }
}
