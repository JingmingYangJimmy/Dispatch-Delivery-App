package com.laioffer.deliver.entity;

import org.springframework.data.relational.core.mapping.Table;

@Table("role_permissions")
public record RolePermissionEntity(
        Long id,
        Long roleId,
        Long permissionId
) {}
