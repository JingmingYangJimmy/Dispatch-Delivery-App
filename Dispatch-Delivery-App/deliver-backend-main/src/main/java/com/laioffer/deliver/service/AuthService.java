package com.laioffer.deliver.service;

import com.laioffer.deliver.model.LoginRequest;
import com.laioffer.deliver.model.RefreshRequest;
import com.laioffer.deliver.model.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest req);
    TokenResponse refresh(RefreshRequest req);
    void logout(String accessSid, String refreshToken);

    void logoutAllForUser(Long userId);

    void logoutAllForCurrentUser();
}
