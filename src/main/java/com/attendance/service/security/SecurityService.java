package com.attendance.service.security;

/**
 * 统一安全服务接口
 * 集中处理用户权限检查相关功能
 */
public interface SecurityService {
    
    /**
     * 检查是否为当前用户
     * 
     * @param userId 用户ID
     * @return 是否为当前用户
     */
    boolean isCurrentUser(String userId);
    
    /**
     * 检查是否可以访问课程任务
     * 
     * @param courseId 课程或签到任务ID
     * @return 是否可以访问课程任务
     */
    boolean canAccessCourse(String courseId);
    
    /**
     * 检查是否可以访问签到记录
     * 
     * @param recordId 记录ID
     * @return 是否可以访问签到记录
     */
    boolean canAccessRecord(String recordId);
    
    /**
     * 检查当前用户是否为课程创建者
     *
     * @param courseId 课程ID
     * @return 是否为课程创建者
     */
    boolean isCourseCreator(String courseId);
    
    /**
     * 检查当前用户是否为课程管理员(创建者或助教)
     *
     * @param courseId 课程ID
     * @return 是否为课程管理员
     */
    boolean isCourseAdmin(String courseId);
    
    /**
     * 检查当前用户是否为课程成员
     *
     * @param courseId 课程ID
     * @return 是否为课程成员
     */
    boolean isCourseMember(String courseId);
    
    /**
     * 检查用户是否可以加入课程
     *
     * @param courseId 课程ID
     * @return 是否可以加入课程
     */
    boolean canJoinCourse(String courseId);
} 