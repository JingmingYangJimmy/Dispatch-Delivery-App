package com.laioffer.deliver.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_roles")
public record UserRoleEntity(
        @Id Long id,
        @Column("user_id") Long userId,
        @Column("role_id") Long roleId,
        @Column("hub_id")  Long hubId   // 可为 null
) {}
