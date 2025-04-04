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
 * 考勤任务实体类
 * 用于管理考勤发布信息
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_tasks")
@EntityListeners(AuditingEntityListener.class)
public class Task extends BaseEntity {

    /**
     * 任务标题
     */
    @Column(nullable = false, length = 100)
    private String title;
    
    /**
     * 任务描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 签到开始时间
     */
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    /**
     * 签到结束时间
     */
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.CREATED;
    
    /**
     * 创建人，多对一关系
     */
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    /**
     * 位置要求（如GPS坐标范围、地址等）
     */
    @Column(length = 255)
    private String locationRequirement;
    
    /**
     * 签到类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInType checkInType;
    
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
     * 任务状态枚举
     */
    public enum TaskStatus {
        CREATED,    // 已创建
        ACTIVE,     // 进行中
        COMPLETED,  // 已完成
        CANCELLED   // 已取消
    }
    
    /**
     * 签到类型枚举
     */
    public enum CheckInType {
        GPS,        // GPS定位签到
        WIFI,       // WiFi签到
        QR_CODE,    // 扫码签到
        MANUAL,     // 手动签到（管理员录入）
        AUTOMATIC,  // 自动签到（系统自动）
        LOCATION    // 位置签到（与GPS类似但处理方式不同）
    }
} 