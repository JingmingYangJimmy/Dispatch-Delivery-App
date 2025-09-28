package com.laioffer.deliver.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("users")
public record UserEntity(
        @Id Long id,
        String email,
        String phone,
        String password,
        String firstName,
        String lastName,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
