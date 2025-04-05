package com.attendance.service.course;

import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程/签到任务服务接口
 */
public interface CourseService {
    
    /**
     * 获取课程/签到任务信息
     * 
     * @param id 课程/任务ID
     * @return 课程/任务信息
     */
    CourseDTO getCourse(String id);
    
    /**
     * 获取所有课程(普通课程)
     * 
     * @return 课程列表
     */
    List<CourseDTO> getAllCourses();
    
    /**
     * 获取当前用户的课程
     * 
     * @return 课程列表
     */
    List<CourseDTO> getMyCourses();
    
    /**
     * 获取课程下的所有签到任务
     * 
     * @param courseId 课程ID
     * @return 签到任务列表
     */
    List<CourseDTO> getCourseCheckinTasks(String courseId);
    
    /**
     * 创建普通课程
     *
     * @param name 课程名称
     * @param description 课程描述
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 创建的课程
     */
    CourseDTO createCourse(String name, String description, LocalDate startDate, LocalDate endDate);
    
    /**
     * 创建签到任务
     *
     * @param parentCourseId 所属课程ID
     * @param name 任务名称
     * @param description 任务描述
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param checkinType 签到类型
     * @param verifyParams 验证参数
     * @return 创建的签到任务
     */
    CourseDTO createCheckinTask(String parentCourseId, String name, String description, 
                              LocalDateTime startTime, LocalDateTime endTime, 
                              String checkinType, String verifyParams);
    
    /**
     * 更新课程/签到任务状态
     *
     * @param id 课程/任务ID
     * @param status 新状态
     * @return 更新后的课程/任务
     */
    CourseDTO updateCourseStatus(String id, String status);
    
    /**
     * 通过邀请码加入课程
     *
     * @param code 邀请码
     * @return 课程用户关系
     */
    CourseUserDTO joinCourseByCode(String code);
    
    /**
     * 通过二维码加入课程
     *
     * @param qrCode 二维码内容
     * @return 课程用户关系
     */
    CourseUserDTO joinCourseByQRCode(String qrCode);
    
    /**
     * 添加课程成员
     *
     * @param courseId 课程ID
     * @param userIds 用户ID列表
     * @param role 角色
     * @return 成功添加的成员数量
     */
    int addCourseMembers(String courseId, List<String> userIds, String role);
    
    /**
     * 获取课程成员数量
     *
     * @param courseId 课程ID
     * @return 成员数量
     */
    int getCourseMemberCount(String courseId);
    
    /**
     * 提交签到
     *
     * @param checkinId 签到任务ID
     * @param verifyData 验证数据
     * @param location 位置信息
     * @param device 设备信息
     * @return 是否签到成功
     */
    boolean submitCheckin(String checkinId, String verifyData, String location, String device);
    
    /**
     * 生成签到码
     *
     * @param checkinId 签到任务ID
     * @return 签到码
     */
    String generateCheckinCode(String checkinId);
} 