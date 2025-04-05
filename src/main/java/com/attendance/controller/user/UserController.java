package com.attendance.controller.user;

import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.model.dto.user.UpdateUserRequest;
import com.attendance.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    @GetMapping("/current")
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
     * 更新用户信息(只能更新自己的信息)
     * 
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/me")
    public ApiResponse<UserDTO> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        log.info("更新当前用户信息: {}", request);
        UserDTO updatedUser = userService.updateCurrentUser(request);
        return ApiResponse.success("用户信息更新成功", updatedUser);
    }
    
    /**
     * 获取课程成员列表
     * 
     * @param courseId 课程ID
     * @return 用户列表
     */
    @GetMapping("/course/{courseId}")
    public ApiResponse<List<UserDTO>> getCourseUsers(@PathVariable String courseId) {
        log.info("获取课程成员列表: courseId={}", courseId);
        List<UserDTO> users = userService.getCourseUsers(courseId);
        return ApiResponse.success(users);
    }
} 