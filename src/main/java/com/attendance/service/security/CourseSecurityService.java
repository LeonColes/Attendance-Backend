package com.attendance.service.security;

/**
 * 课程安全服务接口
 * 用于课程相关的权限检查
 */
public interface CourseSecurityService {
    
    /**
     * 检查当前用户是否为课程创建者
     * 
     * @param courseId 课程ID
     * @return 是否为课程创建者
     */
    boolean isCourseCreator(String courseId);
    
    /**
     * 检查当前用户是否为签到任务创建者
     * 
     * @param checkinId 签到任务ID
     * @return 是否为签到任务创建者
     */
    boolean isCheckinCreator(String checkinId);
    
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