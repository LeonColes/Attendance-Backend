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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

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
     * 获取带分页的课程用户列表
     * 
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 带分页的用户列表
     */
    @Override
    public Map<String, Object> getCourseUsers(String courseId, int page, int size) {
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);
        
        // 获取课程用户关系列表
        List<CourseUser> allCourseUsers = courseUserRepository.findByCourseIdAndActiveTrue(courseId);
        
        // 按加入时间降序排序
        allCourseUsers.sort((a, b) -> b.getJoinedAt().compareTo(a.getJoinedAt()));
        
        // 手动分页（由于没有直接的分页方法）
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allCourseUsers.size());
        
        List<CourseUser> paginatedCourseUsers = start < end 
            ? allCourseUsers.subList(start, end) 
            : new ArrayList<>();
            
        // 转换为DTO
        List<UserDTO> userDTOs = paginatedCourseUsers.stream()
            .map(courseUser -> {
                String userId = courseUser.getUserId();
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在: " + userId));
                return convertToDTO(user);
            })
            .collect(Collectors.toList());
        
        // 创建分页结果
        Map<String, Object> response = new HashMap<>();
        response.put("users", userDTOs);
        response.put("currentPage", page);
        response.put("totalItems", allCourseUsers.size());
        response.put("totalPages", (int) Math.ceil((double) allCourseUsers.size() / size));
        
        return response;
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