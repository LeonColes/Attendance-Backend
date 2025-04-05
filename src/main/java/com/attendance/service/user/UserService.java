package com.attendance.service.user;

import com.attendance.model.dto.user.UserDTO;
import com.attendance.model.dto.user.UpdateUserRequest;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 获取当前用户信息
     * 
     * @return 用户信息
     */
    UserDTO getCurrentUser();
    
    /**
     * 获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUser(String id);
    
    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    List<UserDTO> getAllUsers();
    
    /**
     * 更新当前用户信息
     * 
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserDTO updateCurrentUser(UpdateUserRequest request);
    
    /**
     * 获取课程用户列表
     * 
     * @param courseId 课程ID
     * @return 用户列表
     */
    List<UserDTO> getCourseUsers(String courseId);
    
    /**
     * 获取课程下的所有用户（带分页）
     * 
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页用户列表
     */
    Map<String, Object> getCourseUsers(String courseId, int page, int size);
} 