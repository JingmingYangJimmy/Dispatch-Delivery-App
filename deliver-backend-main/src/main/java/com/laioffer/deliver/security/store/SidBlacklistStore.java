package com.laioffer.deliver.security.store;

public interface SidBlacklistStore {
    void revokeTemporarily(String sid, long ttlSeconds);
    boolean isRevoked(String sid);
}
