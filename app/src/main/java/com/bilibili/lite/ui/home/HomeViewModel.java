package com.bilibili.lite.ui.home;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import com.bilibili.lite.util.NetworkUtil;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final VideoRepository repository = VideoRepository.getInstance();
    private final MutableLiveData<List<VideoInfo>> videos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeViewModel(Application app) { super(app); }

    public LiveData<List<VideoInfo>> getVideos() { return videos; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadPopular() {
        if (!NetworkUtil.isNetworkAvailable(getApplication())) {
            error.setValue("无网络连接，请检查网络设置后下拉刷新");
            return;
        }
        loading.setValue(true);
        error.setValue(null);
        repository.fetchPopular(new VideoRepository.CallbackImpl<List<VideoInfo>>() {
            @Override
            public void onSuccess(List<VideoInfo> result) {
                videos.setValue(result);
                loading.setValue(false);
            }

            @Override
            public void onError(String err) {
                error.setValue(NetworkUtil.getNetworkErrorMessage(
                        new Exception(err), getApplication()));
                loading.setValue(false);
            }
        });
    }
}
