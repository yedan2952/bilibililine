package com.bilibili.lite.ui.video;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.data.repository.VideoRepository;
import com.bilibili.lite.util.ImageLoader;
import java.io.IOException;

public class VideoDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor, tvStats, tvCurrentTime, tvTotalTime;
    private ImageView ivCover, btnPlayPause, btnFullscreen;
    private SurfaceView surfaceView;
    private View playerControls, loadingIndicator;
    private ProgressBar loadingSpinner;
    private SeekBar seekBar;
    private VideoDetailViewModel viewModel;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying, isFullscreen, controlsVisible;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        String bvid = getIntent().getStringExtra("bvid");
        String title = getIntent().getStringExtra("title");
        String pic = getIntent().getStringExtra("pic");
        String author = getIntent().getStringExtra("author");

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvStats = findViewById(R.id.tvStats);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        ivCover = findViewById(R.id.ivCover);
        surfaceView = findViewById(R.id.surfaceView);
        playerControls = findViewById(R.id.playerControls);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        seekBar = findViewById(R.id.seekBar);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        loadingSpinner = findViewById(R.id.loadingSpinner);

        tvTitle.setText(title);
        tvAuthor.setText(author);
        ImageLoader.load(ivCover, pic);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        playerControls.setOnClickListener(v -> {});
        surfaceView.setOnClickListener(v -> toggleControls());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (mediaPlayer != null) mediaPlayer.seekTo(sb.getProgress());
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mediaPlayer != null) mediaPlayer.setDisplay(holder);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });

        viewModel = new ViewModelProvider(this).get(VideoDetailViewModel.class);
        viewModel.getVideo().observe(this, video -> {
            if (video == null) return;
            tvTitle.setText(video.getTitle());
            tvAuthor.setText(video.getOwnerName());
            tvStats.setText(formatStat(video.getPlayCount(), video.getDanmakuCount()));
            ImageLoader.load(ivCover, video.getPic());
            loadVideoUrl(bvid != null ? bvid : video.getBvid(), video.getCid());
        });
        viewModel.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        if (bvid != null) viewModel.loadVideo(bvid);
    }

    private void loadVideoUrl(String bvid, long cid) {
        if (cid <= 0) return;
        showLoading(true);
        VideoRepository.getInstance().getPlayUrl(bvid, cid, new VideoRepository.CallbackImpl<String>() {
            @Override
            public void onSuccess(String url) {
                initPlayer(url);
            }
            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(VideoDetailActivity.this, "Video URL: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initPlayer(String url) {
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, Uri.parse(url));
            mediaPlayer.setDisplay(surfaceView.getHolder());
            mediaPlayer.setOnPreparedListener(mp -> {
                showLoading(false);
                ivCover.setVisibility(View.GONE);
                seekBar.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
                mp.start();
                isPlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                showControls();
                handler.post(updateProgress);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                showLoading(false);
                Toast.makeText(this, "Playback error (" + what + ")", Toast.LENGTH_LONG).show();
                return true;
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            });
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            showLoading(false);
            Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this, 250);
            }
        }
    };

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            handler.post(updateProgress);
        }
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_revert);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_zoom);
        }
    }

    private void toggleControls() {
        if (controlsVisible) hideControls();
        else showControls();
    }

    private void showControls() {
        controlsVisible = true;
        playerControls.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideControlsTask);
        handler.postDelayed(hideControlsTask, 3000);
    }

    private void hideControls() {
        controlsVisible = false;
        playerControls.setVisibility(View.GONE);
    }

    private Runnable hideControlsTask = this::hideControls;

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void releasePlayer() {
        handler.removeCallbacks(updateProgress);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String formatTime(int ms) {
        int s = ms / 1000;
        int m = s / 60;
        s = s % 60;
        return String.format("%02d:%02d", m, s);
    }

    private String formatStat(long play, long danmaku) {
        return (play > 10000 ? (play / 10000) + "\u4e07" : play) + "\u64ad\u653e \u00b7 "
             + (danmaku > 10000 ? (danmaku / 10000) + "\u4e07" : danmaku) + "\u5f39\u5e55";
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        handler.removeCallbacks(hideControlsTask);
        super.onDestroy();
    }
}
