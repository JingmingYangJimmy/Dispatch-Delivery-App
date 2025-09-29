package com.laioffer.deliver.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends CrudRepository<com.laioffer.deliver.entity.PermissionEntity, Long> {

    @Query("""
           SELECT p.code
           FROM permissions p
           JOIN role_permissions rp ON rp.permission_id = p.id
           JOIN user_roles ur ON ur.role_id = rp.role_id
           WHERE ur.user_id = :userId
           """)
    List<String> findPermissionCodesByUserId(Long userId);

    @Query("""
       SELECT p.code
       FROM permissions p
       JOIN role_permissions rp ON rp.permission_id = p.id
       JOIN roles r ON r.id = rp.role_id
       WHERE r.code = :roleCode
       """)
    List<String> findPermissionCodesByRoleCode(String roleCode);

}
