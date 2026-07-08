package com.bilibili.lite.ui.discover;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import java.util.List;

public class DiscoverViewModel extends ViewModel {

    private final VideoRepository repo = VideoRepository.getInstance();
    public final MutableLiveData<List<VideoInfo>> trending = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>();

    public void loadTrending() {
        loading.setValue(true);
        repo.fetchPopular(new VideoRepository.CallbackImpl<List<VideoInfo>>() {
            @Override
            public void onSuccess(List<VideoInfo> result) {
                trending.setValue(result);
                loading.setValue(false);
            }

            @Override
            public void onError(String error) {
                loading.setValue(false);
            }
        });
    }
}
