package com.attendance.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 签到记录实体类
 */
@Data
@Entity
@Table(name = "records")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Record {

    /**
     * 记录ID (UUID)
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * 签到任务ID (关联Course表，type=CHECKIN的记录)
     */
    @Column(name = "course_id", nullable = false)
    private String courseId;
    
    /**
     * 所属课程ID (任务所属的课程ID)
     */
    @Column(name = "parent_course_id")
    private String parentCourseId;

    /**
     * 签到状态 (NORMAL, LATE, ABSENT)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 签到时间
     */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 签到位置 (经纬度JSON格式)
     */
    @Column(length = 255)
    private String location;

    /**
     * 设备信息
     */
    @Column(length = 255)
    private String device;

    /**
     * 验证方式 (QR_CODE, LOCATION, WIFI, MANUAL)
     */
    @Column(name = "verify_method", length = 20)
    private String verifyMethod;

    /**
     * 验证数据 (根据验证方式不同而不同)
     */
    @Column(name = "verify_data", columnDefinition = "TEXT")
    private String verifyData;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 