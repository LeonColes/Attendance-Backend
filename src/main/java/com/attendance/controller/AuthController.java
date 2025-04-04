package com.attendance.controller;

import com.attendance.model.common.Result;
import com.attendance.model.dto.LoginDTO;
import com.attendance.model.dto.UserDTO;
import com.attendance.model.entity.User;
import com.attendance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册
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
     * @param loginDTO 登录信息
     * @return 登录结果（含token和用户信息）
     */
    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("接收到登录请求: {}", loginDTO.getUsername());
        Map<String, Object> result = authService.login(loginDTO);
        return ResponseEntity.ok(Result.success("登录成功", result));
    }
    
    /**
     * 学生注册
     * 
     * @param userDTO 用户信息
     * @return 注册结果
     */
    @PostMapping("/register/student")
    public ResponseEntity<Result<UserDTO>> registerStudent(@Valid @RequestBody UserDTO userDTO) {
        log.info("接收到学生注册请求: username={}, fullName={}", 
                userDTO.getUsername(), userDTO.getFullName());
        
        try {
            userDTO.setRole(User.Role.STUDENT);
            log.info("设置用户角色为: {}", User.Role.STUDENT);
            
            UserDTO result = authService.register(userDTO);
            log.info("学生注册成功: id={}, username={}", result.getId(), result.getUsername());
            return ResponseEntity.ok(Result.success("学生注册成功", result));
        } catch (Exception e) {
            log.error("学生注册失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 教师注册
     * 
     * @param userDTO 用户信息
     * @return 注册结果
     */
    @PostMapping("/register/teacher")
    public ResponseEntity<Result<UserDTO>> registerTeacher(@Valid @RequestBody UserDTO userDTO) {
        log.info("接收到教师注册请求: username={}, fullName={}", 
                userDTO.getUsername(), userDTO.getFullName());
        
        try {
            userDTO.setRole(User.Role.TEACHER);
            log.info("设置用户角色为: {}", User.Role.TEACHER);
            
            UserDTO result = authService.register(userDTO);
            log.info("教师注册成功: id={}, username={}", result.getId(), result.getUsername());
            return ResponseEntity.ok(Result.success("教师注册成功", result));
        } catch (Exception e) {
            log.error("教师注册失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}