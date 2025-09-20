// com/laioffer/deliver/security/store/impl/SidBlacklistStoreSpring.java
package com.laioffer.deliver.security.store.impl;

import com.laioffer.deliver.security.store.SidBlacklistStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class SidBlacklistStoreSpring implements SidBlacklistStore {
    private final Cache cache;

    public SidBlacklistStoreSpring(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("sidBlacklistCache");
    }

    @Override
    public void revokeTemporarily(String sid, long ttlSeconds) {
        // Spring Cache 不支持每条不同 TTL；已在 CacheConfig 里用统一 expireAfterWrite 覆盖窗口
        cache.put(sid, Boolean.TRUE);
    }

    @Override
    public boolean isRevoked(String sid) {
        Boolean v = cache.get(sid, Boolean.class);
        return Boolean.TRUE.equals(v);
    }
}
