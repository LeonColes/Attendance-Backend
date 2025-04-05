package com.attendance.model.dto.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 签到记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDTO {
    
    /**
     * 记录ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名（冗余字段，便于前端展示）
     */
    private String username;
    
    /**
     * 用户全名（冗余字段，便于前端展示）
     */
    private String userFullName;
    
    /**
     * 签到任务ID (关联Course表，type=CHECKIN的记录)
     */
    private String courseId;
    
    /**
     * 签到任务名称（冗余字段，便于前端展示）
     */
    private String courseName;
    
    /**
     * 所属课程ID (签到任务所属的课程ID)
     */
    private String parentCourseId;
    
    /**
     * 所属课程名称（冗余字段，便于前端展示）
     */
    private String parentCourseName;
    
    /**
     * 签到状态
     */
    private String status;
    
    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;
    
    /**
     * 签到位置
     */
    private String location;
    
    /**
     * 设备信息
     */
    private String device;
    
    /**
     * 验证方式
     */
    private String verifyMethod;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 