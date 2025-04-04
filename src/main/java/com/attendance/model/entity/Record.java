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
 * 考勤记录实体类
 * 记录用户的签到签退信息
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_records")
@EntityListeners(AuditingEntityListener.class)
public class Record extends BaseEntity {

    /**
     * 签到用户
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 关联的任务
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    /**
     * 签到时间
     */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    /**
     * 签到位置
     */
    @Column(name = "check_in_location", length = 255)
    private String checkInLocation;
    
    /**
     * 签退时间
     */
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    /**
     * 签退位置
     */
    @Column(name = "check_out_location", length = 255)
    private String checkOutLocation;
    
    /**
     * 签到类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_type", nullable = false)
    private Task.CheckInType checkInType;
    
    /**
     * 签到状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecordStatus status = RecordStatus.PENDING;
    
    /**
     * IP地址
     */
    @Column(length = 50)
    private String ipAddress;
    
    /**
     * 设备信息
     */
    @Column(length = 255)
    private String deviceInfo;
    
    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;
    
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
     * 签到状态枚举
     */
    public enum RecordStatus {
        PENDING,     // 待签到
        NORMAL,      // 正常
        LATE,        // 迟到
        EARLY_LEAVE, // 早退
        ABSENT,      // 缺席
        LEAVE,       // 请假
        APPROVED,    // 已批准（异常情况批准）
        REJECTED     // 已拒绝（异常情况拒绝）
    }
} 