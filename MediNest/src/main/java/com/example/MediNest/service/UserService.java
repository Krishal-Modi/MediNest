package com.example.MediNest.service;

import com.example.MediNest.entity.Role;
import com.example.MediNest.entity.User;
import com.example.MediNest.entity.UserRole;
import com.example.MediNest.exceptions.DataNotFoundException;
import com.example.MediNest.exceptions.DataValidationException;
import com.example.MediNest.mapper.RoleMapper;
import com.example.MediNest.mapper.UserMapper;
import com.example.MediNest.model.RoleModel;
import com.example.MediNest.model.UserModel;
import com.example.MediNest.repository.RoleRepository;
import com.example.MediNest.repository.UserRepository;
import com.example.MediNest.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final RoleMapper roleMapper;

    private final PasswordEncoder passwordEncoder;

    public UserModel signUp(UserModel userModel) {

        User adduser = userMapper.userModelToUser(userModel);
        adduser.setPassword(passwordEncoder.encode(userModel.getPassword()));
        // Save User To get Id
        userRepository.save(adduser);

        // Extract Roles From Model
        List<String> rolesFromModel = userModel.getRoles().stream().map(r -> r.getRoleId()).toList();

        // Find Roles in DB
        List<Role> roleInDb = roleRepository.findAllByRoleIdIn(rolesFromModel);

        // Check If Roles are Valid
        List<String> rolesIdsInDb = roleInDb.stream().map(r -> r.getRoleId()).toList();

        List<String> invalidRoles = new ArrayList<>();

        // Checking for invalid Roles
        for (String roleId : rolesFromModel) {
            if (!rolesIdsInDb.contains(roleId)) {
                invalidRoles.add(roleId.toString());
            }
        }

        if (!invalidRoles.isEmpty()) {
            throw new DataValidationException("Invalid roles: " + invalidRoles);
        }

        // Filter Valid Roles
        List<Role> saveRoles = roleInDb.stream().filter(r -> rolesFromModel.contains(r.getRoleId())).toList();

        // Save User Roles
        for (Role role : saveRoles) {
            UserRole userRole = new UserRole();
            userRole.setUser(adduser);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        // Return User
        UserModel userModelToReturn = userMapper.userToUserModel(adduser);
        List<UserRole> byUserUserId = userRoleRepository.findByUserUserId(adduser.getUserId());
        List<RoleModel> roleList =new ArrayList<>();
        byUserUserId.forEach(ur -> roleList.add(roleMapper.roleToRoleModel(ur.getRole())));
        userModelToReturn.setRoles(roleList);
        return userModelToReturn;

    }

    public void deleteUser(String userId) {
        User user = userRepository.findByEmail(userId);
        userRepository.delete(user);
    }

    public List<UserModel> getAllUsers(String search) {
        List<User> userList = userRepository.searchUsers(search);

        List<UserModel> userModelList = userList.stream().map(user -> {
            UserModel userModel = userMapper.userToUserModel(user);
            List<UserRole> userRoles = userRoleRepository.findByUserUserId(user.getUserId());
            List<RoleModel> roleModelList = userRoles.stream().map(userRole -> roleMapper.roleToRoleModel(userRole.getRole())).toList();
            userModel.setRoles(roleModelList);
            return userModel;
        }).toList();
        return userModelList;
    }


    @Transactional
    public UserModel updateProfile(String email, UserModel userModel) {
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            throw new DataValidationException("User not found for email: " + email);
        }

        userMapper.updateUsersModel(userModel, existingUser);

        if (userModel.getPassword() != null && !userModel.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userModel.getPassword()));
        }

        User savedUser = userRepository.save(existingUser);

        List<String> incomingRoleIdsFromModel = userModel.getRoles().stream()
                .map(r -> r.getRoleId()).distinct().toList();

        List<Role> roleInDb = roleRepository.findAllByRoleIdIn(incomingRoleIdsFromModel);

        List<String> roleIdsInDb = roleInDb.stream()
                .map(r -> r.getRoleId()).distinct().toList();

        List<UserRole> existingRoles = userRoleRepository.findByUserUserId(savedUser.getUserId());
        List<String> existingRoleIds = existingRoles.stream()
                .map(r -> r.getRole().getRoleId()).distinct().toList();

        List<String> removeRoleIds = new ArrayList<>();

        for (String roleId : existingRoleIds) {
            if (!incomingRoleIdsFromModel.contains(roleId)) {
                removeRoleIds.add(roleId);
            }
        }

        if (!removeRoleIds.isEmpty()) {
            userRoleRepository.deleteByRoleRoleIdInAndUserUserId(removeRoleIds, savedUser.getUserId());
        }

        List<String> nonAllocateRoleIds = incomingRoleIdsFromModel.stream()
                .filter(roleId -> !existingRoleIds.contains(roleId))
                .toList();

        List<String> invalidRoles = new ArrayList<>();
        if (!nonAllocateRoleIds.isEmpty()) {
            for (String roleId : nonAllocateRoleIds) {
                if (!roleIdsInDb.contains(roleId)) {
                    invalidRoles.add(roleId);
                }
            }
        }

        if (!invalidRoles.isEmpty()) {
            throw new DataValidationException("Invalid roles: " + invalidRoles);
        }

        List<Role> updatedRoles = roleInDb.stream()
                .filter(r -> nonAllocateRoleIds.contains(r.getRoleId()))
                .toList();

        for (Role role : updatedRoles) {
            UserRole userRole = new UserRole();
            userRole.setUser(savedUser);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        UserModel updatedUserModel = userMapper.userToUserModel(savedUser);

        List<UserRole> updatedUserRoles = userRoleRepository.findByUserUserId(savedUser.getUserId());

        List<RoleModel> updatedRoleModels = updatedUserRoles.stream()
                .map(r -> roleMapper.roleToRoleModel(r.getRole())).toList();
        updatedUserModel.setRoles(updatedRoleModels);
        return updatedUserModel;
    }
}
