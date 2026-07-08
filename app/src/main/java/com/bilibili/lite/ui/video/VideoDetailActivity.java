package com.bilibili.lite.ui.video;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.CommentItem;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.data.repository.VideoRepository;
import com.bilibili.lite.util.Constants;
import com.bilibili.lite.util.DarkThemeHelper;
import com.bilibili.lite.util.DebugLogger;
import com.bilibili.lite.util.ImageLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoDetailActivity extends AppCompatActivity {

    private VideoDetailViewModel viewModel;
    private final VideoRepository repo = VideoRepository.getInstance();

    private TextView tvTitle, tvAuthor, tvStats, tvCurrentTime, tvTotalTime;
    private ImageView ivCover, btnPlayPause, btnFullscreen, btnSpeed, btnQuality;
    private TextView btnFavorite, btnSubtitle;
    private SurfaceView surfaceView;
    private View playerControls, loadingIndicator;
    private ProgressBar loadingSpinner;
    private SeekBar seekBar;
    private LinearLayout relatedContainer, commentsContainer;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isPlaying, isFullscreen, controlsVisible;
    private String bvid;
    private long aid;
    private int currentQn = 32;
    private float currentSpeed = 1.0f;
    private int[] acceptQuality;
    private String[] acceptDesc;
    private String[] backupUrls;
    private int currentUrlIndex = 0;

    private SharedPreferences prefs;
    private Set<String> favorites;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        DarkThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);
        DebugLogger.i("VideoDetail", "onCreate bvid=" + getIntent().getStringExtra("bvid"));

        bvid = getIntent().getStringExtra("bvid");
        prefs = getSharedPreferences("bili", MODE_PRIVATE);
        favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));

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
        btnSpeed = findViewById(R.id.btnSpeed);
        btnQuality = findViewById(R.id.btnQuality);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnSubtitle = findViewById(R.id.btnSubtitle);
        seekBar = findViewById(R.id.seekBar);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        relatedContainer = findViewById(R.id.relatedContainer);
        commentsContainer = findViewById(R.id.commentsContainer);

        tvTitle.setText(getIntent().getStringExtra("title"));
        tvAuthor.setText(getIntent().getStringExtra("author"));
        ImageLoader.load(ivCover, getIntent().getStringExtra("pic"));

        updateFavoriteButton();

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnSpeed.setOnClickListener(v -> showSpeedDialog());
        btnQuality.setOnClickListener(v -> showQualityDialog());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnSubtitle.setOnClickListener(v -> Toast.makeText(this, "Subtitle - coming soon", Toast.LENGTH_SHORT).show());
        surfaceView.setOnClickListener(v -> toggleControls());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {}
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (mediaPlayer != null) mediaPlayer.seekTo(sb.getProgress());
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                if (mediaPlayer != null) mediaPlayer.setDisplay(holder);
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });

        viewModel = new ViewModelProvider(this).get(VideoDetailViewModel.class);
        viewModel.getVideo().observe(this, video -> {
            if (video == null) return;
            aid = video.getAid();
            tvTitle.setText(video.getTitle());
            tvAuthor.setText(video.getOwnerName());
            tvStats.setText(formatStat(video.getPlayCount(), video.getDanmakuCount()));
            ImageLoader.load(ivCover, video.getPic());
            loadVideo(bvid != null ? bvid : video.getBvid(), video.getCid());
        });

        if (bvid != null) viewModel.loadVideo(bvid);
        saveHistory();
    }

    private void loadVideo(String bvid, long cid) {
        if (cid <= 0) return;
        loadUrl(bvid, cid, currentQn);
        loadRelated(bvid);
        loadComments();
    }

    private void loadUrl(String bvid, long cid, int qn) {
        showLoading(true);
        repo.getPlayUrl(bvid, cid, qn, new VideoRepository.CallbackImpl<VideoRepository.PlayUrlResult>() {
            @Override public void onSuccess(VideoRepository.PlayUrlResult r) {
                DebugLogger.i("VideoDetail", "Got play URL, qn=" + qn + " url=" + (r.url != null ? r.url.substring(0, Math.min(80, r.url.length())) : "null"));
                acceptQuality = r.acceptQuality;
                acceptDesc = r.acceptDesc;
                backupUrls = r.backupUrls;
                currentUrlIndex = 0;
                initPlayer(r.url);
            }
            @Override public void onError(String e) {
                showLoading(false);
                DebugLogger.e("VideoDetail", "Play URL error: " + e);
                String msg = e;
                if (msg != null && msg.contains("Unable to resolve host")) {
                    msg = "网络连接失败，请检查网络设置或切换网络后重试";
                } else if (msg != null && msg.contains("timeout") || (msg != null && msg.contains("Timeout"))) {
                    msg = "连接超时，请检查网络后重试";
                }
                Toast.makeText(VideoDetailActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initPlayer(String url) {
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        try {
            // Bilibili CDN requires Referer and User-Agent headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.bilibili.com");
            headers.put("User-Agent", Constants.USER_AGENT);
            mediaPlayer.setDataSource(this, Uri.parse(url), headers);
            mediaPlayer.setDisplay(surfaceView.getHolder());
            mediaPlayer.setOnPreparedListener(mp -> {
                showLoading(false);
                ivCover.setVisibility(View.GONE);
                seekBar.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
                applySpeed();
                mp.start();
                isPlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                showControls();
                handler.post(updateProgress);
            });
            mediaPlayer.setOnErrorListener((mp, w, e) -> {
                showLoading(false);
                DebugLogger.e("VideoDetail", "MediaPlayer error what=" + w + " extra=" + e + " urlIndex=" + currentUrlIndex);
                // Try backup URLs if available
                if (currentUrlIndex < (backupUrls != null ? backupUrls.length : 0) - 1) {
                    currentUrlIndex++;
                    String fallbackUrl = backupUrls[currentUrlIndex];
                    DebugLogger.i("VideoDetail", "Trying backup URL #" + currentUrlIndex + ": " + fallbackUrl.substring(0, Math.min(80, fallbackUrl.length())));
                    Toast.makeText(VideoDetailActivity.this, "正在切换到备用线路...", Toast.LENGTH_SHORT).show();
                    showLoading(true);
                    initPlayer(fallbackUrl);
                } else {
                    String errorMsg;
                    if (w == MediaPlayer.MEDIA_ERROR_IO) {
                        errorMsg = "视频流加载失败，请尝试切换画质或稍后重试";
                    } else if (w == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                        errorMsg = "视频播放超时，请检查网络后重试";
                    } else {
                        errorMsg = "视频播放出错 (" + w + ")，请稍后重试";
                    }
                    Toast.makeText(VideoDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
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
            // Try fallback if the primary URL is invalid
            if (backupUrls != null && backupUrls.length > 0 && currentUrlIndex < backupUrls.length - 1) {
                currentUrlIndex++;
                DebugLogger.i("VideoDetail", "Primary URL failed, trying backup #" + currentUrlIndex);
                initPlayer(backupUrls[currentUrlIndex]);
            } else {
                Toast.makeText(this, "无法播放视频: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void applySpeed() {
        if (mediaPlayer == null) return;
        if (Build.VERSION.SDK_INT >= 23) {
            PlaybackParams pp = new PlaybackParams();
            pp.setSpeed(currentSpeed);
            mediaPlayer.setPlaybackParams(pp);
        }
    }

    private void showSpeedDialog() {
        String[] speeds = {"0.5x", "1.0x", "1.5x", "2.0x"};
        float[] vals = {0.5f, 1.0f, 1.5f, 2.0f};
        new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setItems(speeds, (d, i) -> {
                    currentSpeed = vals[i];
                    applySpeed();
                    Toast.makeText(this, speeds[i], Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void showQualityDialog() {
        if (acceptDesc == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Quality")
                .setItems(acceptDesc, (d, i) -> {
                    currentQn = acceptQuality[i];
                    loadUrl(bvid, viewModel.getVideo().getValue() != null
                            ? viewModel.getVideo().getValue().getCid() : 0, currentQn);
                }).show();
    }

    private void toggleFavorite() {
        if (favorites.contains(bvid)) {
            favorites.remove(bvid);
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            favorites.add(bvid);
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("favorites", favorites).apply();
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        btnFavorite.setText(favorites.contains(bvid) ? "\u2605 \u5df2\u6536\u85cf" : "\u2606 \u6536\u85cf");
    }

    private void saveHistory() {
        String json = prefs.getString("history", "[]");
        Type t = new TypeToken<ArrayList<String>>(){}.getType();
        List<String> h = new Gson().fromJson(json, t);
        h.remove(bvid);
        h.add(0, bvid);
        if (h.size() > 200) h = h.subList(0, 200);
        prefs.edit().putString("history", new Gson().toJson(h)).apply();
    }

    private void loadRelated(String bvid) {
        repo.getRelated(bvid, new VideoRepository.CallbackImpl<List<VideoInfo>>() {
            @Override public void onSuccess(List<VideoInfo> list) {
                relatedContainer.removeAllViews();
                int max = Math.min(list.size(), 10);
                for (int i = 0; i < max; i++) {
                    VideoInfo v = list.get(i);
                    View card = getLayoutInflater().inflate(R.layout.item_related_video, relatedContainer, false);
                    ImageLoader.load((ImageView) card.findViewById(R.id.ivCover), v.getPic());
                    ((TextView) card.findViewById(R.id.tvTitle)).setText(v.getTitle());
                    ((TextView) card.findViewById(R.id.tvAuthor)).setText(v.getOwnerName());
                    ((TextView) card.findViewById(R.id.tvPlayCount)).setText(formatCount(v.getPlayCount()));
                    int fi = i;
                    card.setOnClickListener(vv -> openVideo(list.get(fi)));
                    relatedContainer.addView(card);
                }
            }
            @Override public void onError(String e) {
                DebugLogger.e("VideoDetail", "loadRelated failed: " + e);
                relatedContainer.removeAllViews();
                View tip = getLayoutInflater().inflate(R.layout.item_related_video, relatedContainer, false);
                ((TextView) tip.findViewById(R.id.tvTitle)).setText("推荐加载失败");
                relatedContainer.addView(tip);
            }
        });
    }

    private void loadComments() {
        if (aid <= 0) return;
        repo.getComments(aid, 1, new VideoRepository.CallbackImpl<List<CommentItem>>() {
            @Override public void onSuccess(List<CommentItem> list) {
                commentsContainer.removeAllViews();
                int max = Math.min(list.size(), 10);
                for (int i = 0; i < max; i++) {
                    CommentItem c = list.get(i);
                    View item = getLayoutInflater().inflate(R.layout.item_comment, commentsContainer, false);
                    ImageLoader.load((ImageView) item.findViewById(R.id.ivAvatar), c.getUserAvatar());
                    ((TextView) item.findViewById(R.id.tvUserName)).setText(c.getUserName());
                    ((TextView) item.findViewById(R.id.tvMessage)).setText(c.getMessage());
                    ((TextView) item.findViewById(R.id.tvLike)).setText(c.getLike() + " \u8d5e");
                    commentsContainer.addView(item);
                }
            }
            @Override public void onError(String e) {
                DebugLogger.e("VideoDetail", "loadComments failed: " + e);
            }
        });
    }

    private void openVideo(VideoInfo v) {
        startActivity(new android.content.Intent(this, VideoDetailActivity.class)
                .putExtra("bvid", v.getBvid())
                .putExtra("title", v.getTitle())
                .putExtra("pic", v.getPic())
                .putExtra("author", v.getOwnerName()));
    }

    private final Runnable updateProgress = () -> {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(this.updateProgress, 250);
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

    private final Runnable hideControlsTask = this::hideControls;

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
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String formatStat(long play, long danmaku) {
        return (play > 10000 ? (play / 10000) + "\u4e07" : play) + " \u64ad\u653e \u00b7 "
             + (danmaku > 10000 ? (danmaku / 10000) + "\u4e07" : danmaku) + " \u5f39\u5e55";
    }

    private String formatCount(long c) {
        return c >= 10000 ? (c / 10000) + "\u4e07" : String.valueOf(c);
    }

    @Override protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override protected void onDestroy() {
        releasePlayer();
        handler.removeCallbacks(hideControlsTask);
        super.onDestroy();
    }
}
