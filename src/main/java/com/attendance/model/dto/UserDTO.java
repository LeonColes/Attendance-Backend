package com.attendance.model.dto;

import com.attendance.model.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    /**
     * 全名
     */
    @Size(max = 100, message = "全名长度不能超过100个字符")
    private String fullName;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 用户角色
     */
    private User.Role role;
    
    /**
     * 用户状态
     */
    private Boolean enabled;
    
    /**
     * 密码 - 只用于写入，不返回给前端
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 实体转DTO
     */
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    /**
     * DTO转实体
     */
    public User toEntity() {
        User user = User.builder()
                .username(this.username)
                .password(this.password)
                .fullName(this.fullName)
                .email(this.email)
                .role(this.role != null ? this.role : User.Role.STUDENT)
                .enabled(this.enabled != null ? this.enabled : true)
                .build();
        
        if (this.id != null) {
            user.setId(this.id);
        }
        
        return user;
    }
}