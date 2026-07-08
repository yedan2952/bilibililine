package com.bilibili.lite.ui.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.remote.ApiService;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.data.remote.WbiSigner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    private final ApiService api = RetrofitClient.getInstance().getApiService();
    private final MutableLiveData<List<VideoInfo>> results = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int page = 1;
    private String currentQuery;

    public LiveData<List<VideoInfo>> getResults() { return results; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void search(String query) {
        currentQuery = query;
        page = 1;
        results.setValue(new ArrayList<>());
        executeSearch();
    }

    public void loadMore() {
        page++;
        executeSearch();
    }

    private void executeSearch() {
        if (currentQuery == null || currentQuery.isEmpty()) return;
        loading.setValue(true);
        Map<String, String> params = WbiSigner.sign(new java.util.HashMap<String, String>() {{
            put("keyword", currentQuery);
            put("search_type", "video");
            put("page", String.valueOf(page));
        }));
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
                error.setValue(t.getMessage());
            }
        });
    }
}
