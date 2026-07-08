package com.bilibili.lite.ui.video;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;

public class VideoDetailActivity extends AppCompatActivity {

    private VideoDetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);
        viewModel = new ViewModelProvider(this).get(VideoDetailViewModel.class);
    }
}
