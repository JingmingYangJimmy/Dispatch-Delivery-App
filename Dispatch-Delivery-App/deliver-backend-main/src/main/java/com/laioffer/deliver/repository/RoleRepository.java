package com.laioffer.deliver.repository;

import com.laioffer.deliver.entity.RoleEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<RoleEntity, Long> {
    @Query("SELECT id FROM roles WHERE code = :code")
    Long findIdByCode(String code);

    RoleEntity findByCode(String code);
}
