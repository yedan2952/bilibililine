package com.bilibili.lite.ui.mine;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.bilibili.lite.data.model.UserInfo;
import com.bilibili.lite.data.repository.UserRepository;
import com.bilibili.lite.util.NetworkUtil;

public class MineViewModel extends AndroidViewModel {

    private final UserRepository repo = UserRepository.getInstance();
    public final MutableLiveData<UserInfo> userInfo = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loggedIn = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>();

    public MineViewModel(Application app) { super(app); }

    public void loadUserInfo() {
        if (!NetworkUtil.isNetworkAvailable(getApplication())) {
            loggedIn.setValue(false);
            return;
        }
        repo.getNavInfo(new UserRepository.CallbackImpl<UserInfo>() {
            @Override
            public void onSuccess(UserInfo result) {
                userInfo.setValue(result);
                loggedIn.setValue(true);
            }

            @Override
            public void onError(String err) {
                loggedIn.setValue(false);
                error.setValue(NetworkUtil.getNetworkErrorMessage(
                        new Exception(err), getApplication()));
            }
        });
    }
}
