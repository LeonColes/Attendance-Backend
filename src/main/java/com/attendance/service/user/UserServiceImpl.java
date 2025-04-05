package com.attendance.service.user;

import com.attendance.common.exception.BusinessException;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.model.dto.user.UpdateUserRequest;
import com.attendance.model.entity.User;
import com.attendance.model.entity.CourseUser;
import com.attendance.repository.user.UserRepository;
import com.attendance.repository.course.CourseUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CourseUserRepository courseUserRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 获取当前用户信息
     * 
     * @return 用户信息
     */
    @Override
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        return convertToDTO(user);
    }
    
    /**
     * 获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        return convertToDTO(user);
    }
    
    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新当前用户信息
     * 
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @Override
    @Transactional
    public UserDTO updateCurrentUser(UpdateUserRequest request) {
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 更新基本信息
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        // 更新密码（如果提供了）
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // 保存更新
        User updatedUser = userRepository.save(user);
        
        return convertToDTO(updatedUser);
    }
    
    /**
     * 获取课程用户列表
     * 
     * @param courseId 课程ID
     * @return 用户列表
     */
    @Override
    public List<UserDTO> getCourseUsers(String courseId) {
        List<CourseUser> courseUsers = courseUserRepository.findByCourseId(courseId);
        
        return courseUsers.stream()
                .map(courseUser -> {
                    String userId = courseUser.getUserId();
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException("用户不存在: " + userId));
                    return convertToDTO(user);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 将实体转换为DTO
     * 
     * @param user 用户实体
     * @return 用户DTO
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
} 