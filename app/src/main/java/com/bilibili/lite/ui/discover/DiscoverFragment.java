package com.bilibili.lite.ui.discover;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.ui.home.VideoFeedAdapter;
import com.bilibili.lite.ui.search.SearchActivity;
import com.bilibili.lite.ui.video.VideoDetailActivity;

public class DiscoverFragment extends Fragment {

    private DiscoverViewModel viewModel;
    private VideoFeedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new VideoFeedAdapter(video -> {
            Intent intent = new Intent(getActivity(), VideoDetailActivity.class);
            intent.putExtra("bvid", video.getBvid());
            intent.putExtra("title", video.getTitle());
            intent.putExtra("pic", video.getPic());
            intent.putExtra("author", video.getOwnerName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.searchBar).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));

        TextView categoryLive = view.findViewById(R.id.categoryLive);
        categoryLive.setOnClickListener(v -> Toast.makeText(getContext(), "Live - coming soon", Toast.LENGTH_SHORT).show());

        viewModel = new ViewModelProvider(this).get(DiscoverViewModel.class);
        viewModel.trending.observe(getViewLifecycleOwner(), videos -> adapter.submitList(videos));
        viewModel.loadTrending();
    }
}
