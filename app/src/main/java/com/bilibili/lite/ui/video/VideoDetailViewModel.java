package com.bilibili.lite.ui.video;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import com.bilibili.lite.util.NetworkUtil;

public class VideoDetailViewModel extends AndroidViewModel {

    private final VideoRepository repository = VideoRepository.getInstance();
    private final MutableLiveData<VideoInfo> video = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public VideoDetailViewModel(Application app) { super(app); }

    public LiveData<VideoInfo> getVideo() { return video; }
    public LiveData<String> getError() { return error; }

    public void loadVideo(String bvid) {
        if (!NetworkUtil.isNetworkAvailable(getApplication())) {
            error.setValue("无网络连接，无法加载视频");
            return;
        }
        repository.getVideoDetail(bvid, new VideoRepository.CallbackImpl<VideoInfo>() {
            @Override
            public void onSuccess(VideoInfo result) { video.setValue(result); }
            @Override
            public void onError(String err) {
                error.setValue(NetworkUtil.getNetworkErrorMessage(
                        new Exception(err), getApplication()));
            }
        });
    }
}
