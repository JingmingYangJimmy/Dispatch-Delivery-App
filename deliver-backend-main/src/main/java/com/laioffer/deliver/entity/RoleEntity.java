package com.laioffer.deliver.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("roles")
public record RoleEntity(
        @Id Long id,
        String code,
        String name
) {}
