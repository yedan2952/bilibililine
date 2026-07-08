package com.bilibili.lite.ui.video;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.util.ImageLoader;
import java.io.IOException;

public class VideoDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor, tvStats;
    private ImageView ivCover;
    private SurfaceView surfaceView;
    private View playerControls, btnPlayPause;
    private VideoDetailViewModel viewModel;
    private MediaPlayer mediaPlayer;
    private String bvid, videoUrl, title, pic, author;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        bvid = getIntent().getStringExtra("bvid");
        title = getIntent().getStringExtra("title");
        pic = getIntent().getStringExtra("pic");
        author = getIntent().getStringExtra("author");

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvStats = findViewById(R.id.tvStats);
        ivCover = findViewById(R.id.ivCover);
        surfaceView = findViewById(R.id.surfaceView);
        playerControls = findViewById(R.id.playerControls);
        btnPlayPause = findViewById(R.id.btnPlayPause);

        tvTitle.setText(title);
        tvAuthor.setText(author);
        ImageLoader.load(ivCover, pic);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { playVideo(holder); }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { releasePlayer(); }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setVisibility(View.VISIBLE);
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setVisibility(View.GONE);
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(VideoDetailViewModel.class);
        viewModel.getVideo().observe(this, video -> {
            if (video != null) {
                tvTitle.setText(video.getTitle());
                tvAuthor.setText(video.getOwnerName());
                tvStats.setText(formatStat(video.getPlayCount(), video.getDanmakuCount()));
                ImageLoader.load(ivCover, video.getPic());
                resolveVideoUrl(video.getAid(), video.getBvid());
            }
        });
        viewModel.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        if (bvid != null) viewModel.loadVideo(bvid);
    }

    private void resolveVideoUrl(long aid, String bvid) {
        videoUrl = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid + "&cid=0&qn=32";
    }

    private void playVideo(SurfaceHolder holder) {
        if (videoUrl == null) return;
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDisplay(holder);
        try {
            mediaPlayer.setDataSource(this, Uri.parse(videoUrl));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                ivCover.setVisibility(View.GONE);
                playerControls.setVisibility(View.GONE);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback error: " + what, Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String formatStat(long play, long danmaku) {
        return (play > 10000 ? (play / 10000) + "万" : play) + "播放 · "
             + (danmaku > 10000 ? (danmaku / 10000) + "万" : danmaku) + "弹幕";
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
