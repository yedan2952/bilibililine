package com.bilibili.lite.ui.search;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.data.remote.WbiSigner;
import com.bilibili.lite.util.NetworkUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends AndroidViewModel {

    private static final int MAX_HISTORY = 20;

    private final ApiService api = RetrofitClient.getInstance().getApiService();
    private final MutableLiveData<List<VideoInfo>> results = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> searchHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int page = 1;
    private String currentQuery;

    public SearchViewModel(Application app) { super(app); loadHistory(); }

    public LiveData<List<VideoInfo>> getResults() { return results; }
    public LiveData<List<String>> getSearchHistory() { return searchHistory; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    private SharedPreferences getPrefs() {
        return getApplication().getSharedPreferences("search_history", 0);
    }

    private void loadHistory() {
        java.util.Set<String> saved = getPrefs().getStringSet("history", new java.util.HashSet<>());
        List<String> list = new ArrayList<>(saved);
        // Sort by most recent first (reverse order of insertion)
        java.util.Collections.reverse(list);
        searchHistory.setValue(list);
    }

    public void saveSearch(String query) {
        query = query.trim();
        if (query.isEmpty()) return;
        SharedPreferences prefs = getPrefs();
        java.util.Set<String> saved = new java.util.HashSet<>(prefs.getStringSet("history", new java.util.HashSet<>()));
        saved.remove(query); // Remove duplicate
        saved.add(query);
        // Keep only last MAX_HISTORY
        if (saved.size() > MAX_HISTORY) {
            List<String> list = new ArrayList<>(saved);
            saved = new java.util.HashSet<>(list.subList(list.size() - MAX_HISTORY, list.size()));
        }
        prefs.edit().putStringSet("history", saved).apply();
        loadHistory();
    }

    public void clearHistory() {
        getPrefs().edit().clear().apply();
        searchHistory.setValue(new ArrayList<>());
    }

    public void search(String query) {
        if (!NetworkUtil.isNetworkAvailable(getApplication())) {
            error.setValue("无网络连接，请检查网络设置");
            return;
        }
        currentQuery = query;
        page = 1;
        results.setValue(new ArrayList<>());
        saveSearch(query);
        executeSearch();
    }

    public void loadMore() {
        page++;
        executeSearch();
    }

    private void executeSearch() {
        if (currentQuery == null || currentQuery.isEmpty()) return;
        loading.setValue(true);
        HashMap<String, String> raw = new HashMap<>();
        raw.put("keyword", currentQuery);
        raw.put("search_type", "video");
        raw.put("page", String.valueOf(page));
        Map<String, String> params = WbiSigner.sign(raw);
        api.search(params).enqueue(new Callback<ApiService.BiliResponse<ApiService.SearchResultData>>() {
            @Override
            public void onResponse(Call<ApiService.BiliResponse<ApiService.SearchResultData>> call,
                                   Response<ApiService.BiliResponse<ApiService.SearchResultData>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<VideoInfo> current = results.getValue();
                    if (current == null) current = new ArrayList<>();
                    if (response.body().data.result != null) current.addAll(response.body().data.result);
                    results.setValue(current);
                }
            }

            @Override
            public void onFailure(Call<ApiService.BiliResponse<ApiService.SearchResultData>> call, Throwable t) {
                loading.setValue(false);
                error.setValue(NetworkUtil.getNetworkErrorMessage(t, getApplication()));
            }
        });
    }
}
