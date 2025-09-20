package com.laioffer.deliver.repository;

import com.laioffer.deliver.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    UserEntity findByEmail(String email); // 登录/查询可用（可选）

    @org.springframework.data.jdbc.repository.query.Query("SELECT token_version FROM users WHERE id = :userId")
    long findTokenVersionById(@org.springframework.data.repository.query.Param("userId") long userId);

    @org.springframework.data.jdbc.repository.query.Modifying
    @org.springframework.data.jdbc.repository.query.Query("UPDATE users SET token_version = token_version + 1 WHERE id = :userId")
    void bumpTokenVersion(@org.springframework.data.repository.query.Param("userId") long userId);
}
