package com.bilibili.lite.ui.mine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.ui.debug.DebugActivity;
import com.bilibili.lite.ui.login.LoginActivity;
import com.bilibili.lite.util.DarkThemeHelper;
import com.bilibili.lite.util.DebugLogger;
import com.bilibili.lite.util.ImageLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class MineFragment extends Fragment {

    private MineViewModel viewModel;
    private ImageView avatar;
    private TextView tvName, tvSign, tvLevel, tvHistoryCount, tvFavoriteCount;
    private View loginCard, userCard;

    // Launcher to handle login result and refresh user info
    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == LoginActivity.RESULT_OK) {
                    DebugLogger.i("Mine", "Login success, refreshing user info");
                    viewModel.loadUserInfo();
                }
            });

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
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);

        loginCard.setOnClickListener(v ->
                loginLauncher.launch(new Intent(getActivity(), LoginActivity.class)));

        view.findViewById(R.id.menuHistory).setOnClickListener(v -> {
            SharedPreferences prefs = getActivity().getSharedPreferences("bili", 0);
            String json = prefs.getString("history", "[]");
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> h = new Gson().fromJson(json, t);
            Toast.makeText(getContext(), "History: " + h.size() + " videos", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.menuFavorites).setOnClickListener(v -> {
            SharedPreferences prefs = getActivity().getSharedPreferences("bili", 0);
            int count = prefs.getStringSet("favorites", new java.util.HashSet<>()).size();
            Toast.makeText(getContext(), "Favorites: " + count + " videos", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            boolean dark = DarkThemeHelper.isDark(getActivity());
            new AlertDialog.Builder(getContext())
                    .setTitle("Settings")
                    .setItems(new String[]{
                            (dark ? "\u2713 " : "") + "Dark Mode",
                            "Debug Log"
                    }, (d, i) -> {
                        if (i == 0) DarkThemeHelper.toggle(getActivity());
                        else startActivity(new Intent(getActivity(), DebugActivity.class));
                    }).show();
        });

        updateCounts();

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

    @Override
    public void onResume() {
        super.onResume();
        updateCounts();
    }

    private void updateCounts() {
        SharedPreferences prefs = getActivity().getSharedPreferences("bili", 0);
        String json = prefs.getString("history", "[]");
        Type t = new TypeToken<List<String>>(){}.getType();
        List<String> h = new Gson().fromJson(json, t);
        int fav = prefs.getStringSet("favorites", new java.util.HashSet<>()).size();
        if (tvHistoryCount != null) tvHistoryCount.setText(h.size() + " items");
        if (tvFavoriteCount != null) tvFavoriteCount.setText(fav + " items");
    }
}
