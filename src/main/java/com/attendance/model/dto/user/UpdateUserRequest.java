package com.attendance.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    /**
     * 姓名
     */
    @Size(max = 100, message = "姓名最大长度为100")
    private String fullName;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最大长度为100")
    private String email;
    
    /**
     * 电话
     */
    @Size(max = 20, message = "电话最大长度为20")
    private String phone;
    
    /**
     * 头像URL
     */
    @Size(max = 255, message = "头像URL最大长度为255")
    private String avatarUrl;
    
    /**
     * 个性签名
     */
    @Size(max = 500, message = "个性签名最大长度为500")
    private String bio;
    
    /**
     * 密码 (可选，如果提供则更新密码)
     */
    @Size(min = 6, max = 100, message = "密码长度应在6-100之间")
    private String password;
} 