package com.attendance.service.security;

/**
 * 安全服务接口
 * 用于自定义权限检查
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
     * @param courseId 课程或任务ID
     * @return 是否可以访问课程任务
     */
    boolean canAccessTask(String courseId);
    
    /**
     * 检查是否可以访问签到记录
     * 
     * @param recordId 记录ID
     * @return 是否可以访问签到记录
     */
    boolean canAccessRecord(String recordId);
} 