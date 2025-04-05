package com.attendance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * 课程-用户关联实体
 * 用于表示课程与用户的多对多关系
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "course_users")
public class CourseUser extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;
    
    /**
     * 课程ID
     */
    @Column(name = "course_id", nullable = false)
    private String courseId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    /**
     * 用户在课程中的角色
     * 如：CREATOR（创建者）, ASSISTANT（助教）, STUDENT（学生）
     */
    @Column(name = "role", nullable = false)
    private String role;
    
    /**
     * 加入时间
     */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    /**
     * 加入方式
     * 如：CODE（通过邀请码）, QR_CODE（通过二维码）, ADDED（被管理员添加）
     */
    @Column(name = "join_method")
    private String joinMethod;
    
    /**
     * 是否激活
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;
} 