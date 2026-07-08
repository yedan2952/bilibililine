package com.bilibili.lite.ui.video;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;

public class VideoDetailViewModel extends ViewModel {

    private final VideoRepository repository = VideoRepository.getInstance();
    private final MutableLiveData<VideoInfo> video = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<VideoInfo> getVideo() { return video; }
    public LiveData<String> getError() { return error; }

    public void loadVideo(String bvid) {
        repository.getVideoDetail(bvid, new VideoRepository.CallbackImpl<VideoInfo>() {
            @Override
            public void onSuccess(VideoInfo result) { video.setValue(result); }
            @Override
            public void onError(String err) { error.setValue(err); }
        });
    }
}
