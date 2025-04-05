package com.attendance.model.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程/签到任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    
    /**
     * 课程/任务ID
     */
    private String id;
    
    /**
     * 课程/任务名称
     */
    private String name;
    
    /**
     * 课程/任务描述
     */
    private String description;
    
    /**
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 创建者用户名（冗余字段，便于前端展示）
     */
    private String creatorUsername;
    
    /**
     * 创建者全名（冗余字段，便于前端展示）
     */
    private String creatorFullName;
    
    /**
     * 课程邀请码 (仅课程类型有效)
     */
    private String code;
    
    /**
     * 开始日期 (课程类型)
     */
    private LocalDate startDate;
    
    /**
     * 结束日期 (课程类型)
     */
    private LocalDate endDate;
    
    /**
     * 类型 (COURSE-普通课程, CHECKIN-签到任务)
     */
    private String type;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 成员数量 (仅课程类型有效)
     */
    private Integer memberCount;
    
    /**
     * 签到开始时间 (仅签到任务类型有效)
     */
    private LocalDateTime checkinStartTime;
    
    /**
     * 签到结束时间 (仅签到任务类型有效)
     */
    private LocalDateTime checkinEndTime;
    
    /**
     * 签到类型 (仅签到任务类型有效)
     * (QR_CODE, LOCATION, WIFI, MANUAL)
     */
    private String checkinType;
    
    /**
     * 验证参数 (仅签到任务类型有效)
     * JSON格式，根据签到类型存储相关信息
     */
    private String verifyParams;
    
    /**
     * 所属课程ID (仅签到任务类型有效)
     */
    private String parentCourseId;
    
    /**
     * 所属课程名称 (仅签到任务类型有效)
     */
    private String parentCourseName;
    
    /**
     * 签到状态 (学生查看课程列表时显示)
     */
    private String attendanceStatus;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 