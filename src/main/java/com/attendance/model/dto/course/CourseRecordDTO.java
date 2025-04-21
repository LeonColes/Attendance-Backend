package com.attendance.model.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程签到记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecordDTO {

    /**
     * 记录ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户姓名
     */
    private String fullName;
    
    /**
     * 签到任务ID
     */
    private String courseId;
    
    /**
     * 签到任务名称
     */
    private String courseName;
    
    /**
     * 父课程ID
     */
    private String parentCourseId;
    
    /**
     * 父课程名称
     */
    private String parentCourseName;
    
    /**
     * 签到状态
     */
    private String status;
    
    /**
     * 签到时间
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
     * 备注
     */
    private String remark;
    
    /**
     * 是否有效
     */
    private boolean active;
    
    /**
     * 创建时间
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}