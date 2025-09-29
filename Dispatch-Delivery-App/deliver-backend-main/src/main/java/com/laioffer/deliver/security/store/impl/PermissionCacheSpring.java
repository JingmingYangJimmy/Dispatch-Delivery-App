// com/laioffer/deliver/security/store/impl/PermissionCacheSpring.java
package com.laioffer.deliver.security.store.impl;

import com.laioffer.deliver.repository.PermissionRepository;
import com.laioffer.deliver.security.store.PermissionCache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionCacheSpring implements PermissionCache {
    private final Cache cache;
    private final PermissionRepository permissionRepository;

    public PermissionCacheSpring(CacheManager cacheManager, PermissionRepository permissionRepository) {
        this.cache = cacheManager.getCache("permissionCache");
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<String> getPermissions(long userId) {
        return cache.get(userId, () -> permissionRepository.findPermissionCodesByUserId(userId));
    }

    @Override
    public void invalidate(long userId) {
        cache.evict(userId);
    }
}
