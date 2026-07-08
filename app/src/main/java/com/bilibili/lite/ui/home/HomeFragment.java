package com.bilibili.lite.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.VideoInfo;
import com.xuexiang.xui.widget.layout.XUILinearLayout;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private VideoFeedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new VideoFeedAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            adapter.submitList(videos);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            swipeRefresh.setRefreshing(loading);
        });

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadPopular());

        viewModel.loadPopular();
    }
}
