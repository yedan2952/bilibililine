package com.bilibili.lite.ui.login;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bilibili.lite.R;
import com.bilibili.lite.data.remote.RetrofitClient;
import com.bilibili.lite.util.DarkThemeHelper;
import com.bilibili.lite.util.DebugLogger;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private ImageView qrImage;
    private TextView statusText, btnRefresh;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        DarkThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        DebugLogger.i("Login", "onCreate");

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
            if (err != null) {
                DebugLogger.e("Login", "Login error: " + err);
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
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

    private void loadQrImage(String url) {
        OkHttpClient client = RetrofitClient.getInstance().getOkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                        + java.net.URLEncoder.encode(url))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                DebugLogger.e("Login", "QR load failed", e);
                runOnUiThread(() -> statusText.setText("二维码加载失败，请点击刷新"));
            }
            @Override public void onResponse(Call call, Response response) {
                try (InputStream is = response.body() != null ? response.body().byteStream() : null) {
                    if (is != null) {
                        final Bitmap bmp = BitmapFactory.decodeStream(is);
                        runOnUiThread(() -> qrImage.setImageBitmap(bmp));
                    } else {
                        runOnUiThread(() -> statusText.setText("二维码加载失败"));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> statusText.setText("二维码加载失败"));
                }
            }
        });
    }
}
