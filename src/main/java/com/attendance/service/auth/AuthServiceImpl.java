package com.attendance.service.auth;

import com.attendance.common.config.security.JwtTokenProvider;
import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.model.dto.auth.LoginRequest;
import com.attendance.model.dto.auth.LoginResponse;
import com.attendance.model.dto.auth.RegisterRequest;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.model.entity.User;
import com.attendance.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        // 设置认证信息到上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 生成JWT令牌
        String jwt = jwtTokenProvider.generateToken(authentication);
        
        // 获取用户信息
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 构建并返回响应
        return LoginResponse.builder()
                .accessToken(jwt)
                .tokenType(SystemConstants.Security.TOKEN_PREFIX.trim())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
    
    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 用户信息
     */
    @Override
    @Transactional
    public UserDTO register(RegisterRequest registerRequest) {
        // 验证用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        
        // 验证邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("邮箱已存在");
        }
        
        // 创建用户实体
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());
        user.setRole(registerRequest.getRole());
        user.setEnabled(true);
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        log.info("用户注册成功: {}", savedUser.getUsername());
        
        // 转换为DTO并返回
        return UserDTO.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .enabled(savedUser.getEnabled())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }
} 