package com.bilibili.lite.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.repository.UserRepository;

public class LoginViewModel extends ViewModel {

    private final UserRepository repo = UserRepository.getInstance();
    private final MutableLiveData<String> qrUrl = new MutableLiveData<>();
    private final MutableLiveData<Integer> pollCode = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();
    private String qrcodeKey;
    private boolean polling;

    public LiveData<String> getQrUrl() { return qrUrl; }
    public LiveData<Integer> getPollCode() { return pollCode; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSuccess() { return success; }

    public void generateQr() {
        repo.generateQrCode(new UserRepository.QrCallback() {
            @Override
            public void onGenerated(String url, String key) {
                qrUrl.setValue(url);
                qrcodeKey = key;
                startPolling();
            }

            @Override
            public void onError(String err) {
                error.setValue(err);
            }
        });
    }

    private void startPolling() {
        if (polling) return;
        polling = true;
        pollOnce();
    }

    private void pollOnce() {
        if (!polling || qrcodeKey == null) return;
        repo.pollQrLogin(qrcodeKey, new UserRepository.QrPollCallback() {
            @Override
            public void onResult(int code) {
                pollCode.setValue(code);
                if (code == 0) {
                    polling = false;
                    success.setValue(true);
                } else if (code == 86038 || code == -1) {
                    polling = false;
                    error.setValue("QR expired, regenerate");
                } else {
                    new android.os.Handler().postDelayed(() -> pollOnce(), 1500);
                }
            }

            @Override
            public void onError(String err) {
                error.setValue(err);
                polling = false;
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        polling = false;
    }
}
