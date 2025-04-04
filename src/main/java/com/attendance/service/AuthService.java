package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.dto.LoginDTO;
import com.attendance.model.dto.UserDTO;
import com.attendance.model.entity.User;
import com.attendance.repository.UserRepository;
import com.attendance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 检查是否存在其他系统管理员（除了指定ID的用户）
     * 
     * @param userId 当前用户ID
     * @return 如果存在其他系统管理员则返回true
     */
    public boolean hasOtherSystemAdmin(String userId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(userId)) // 排除当前用户
                .anyMatch(user -> user.getRole() == User.Role.SYSTEM_ADMIN);
    }
    
    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息
     * @return 登录结果（包含token和用户信息）
     */
    public Map<String, Object> login(LoginDTO loginDTO) {
        log.info("用户登录: {}", loginDTO.getUsername());
        
        // 检查用户是否存在
        if (!userRepository.existsByUsername(loginDTO.getUsername())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 进行身份验证
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        
        // 设置安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 生成JWT令牌
        String token = jwtTokenProvider.generateToken(authentication);
        
        // 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        
        log.info("用户登录成功: {}", loginDTO.getUsername());
        return result;
    }
    
    /**
     * 用户注册
     * 
     * @param userDTO 用户信息
     * @return 注册成功的用户信息
     */
    @Transactional
    public UserDTO register(UserDTO userDTO) {
        log.info("新用户注册: {}, 角色: {}", userDTO.getUsername(), userDTO.getRole());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 如果是系统管理员角色，检查是否已存在系统管理员
        if (userDTO.getRole() == User.Role.SYSTEM_ADMIN && hasSystemAdmin()) {
            throw new BusinessException(400, "系统已存在系统管理员账户，不允许创建多个");
        }
        
        // 创建新用户
        User user = userDTO.toEntity();
        
        // 密码加密
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        
        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}, ID: {}, 角色: {}", 
                savedUser.getUsername(), savedUser.getId(), savedUser.getRole());
        
        return UserDTO.fromEntity(savedUser);
    }
    
    /**
     * 获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    public UserDTO getUserById(String id) {
        log.info("获取用户信息: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        return UserDTO.fromEntity(user);
    }
    
    /**
     * 更新用户角色
     * 
     * @param id 用户ID
     * @param role 角色
     * @return 更新后的用户信息
     */
    @Transactional
    public UserDTO updateUserRole(String id, User.Role role) {
        log.info("更新用户角色: {}, 角色: {}", id, role);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 如果要更新为系统管理员，需要检查是否已存在其他系统管理员
        if (role == User.Role.SYSTEM_ADMIN && hasOtherSystemAdmin(id)) {
            throw new BusinessException(400, "系统已存在系统管理员，不允许创建多个");
        }
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        
        log.info("用户角色更新成功: {}, 新角色: {}", id, role);
        return UserDTO.fromEntity(updatedUser);
    }
    
    /**
     * 检查是否存在系统管理员
     * 
     * @return 如果存在系统管理员则返回true
     */
    private boolean hasSystemAdmin() {
        return userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == User.Role.SYSTEM_ADMIN);
    }
}