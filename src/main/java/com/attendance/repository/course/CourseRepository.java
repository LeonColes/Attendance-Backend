package com.attendance.repository.course;

import com.attendance.model.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 课程/签到任务数据访问接口
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    
    /**
     * 根据创建者ID查找
     * 
     * @param creatorId 创建者ID
     * @return 课程/任务列表
     */
    List<Course> findByCreatorId(String creatorId);
    
    /**
     * 根据创建者ID和类型查找
     * 
     * @param creatorId 创建者ID
     * @param type 类型 (COURSE/CHECKIN)
     * @return 课程/任务列表
     */
    List<Course> findByCreatorIdAndType(String creatorId, String type);
    
    /**
     * 根据邀请码查找课程
     * 
     * @param code 邀请码
     * @return 课程
     */
    Optional<Course> findByCode(String code);
    
    /**
     * 查找所有普通课程
     * 
     * @param type 类型 (COURSE)
     * @return 课程列表
     */
    List<Course> findByType(String type);
    
    /**
     * 根据状态和类型查找
     * 
     * @param status 状态
     * @param type 类型
     * @return 课程/任务列表
     */
    List<Course> findByStatusAndType(String status, String type);
    
    /**
     * 查找特定课程下的所有签到任务
     * 
     * @param parentCourseId 父课程ID
     * @return 签到任务列表
     */
    List<Course> findByParentCourseIdAndType(String parentCourseId, String type);
    
    /**
     * 查找当前正在进行的签到任务
     * (开始时间早于当前时间，结束时间晚于当前时间)
     * 
     * @param now 当前时间
     * @param type CHECKIN类型
     * @param status ACTIVE状态
     * @return 活跃中的签到任务
     */
    List<Course> findByCheckinStartTimeBeforeAndCheckinEndTimeAfterAndTypeAndStatus(
            LocalDateTime now, 
            LocalDateTime nowAgain,
            String type,
            String status);
    
    /**
     * 查找指定课程的活跃签到任务数量
     * 
     * @param parentCourseId 父课程ID
     * @param type CHECKIN类型
     * @param status ACTIVE状态
     * @return 活跃签到任务数量
     */
    long countByParentCourseIdAndTypeAndStatus(String parentCourseId, String type, String status);
    
    /**
     * 查找需要自动结束的签到任务
     * (结束时间早于当前时间，状态为ACTIVE)
     * 
     * @param now 当前时间
     * @param type CHECKIN类型
     * @param status ACTIVE状态
     * @return 需要结束的签到任务
     */
    List<Course> findByCheckinEndTimeBeforeAndTypeAndStatus(
            LocalDateTime now, 
            String type,
            String status);
} 