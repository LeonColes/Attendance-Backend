package com.attendance.service.user;

import com.attendance.model.dto.user.UserDTO;

import java.util.List;

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
} 