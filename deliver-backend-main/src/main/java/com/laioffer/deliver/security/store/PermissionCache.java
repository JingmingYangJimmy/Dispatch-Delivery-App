package com.laioffer.deliver.security.store;

import java.util.List;

public interface PermissionCache {
    java.util.List<String> getPermissions(long userId);
    void invalidate(long userId);
}
