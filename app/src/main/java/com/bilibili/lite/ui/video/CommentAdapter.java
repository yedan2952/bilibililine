package com.bilibili.lite.ui.video;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bilibili.lite.R;
import com.bilibili.lite.data.model.CommentItem;
import com.bilibili.lite.util.ImageLoader;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final List<CommentItem> list = new ArrayList<>();

    public void submitList(List<CommentItem> items) {
        list.clear();
        if (items != null) list.addAll(items);
        notifyDataSetChanged();
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup p, int i) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_comment, p, false));
    }

    @Override public void onBindViewHolder(ViewHolder h, int p) {
        CommentItem c = list.get(p);
        ImageLoader.load(h.avatar, c.getUserAvatar());
        h.userName.setText(c.getUserName());
        h.message.setText(c.getMessage());
        h.like.setText(c.getLike() + " \u8d5e");
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView userName, message, like;
        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.ivAvatar);
            userName = v.findViewById(R.id.tvUserName);
            message = v.findViewById(R.id.tvMessage);
            like = v.findViewById(R.id.tvLike);
        }
    }
}
