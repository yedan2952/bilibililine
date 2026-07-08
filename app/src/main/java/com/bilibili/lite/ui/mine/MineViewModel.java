package com.bilibili.lite.ui.mine;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bilibili.lite.data.model.UserInfo;
import com.bilibili.lite.data.repository.UserRepository;

public class MineViewModel extends ViewModel {

    private final UserRepository repo = UserRepository.getInstance();
    public final MutableLiveData<UserInfo> userInfo = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loggedIn = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>();

    public void loadUserInfo() {
        repo.getNavInfo(new UserRepository.CallbackImpl<UserInfo>() {
            @Override
            public void onSuccess(UserInfo result) {
                userInfo.setValue(result);
                loggedIn.setValue(true);
            }

            @Override
            public void onError(String err) {
                loggedIn.setValue(false);
                error.setValue(err);
            }
        });
    }
}
