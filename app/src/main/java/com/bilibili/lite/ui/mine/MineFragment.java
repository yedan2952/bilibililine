package com.bilibili.lite.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.ui.login.LoginActivity;
import com.bilibili.lite.util.ImageLoader;

public class MineFragment extends Fragment {

    private MineViewModel viewModel;
    private ImageView avatar;
    private TextView tvName, tvSign, tvLevel;
    private View loginCard, userCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatar = view.findViewById(R.id.avatar);
        tvName = view.findViewById(R.id.tvName);
        tvSign = view.findViewById(R.id.tvSign);
        tvLevel = view.findViewById(R.id.tvLevel);
        loginCard = view.findViewById(R.id.loginCard);
        userCard = view.findViewById(R.id.userCard);

        loginCard.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), LoginActivity.class)));

        view.findViewById(R.id.menuHistory).setOnClickListener(v ->
                Toast.makeText(getContext(), "History - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.menuFavorites).setOnClickListener(v ->
                Toast.makeText(getContext(), "Favorites - coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.menuSettings).setOnClickListener(v ->
                Toast.makeText(getContext(), "Settings - coming soon", Toast.LENGTH_SHORT).show());

        viewModel = new ViewModelProvider(this).get(MineViewModel.class);
        viewModel.loggedIn.observe(getViewLifecycleOwner(), logged -> {
            loginCard.setVisibility(logged ? View.GONE : View.VISIBLE);
            userCard.setVisibility(logged ? View.VISIBLE : View.GONE);
        });
        viewModel.userInfo.observe(getViewLifecycleOwner(), info -> {
            if (info == null) return;
            ImageLoader.load(avatar, info.getFace());
            tvName.setText(info.getName());
            tvSign.setText(info.getSign());
            tvLevel.setText("Lv" + info.getLevel());
        });

        viewModel.loadUserInfo();
    }
}
