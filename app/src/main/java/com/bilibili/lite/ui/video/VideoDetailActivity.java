package com.bilibili.lite.ui.video;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
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
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoDetailActivity extends AppCompatActivity {

    private VideoDetailViewModel viewModel;
    private final VideoRepository repo = VideoRepository.getInstance();

    private TextView tvTitle, tvAuthor, tvStats, tvPubdate, tvDescription;
    private ImageView ivCover, btnFullscreen;
    private TextView btnFavorite, btnSubtitle;
    private PlayerView playerView;
    private View loadingIndicator;
    private LinearLayout relatedContainer, commentsContainer;

    private ExoPlayer exoPlayer;
    private boolean isFullscreen;
    private String bvid;
    private long aid;
    private int currentQn = 32;
    private String[] backupUrls;
    private int currentUrlIndex = 0;
    private int originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private ScrollView contentScrollView;
    private View btnLoadMoreComments;
    private int commentPage = 1;
    private boolean hasMoreComments = true;

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
        tvPubdate = findViewById(R.id.tvPubdate);
        tvDescription = findViewById(R.id.tvDescription);
        ivCover = findViewById(R.id.ivCover);
        playerView = findViewById(R.id.playerView);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnSubtitle = findViewById(R.id.btnSubtitle);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        relatedContainer = findViewById(R.id.relatedContainer);
        commentsContainer = findViewById(R.id.commentsContainer);
        btnLoadMoreComments = findViewById(R.id.btnLoadMoreComments);
        contentScrollView = findViewById(R.id.contentScrollView);

        tvTitle.setText(getIntent().getStringExtra("title"));
        tvAuthor.setText(getIntent().getStringExtra("author"));
        ImageLoader.load(ivCover, getIntent().getStringExtra("pic"));

        updateFavoriteButton();

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnSubtitle.setOnClickListener(v -> Toast.makeText(this, "\u5b57\u5e55 - \u5373\u5c06\u6765\u4e34", Toast.LENGTH_SHORT).show());
        btnLoadMoreComments.setOnClickListener(v -> loadMoreComments());

        // ExoPlayer's PlayerView handles play/pause, seek, and time display automatically

        viewModel = new ViewModelProvider(this).get(VideoDetailViewModel.class);
        viewModel.getVideo().observe(this, video -> {
            if (video == null) return;
            aid = video.getAid();
            tvTitle.setText(video.getTitle());
            tvAuthor.setText(video.getOwnerName());
            tvStats.setText(formatStat(video.getPlayCount(), video.getDanmakuCount()));
            ImageLoader.load(ivCover, video.getPic());
            // Show publish date
            if (video.getPubdate() > 0) {
                tvPubdate.setVisibility(View.VISIBLE);
                tvPubdate.setText("\\u53d1\\u5e03\\u4e8e " + formatDate(video.getPubdate()));
            }
            // Show description
            String desc = video.getDescription();
            if (desc != null && !desc.isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(desc);
                tvDescription.setOnClickListener(v -> {
                    if (tvDescription.getMaxLines() == 3) {
                        tvDescription.setMaxLines(Integer.MAX_VALUE);
                    } else {
                        tvDescription.setMaxLines(3);
                    }
                });
            }
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
                backupUrls = r.backupUrls;
                currentUrlIndex = 0;
                initExoPlayer(r.url);
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

    private void initExoPlayer(String url) {
        releasePlayer();
        showLoading(true);
        ivCover.setVisibility(View.GONE);

        // Build data source factory with Referer and User-Agent headers
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Constants.USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS);
        java.util.HashMap<String, String> defaultHeaders = new java.util.HashMap<>();
        defaultHeaders.put("Referer", "https://www.bilibili.com");
        defaultHeaders.put("User-Agent", Constants.USER_AGENT);
        dataSourceFactory.setDefaultRequestProperties(defaultHeaders);

        // Create media source
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(url)));

        // Build ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this)
                .build();
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    showLoading(false);
                    playerView.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_ENDED) {
                    // Video finished
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                showLoading(false);
                DebugLogger.e("VideoDetail", "ExoPlayer error urlIndex=" + currentUrlIndex
                        + " error=" + error.getLocalizedMessage()
                        + " code=" + error.getErrorCodeName());
                tryFallback();
            }
        });

        playerView.setPlayer(exoPlayer);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare();
    }

    private void tryFallback() {
        if (currentUrlIndex < (backupUrls != null ? backupUrls.length : 0) - 1) {
            currentUrlIndex++;
            String fallbackUrl = backupUrls[currentUrlIndex];
            DebugLogger.i("VideoDetail", "Trying backup URL #" + currentUrlIndex);
            Toast.makeText(VideoDetailActivity.this, "正在切换到备用线路...", Toast.LENGTH_SHORT).show();
            showLoading(true);
            initExoPlayer(fallbackUrl);
        } else {
            String errorMsg = "视频加载失败，请尝试切换画质或稍后重试";
            Toast.makeText(VideoDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        playerView.setPlayer(null);
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
        commentPage = 1;
        hasMoreComments = true;
        commentsContainer.removeAllViews();
        fetchComments(1);
    }

    private void loadMoreComments() {
        if (!hasMoreComments) return;
        commentPage++;
        fetchComments(commentPage);
    }

    private void fetchComments(int page) {
        if (aid <= 0) return;
        repo.getComments(aid, page, new VideoRepository.CallbackImpl<List<CommentItem>>() {
            @Override public void onSuccess(List<CommentItem> list) {
                if (list == null || list.isEmpty()) {
                    hasMoreComments = false;
                    btnLoadMoreComments.setVisibility(View.GONE);
                    return;
                }
                hasMoreComments = list.size() >= 20;
                int max = Math.min(list.size(), 20);
                for (int i = 0; i < max; i++) {
                    CommentItem c = list.get(i);
                    View item = getLayoutInflater().inflate(R.layout.item_comment, commentsContainer, false);
                    ImageLoader.load((ImageView) item.findViewById(R.id.ivAvatar), c.getUserAvatar());
                    ((TextView) item.findViewById(R.id.tvUserName)).setText(c.getUserName());
                    ((TextView) item.findViewById(R.id.tvMessage)).setText(c.getMessage());
                    ((TextView) item.findViewById(R.id.tvLike)).setText(c.getLike() + " \u8d5e");
                    commentsContainer.addView(item);
                }
                btnLoadMoreComments.setVisibility(hasMoreComments ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(String e) {
                DebugLogger.e("VideoDetail", "loadComments page=" + page + " failed: " + e);
                if (page > 1) commentPage--; // Roll back page counter on error
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

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            // Save original orientation before locking
            originalOrientation = getRequestedOrientation();
            // Lock to landscape
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            // Hide content and fill screen with player
            contentScrollView.setVisibility(View.GONE);
            // Immersive fullscreen
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_revert);
        } else {
            // Restore original orientation
            setRequestedOrientation(originalOrientation);
            // Show content again
            contentScrollView.setVisibility(View.VISIBLE);
            // Exit immersive mode
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_zoom);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Auto fullscreen when rotating to landscape, exit when portrait
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && !isFullscreen) {
            toggleFullscreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && isFullscreen) {
            toggleFullscreen();
        }
    }

    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp * 1000));
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
        if (exoPlayer != null && exoPlayer.isPlaying()) exoPlayer.pause();
    }

    @Override protected void onStop() {
        super.onStop();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
