package com.attendance.service.course;

import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.dto.course.CourseRecordDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * 获取当前用户的课程列表
     * 教师只能看到自己创建的课程
     * 学生只能看到自己加入的课程
     * 管理员可以看到所有课程
     * 
     * @return 课程列表
     */
    List<CourseDTO> getMyCourses();
    
    /**
     * 获取当前用户的课程列表（带分页）
     * 教师只能看到自己创建的课程
     * 学生只能看到自己加入的课程
     * 管理员可以看到所有课程
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 分页课程列表
     */
    Map<String, Object> getMyCourses(int page, int size);
    
    /**
     * 获取课程下的所有签到任务
     * 
     * @param courseId 课程ID
     * @return 签到任务列表
     */
    List<CourseDTO> getCourseCheckinTasks(String courseId);
    
    /**
     * 获取课程下的所有签到任务（带分页）
     * 
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 签到任务分页列表
     */
    Map<String, Object> getCourseCheckinTasks(String courseId, int page, int size);
    
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
     * 移除课程成员
     *
     * @param courseId 课程ID
     * @param userId 用户ID
     * @param reason 移除原因
     * @return 是否成功
     */
    boolean removeCourseMember(String courseId, String userId, String reason);
    
    /**
     * 获取签到统计信息
     *
     * @param checkinId 签到任务ID
     * @return 签到统计信息
     */
    Map<String, Object> getCheckinStatistics(String checkinId);
    
    /**
     * 提交签到
     *
     * @param taskId 签到任务ID
     * @param verifyData 验证数据
     * @param verifyMethod 验证方式
     * @param location 位置信息
     * @param device 设备信息
     * @return 签到结果
     */
    Map<String, Object> submitCheckin(String taskId, String verifyData, String verifyMethod, String location, String device);
    
    /**
     * 学生提交签到（新版接口）
     * 
     * @param courseId 签到任务ID
     * @param verifyMethod 验证方式
     * @param location 位置信息
     * @param device 设备信息
     * @param verifyData 验证数据
     * @return 签到记录
     */
    CourseRecordDTO submitCheckIn(String courseId, String verifyMethod, String location, String device, String verifyData);
    
    /**
     * 获取用户在课程中的所有签到记录
     *
     * @param courseId 课程ID
     * @param userId 用户ID（不传则查当前用户）
     * @param page 页码
     * @param size 每页大小
     * @return 签到记录分页数据
     */
    Map<String, Object> getUserCourseRecords(String courseId, String userId, int page, int size);
    
    /**
     * 获取签到任务详情（简化版）
     *
     * @param checkinId 签到任务ID
     * @param page 页码
     * @param size 每页大小
     * @return 签到任务详情
     */
    Map<String, Object> getCheckinDetail(String checkinId, int page, int size);
    
    /**
     * 获取课程的所有签到统计（带分页）
     *
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 签到统计分页数据
     */
    Map<String, Object> getCourseAttendanceStats(String courseId, int page, int size);
    
    /**
     * 获取签到任务列表
     *
     * @param courseId 父课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 签到任务列表
     */
    Map<String, Object> getAttendanceList(String courseId, int page, int size);
    
    /**
     * 获取课程签到统计信息
     *
     * @param courseId 课程ID
     * @return 课程签到统计信息
     */
    Map<String, Object> getCourseAttendanceDetail(String courseId);
    
    /**
     * 删除课程（逻辑删除）
     * 只有课程创建者或管理员可以删除课程
     * 删除课程会同时标记该课程下的所有签到任务和签到记录为已删除
     *
     * @param courseId 课程ID
     * @return 是否删除成功
     */
    boolean deleteCourse(String courseId);
    
    /**
     * 删除签到任务（逻辑删除）
     * 只有签到任务创建者或管理员可以删除签到任务
     * 删除签到任务会同时标记该任务下的所有签到记录为已删除
     *
     * @param checkinId 签到任务ID
     * @return 是否删除成功
     */
    boolean deleteCheckinTask(String checkinId);
    
    /**
     * 更新课程信息
     * 只有课程创建者或管理员可以更新课程
     *
     * @param courseId 课程ID
     * @param name 课程名称
     * @param description 课程描述
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 更新后的课程
     */
    CourseDTO updateCourse(String courseId, String name, String description, LocalDate startDate, LocalDate endDate);
} 