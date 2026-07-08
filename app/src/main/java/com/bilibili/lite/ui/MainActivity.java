package com.bilibili.lite.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.bilibili.lite.R;
import com.bilibili.lite.ui.discover.DiscoverFragment;
import com.bilibili.lite.ui.home.HomeFragment;
import com.bilibili.lite.ui.mine.MineFragment;
import com.bilibili.lite.ui.search.SearchActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xuexiang.xui.widget.actionbar.TitleBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private BottomNavigationView bottomNav;
    private TitleBar titleBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleBar = findViewById(R.id.titleBar);
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        titleBar.setTitle(getString(R.string.app_name));
        titleBar.addRightAction(new TitleBar.ImageAction(R.drawable.ic_search) {
            @Override
            public void performAction() {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new HomeFragment());
        fragments.add(new DiscoverFragment());
        fragments.add(new MineFragment());

        viewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager(), fragments));
        viewPager.setOffscreenPageLimit(2);

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                bottomNav.getMenu().getItem(position).setChecked(true);
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0);
            else if (id == R.id.nav_discover) viewPager.setCurrentItem(1);
            else if (id == R.id.nav_mine) viewPager.setCurrentItem(2);
            return true;
        });
    }

    private static class MainPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments;

        MainPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = fragments;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
