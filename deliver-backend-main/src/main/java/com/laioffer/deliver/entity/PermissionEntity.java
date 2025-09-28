package com.laioffer.deliver.entity;

import org.springframework.data.relational.core.mapping.Table;

@Table("permissions")
public record PermissionEntity(
        Long id,
        String code,
        String name
) {}
