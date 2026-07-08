package com.bilibili.lite.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final VideoRepository repository = VideoRepository.getInstance();
    private final MutableLiveData<List<VideoInfo>> videos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<VideoInfo>> getVideos() { return videos; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadPopular() {
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
                error.setValue(err);
                loading.setValue(false);
            }
        });
    }
}
