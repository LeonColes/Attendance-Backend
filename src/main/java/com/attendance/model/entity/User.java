package com.attendance.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 存储用户信息并管理角色
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity {

    /**
     * 用户名，唯一
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码，加密存储
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 用户全名
     */
    @Column(length = 100)
    private String fullName;

    /**
     * 电子邮箱
     */
    @Column(length = 100)
    private String email;

    /**
     * 用户角色
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.STUDENT;

    /**
     * 用户状态：true-启用，false-禁用
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 用户角色枚举
     * 简化为三种角色：学生、教师和系统管理员
     */
    public enum Role {
        STUDENT,      // 学生用户，可以参与考勤
        TEACHER,      // 教师用户，可以管理学生和考勤任务
        SYSTEM_ADMIN  // 系统管理员，拥有所有权限（应当只有一个）
    }
    
    /**
     * 检查用户是否为教师或以上权限
     */
    public boolean isTeacherOrAdmin() {
        return this.role == Role.TEACHER || this.role == Role.SYSTEM_ADMIN;
    }
    
    /**
     * 检查用户是否为系统管理员
     */
    public boolean isSystemAdmin() {
        return this.role == Role.SYSTEM_ADMIN;
    }
}