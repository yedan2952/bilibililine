package com.bilibili.lite.ui.debug;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bilibili.lite.R;
import com.bilibili.lite.util.DebugLogger;

public class DebugActivity extends AppCompatActivity {

    private TextView logView;
    private boolean autoRefresh = true;
    private Runnable refresher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        logView = findViewById(R.id.logView);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnAuto = findViewById(R.id.btnAuto);

        refreshLogs();
        btnRefresh.setOnClickListener(v -> refreshLogs());
        btnClear.setOnClickListener(v -> {
            DebugLogger.clear();
            refreshLogs();
        });
        btnShare.setOnClickListener(v -> {
            String logs = DebugLogger.getLogs();
            if (logs.isEmpty()) { Toast.makeText(this, "No logs", Toast.LENGTH_SHORT).show(); return; }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, logs);
            startActivity(Intent.createChooser(share, "Share logs"));
        });
        btnAuto.setOnClickListener(v -> {
            autoRefresh = !autoRefresh;
            btnAuto.setText(autoRefresh ? "Auto ON" : "Auto OFF");
            if (autoRefresh) startAutoRefresh();
            else stopAutoRefresh();
        });
        startAutoRefresh();
    }

    private void refreshLogs() {
        logView.setText(DebugLogger.getLogs());
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void startAutoRefresh() {
        refresher = () -> {
            if (autoRefresh) {
                refreshLogs();
                logView.postDelayed(refresher, 1000);
            }
        };
        logView.postDelayed(refresher, 1000);
    }

    private void stopAutoRefresh() {
        if (refresher != null) logView.removeCallbacks(refresher);
    }

    @Override protected void onDestroy() {
        stopAutoRefresh();
        super.onDestroy();
    }
}
