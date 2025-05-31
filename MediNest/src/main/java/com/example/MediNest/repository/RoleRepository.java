package com.example.MediNest.repository;

import com.example.MediNest.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findAllByRoleIdIn(List<String> rolesFromModel);
}
