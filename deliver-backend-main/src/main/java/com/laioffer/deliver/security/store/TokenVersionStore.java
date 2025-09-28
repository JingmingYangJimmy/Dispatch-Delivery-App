package com.laioffer.deliver.security.store;

public interface TokenVersionStore {
    long getCurrentVersion(long userId);
    void bumpVersion(long userId);
    void invalidate(long userId);
}
