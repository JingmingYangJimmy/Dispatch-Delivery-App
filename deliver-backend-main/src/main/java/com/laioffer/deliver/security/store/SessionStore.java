package com.laioffer.deliver.security.store;

import java.time.Instant;

public interface SessionStore {
    void createSession(long userId, String sid, String refreshPlain, Instant expiresAt, String deviceInfo);
    void rotateSession(String oldSid, String newSid, String newRefreshPlain, Instant newExpiresAt);
    void revokeBySid(String sid);
    void revokeAll(long userId);
    boolean isRefreshValid(long userId, String sid, String refreshPlain);
}
