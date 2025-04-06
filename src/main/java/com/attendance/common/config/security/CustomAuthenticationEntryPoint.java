package com.attendance.common.config.security;

import com.attendance.common.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 自定义认证入口点，处理未登录或token过期的情况
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) 
            throws IOException {
        
        log.error("认证异常: {}", authException.getMessage());
        
        ApiResponse<?> apiResponse;
        int statusCode = HttpStatus.UNAUTHORIZED.value();
        
        // 根据异常类型返回不同的消息
        if (authException instanceof BadCredentialsException) {
            apiResponse = ApiResponse.error(statusCode, "登录失败：用户名或密码不正确，请检查您的输入后重试");
        } else if (authException instanceof DisabledException) {
            apiResponse = ApiResponse.error(statusCode, "账户已被禁用：您的账户当前不可用，请联系管理员获取支持");
        } else if (authException instanceof InsufficientAuthenticationException) {
            String msg = authException.getMessage();
            if (msg != null && msg.contains("expired")) {
                apiResponse = ApiResponse.error(statusCode, "登录已过期：您的会话已超时，请重新登录以继续操作");
            } else {
                apiResponse = ApiResponse.error(statusCode, "身份验证失败：请确保您已登录，或尝试使用正确的API地址，如check-in而非signin");
            }
        } else {
            apiResponse = ApiResponse.error(statusCode, "认证失败：" + authException.getMessage() + "，请检查您的凭据或联系系统管理员");
        }
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
} 