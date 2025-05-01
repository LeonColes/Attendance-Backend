package com.attendance.model.dto.course;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程用户关系DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseUserDTO {
    
    /**
     * 关系ID
     */
    private String id;
    
    /**
     * 课程ID
     */
    private String courseId;
    
    /**
     * 课程名称（冗余字段，便于前端展示）
     */
    private String courseName;
    
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
     * 用户在课程中的角色
     */
    private String role;
    
    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;
}