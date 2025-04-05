package com.attendance.service.auth;

import com.attendance.model.dto.auth.LoginRequest;
import com.attendance.model.dto.auth.LoginResponse;
import com.attendance.model.dto.auth.RegisterRequest;
import com.attendance.model.dto.user.UserDTO;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 用户信息
     */
    UserDTO register(RegisterRequest registerRequest);
} 