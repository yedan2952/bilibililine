package com.bilibili.lite.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.VideoInfo;
import com.bilibili.lite.util.ImageLoader;
import java.util.ArrayList;
import java.util.List;

public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.ViewHolder> {

    private final List<VideoInfo> list = new ArrayList<>();
    private final OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(VideoInfo video);
    }

    public VideoFeedAdapter(OnVideoClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<VideoInfo> videos) {
        list.clear();
        if (videos != null) list.addAll(videos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoInfo video = list.get(position);
        ImageLoader.load(holder.cover, video.getPic());
        holder.title.setText(video.getTitle());
        holder.author.setText(video.getOwnerName());
        holder.playCount.setText(formatCount(video.getPlayCount()));
        holder.itemView.setOnClickListener(v -> listener.onVideoClick(video));
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatCount(long count) {
        if (count >= 10000) return (count / 10000) + "\u4e07";
        return String.valueOf(count);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, author, playCount;

        ViewHolder(View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ivCover);
            title = itemView.findViewById(R.id.tvTitle);
            author = itemView.findViewById(R.id.tvAuthor);
            playCount = itemView.findViewById(R.id.tvPlayCount);
        }
    }
}
