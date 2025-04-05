package com.attendance.controller.user;

import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    
    /**
     * 获取当前用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserDTO> getCurrentUser() {
        UserDTO userDTO = userService.getCurrentUser();
        return ApiResponse.success(userDTO);
    }
    
    /**
     * 获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    public ApiResponse<UserDTO> getUser(@PathVariable String id) {
        UserDTO userDTO = userService.getUser(id);
        return ApiResponse.success(userDTO);
    }
    
    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ApiResponse<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }
} 