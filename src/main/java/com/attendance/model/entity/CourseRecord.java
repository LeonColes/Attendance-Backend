package com.attendance.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 课程签到记录实体
 */
@Entity
@Table(name = "course_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@EntityListeners(AuditingEntityListener.class)
public class CourseRecord extends BaseEntity {

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
    @Column(nullable = false)
    private String userId;

    /**
     * 签到任务ID（对应Course表中的签到任务）
     */
    @Column(nullable = false)
    private String courseId;

    /**
     * 父课程ID（对应主课程ID）
     */
    private String parentCourseId;

    /**
     * 签到状态：
     * - NORMAL：正常签到
     * - LATE：迟到
     * - LEAVE：请假
     * - ABSENT：缺席
     */
    @Column(nullable = false)
    private String status;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 签到位置（经纬度或位置描述）
     */
    private String location;

    /**
     * 设备信息（设备类型、浏览器信息等）
     */
    private String device;

    /**
     * 验证方式：
     * - CODE：签到码
     * - QR：二维码
     * - LOCATION：位置
     * - BLUETOOTH：蓝牙
     * - NFC：NFC
     * - MANUAL：手动
     */
    private String verifyMethod;

    /**
     * 备注信息
     */
    @Column(length = 500)
    private String remark;

    /**
     * 是否有效
     */
    @Builder.Default
    private boolean active = true;

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