package com.bilibili.lite.ui.discover;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import com.bilibili.lite.util.NetworkUtil;
import java.util.List;

public class DiscoverViewModel extends AndroidViewModel {

    private final VideoRepository repo = VideoRepository.getInstance();
    public final MutableLiveData<List<VideoInfo>> trending = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>();

    public DiscoverViewModel(Application app) { super(app); }

    public void loadTrending() {
        if (!NetworkUtil.isNetworkAvailable(getApplication())) {
            loading.setValue(false);
            return;
        }
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
