package com.attendance.controller.auth;

import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.auth.LoginRequest;
import com.attendance.model.dto.auth.LoginResponse;
import com.attendance.model.dto.auth.RegisterRequest;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    
    /**
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求: {}", loginRequest.getUsername());
        LoginResponse response = authService.login(loginRequest);
        return ApiResponse.success(response);
    }
    
    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 用户信息
     */
    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("用户注册请求: {}", registerRequest.getUsername());
        UserDTO user = authService.register(registerRequest);
        return ApiResponse.success("用户注册成功", user);
    }
} 