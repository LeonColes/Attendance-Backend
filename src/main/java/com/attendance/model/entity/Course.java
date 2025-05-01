package com.attendance.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程与签到任务实体类
 * 整合了课程和签到任务的功能
 */
@Data
@Entity
@Table(name = "courses")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Course {

    /**
     * 课程ID (UUID)
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    /**
     * 课程名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 课程描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 创建者ID（关联用户）
     */
    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    /**
     * 课程邀请码
     */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    /**
     * 开始日期 (课程开始日期)
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * 结束日期 (课程结束日期)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 课程类型 (COURSE-普通课程, CHECKIN-签到任务)
     */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * 课程状态 (CREATED, ACTIVE, FINISHED, ARCHIVED)
     * 签到任务状态 (CREATED, ACTIVE, COMPLETED, ENDED, CANCELED)
     */
    @Column(nullable = false, length = 20)
    private String status;

    // 以下字段仅在type="CHECKIN"时使用 (签到任务相关字段)
    
    /**
     * 签到开始时间
     */
    @Column(name = "checkin_start_time")
    private LocalDateTime checkinStartTime;

    /**
     * 签到结束时间
     */
    @Column(name = "checkin_end_time")
    private LocalDateTime checkinEndTime;

    /**
     * 签到类型（QR_CODE, LOCATION, WIFI, MANUAL）
     */
    @Column(name = "checkin_type", length = 20)
    private String checkinType;

    /**
     * 验证参数（JSON格式，根据签到类型存储相关信息）
     * QR_CODE: {"code": "唯一码"}
     * LOCATION: {"latitude": 纬度, "longitude": 经度, "radius": 半径}
     * WIFI: {"ssid": "WiFi名称", "bssid": "MAC地址"}
     */
    @Column(name = "verify_params", columnDefinition = "TEXT")
    private String verifyParams;

    /**
     * 所属课程ID (如果是签到任务，则关联到其所属课程)
     */
    @Column(name = "parent_course_id")
    private String parentCourseId;

    /**
     * 是否有效，用于逻辑删除
     * true: 有效，false: 已删除
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

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