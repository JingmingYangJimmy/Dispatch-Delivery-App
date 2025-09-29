package com.laioffer.deliver.security.store.impl;

import com.laioffer.deliver.repository.UserRepository;
import com.laioffer.deliver.security.store.TokenVersionStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TokenVersionStoreSpring implements TokenVersionStore {
    private final Cache cache;
    private final UserRepository userRepository;

    public TokenVersionStoreSpring(CacheManager cacheManager, UserRepository userRepository) {
        this.cache = cacheManager.getCache("tokenVersionCache");
        this.userRepository = userRepository;
    }

    @Override
    public long getCurrentVersion(long userId) {
        Long v = cache.get(userId, () -> userRepository.findTokenVersionById(userId));
        return v == null ? 1L : v;
    }

    @Transactional
    @Override
    public void bumpVersion(long userId) {
        userRepository.bumpTokenVersion(userId);
        cache.evict(userId); // 立刻让新版本生效
    }

    @Override
    public void invalidate(long userId) {
        cache.evict(userId);
    }
}
