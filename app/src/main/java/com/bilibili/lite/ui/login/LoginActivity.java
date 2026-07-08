package com.bilibili.lite.ui.login;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private ImageView qrImage;
    private TextView statusText, btnRefresh;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        qrImage = findViewById(R.id.qrImage);
        statusText = findViewById(R.id.statusText);
        btnRefresh = findViewById(R.id.btnRefresh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.getQrUrl().observe(this, url -> loadQrImage(url));
        viewModel.getPollCode().observe(this, code -> {
            String msg;
            if (code == 86101) msg = "Scan QR code";
            else if (code == 86090) msg = "Confirm on phone";
            else if (code == 86038) msg = "QR expired";
            else msg = "Waiting...";
            statusText.setText(msg);
        });
        viewModel.getError().observe(this, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
        viewModel.getSuccess().observe(this, ok -> {
            if (ok) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnRefresh.setOnClickListener(v -> {
            statusText.setText("Generating...");
            viewModel.generateQr();
        });

        viewModel.generateQr();
    }

    private void loadQrImage(final String url) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(
                        "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                                + java.net.URLEncoder.encode(url, "UTF-8")).openConnection();
                conn.setConnectTimeout(5000);
                InputStream is = conn.getInputStream();
                final Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                runOnUiThread(() -> qrImage.setImageBitmap(bmp));
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Failed to load QR"));
            }
        }).start();
    }
}
