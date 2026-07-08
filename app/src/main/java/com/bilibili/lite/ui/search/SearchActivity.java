package com.bilibili.lite.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.ui.home.VideoFeedAdapter;
import com.bilibili.lite.ui.video.VideoDetailActivity;
import com.bilibili.lite.util.DarkThemeHelper;
import com.bilibili.lite.util.DebugLogger;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private View clearBtn, emptyView;
    private RecyclerView recyclerView;
    private VideoFeedAdapter adapter;
    private SearchViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        DarkThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        DebugLogger.i("Search", "onCreate");

        searchInput = findViewById(R.id.searchInput);
        clearBtn = findViewById(R.id.btnClear);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoFeedAdapter(video -> {
            Intent intent = new Intent(this, VideoDetailActivity.class);
            intent.putExtra("bvid", video.getBvid());
            intent.putExtra("title", video.getTitle());
            intent.putExtra("pic", video.getPic());
            intent.putExtra("author", video.getOwnerName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        clearBtn.setOnClickListener(v -> searchInput.setText(""));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() >= 2) viewModel.search(s.toString());
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 3) {
                    viewModel.loadMore();
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.getResults().observe(this, videos -> {
            adapter.submitList(videos);
            emptyView.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getError().observe(this, err -> {
            if (err != null) {
                DebugLogger.e("Search", "Search error: " + err);
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
