package com.attendance.repository.course;

import com.attendance.model.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 根据创建者ID和类型查找活跃课程（未删除的）
     * 
     * @param creatorId 创建者ID
     * @param type 类型 (COURSE/CHECKIN)
     * @param active 是否活跃
     * @return 课程/任务列表
     */
    List<Course> findByCreatorIdAndTypeAndActive(String creatorId, String type, Boolean active);
    
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
     * 查找所有活跃的普通课程（未删除的）
     * 
     * @param type 类型 (COURSE)
     * @param active 是否活跃
     * @return 课程列表
     */
    List<Course> findByTypeAndActive(String type, Boolean active);
    
    /**
     * 根据类型和状态查找
     * 
     * @param type 类型 (COURSE/CHECKIN)
     * @param status 状态
     * @return 课程/任务列表
     */
    List<Course> findByTypeAndStatus(String type, String status);
    
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
    List<Course> findByParentCourseId(String parentCourseId);
    
    /**
     * 查找特定课程下的所有活跃签到任务（未删除的）
     * 
     * @param parentCourseId 父课程ID
     * @param active 是否活跃
     * @return 签到任务列表
     */
    List<Course> findByParentCourseIdAndActive(String parentCourseId, Boolean active);
    
    /**
     * 查找特定课程下的所有指定类型的签到任务
     * 
     * @param parentCourseId 父课程ID
     * @param type 类型
     * @return 签到任务列表
     */
    List<Course> findByParentCourseIdAndType(String parentCourseId, String type);
    
    /**
     * 查找特定课程下指定状态的签到任务
     * 
     * @param parentCourseId 父课程ID
     * @param type 类型 (CHECKIN)
     * @param status 状态
     * @return 签到任务列表
     */
    List<Course> findByParentCourseIdAndTypeAndStatus(String parentCourseId, String type, String status);
    
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
            
    /**
     * 查找需要自动激活的签到任务
     * (开始时间早于当前时间，状态为CREATED)
     * 
     * @param now 当前时间
     * @param type CHECKIN类型
     * @param status CREATED状态
     * @return 需要激活的签到任务
     */
    List<Course> findByCheckinStartTimeBeforeAndTypeAndStatus(
            LocalDateTime now, 
            String type,
            String status);

    /**
     * 根据创建者ID和类型查找（带分页）
     * 
     * @param creatorId 创建者ID
     * @param type 类型 (COURSE/CHECKIN)
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByCreatorIdAndType(String creatorId, String type, Pageable pageable);
    
    /**
     * 根据创建者ID、类型和活跃状态查找（带分页）
     * 
     * @param creatorId 创建者ID
     * @param type 类型 (COURSE/CHECKIN)
     * @param active 是否活跃（未删除）
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByCreatorIdAndTypeAndActive(String creatorId, String type, Boolean active, Pageable pageable);
    
    /**
     * 根据类型查找（带分页）
     * 
     * @param type 类型 (COURSE/CHECKIN)
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByType(String type, Pageable pageable);
    
    /**
     * 根据类型和活跃状态查找（带分页）
     * 
     * @param type 类型 (COURSE/CHECKIN)
     * @param active 是否活跃（未删除）
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByTypeAndActive(String type, Boolean active, Pageable pageable);
    
    /**
     * 根据类型和ID列表查找（带分页）
     * 
     * @param type 类型 (COURSE/CHECKIN)
     * @param ids ID列表
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByTypeAndIdIn(String type, List<String> ids, Pageable pageable);
    
    /**
     * 根据类型、ID列表和活跃状态查找（带分页）
     * 
     * @param type 类型 (COURSE/CHECKIN)
     * @param ids ID列表
     * @param active 是否活跃（未删除）
     * @param pageable 分页参数
     * @return 课程/任务分页列表
     */
    Page<Course> findByTypeAndIdInAndActive(String type, List<String> ids, Boolean active, Pageable pageable);

    /**
     * 查找特定课程下的所有签到任务（带分页）
     * 
     * @param parentCourseId 父课程ID
     * @param type 类型 (CHECKIN)
     * @param pageable 分页参数
     * @return 签到任务分页列表
     */
    Page<Course> findByParentCourseIdAndType(String parentCourseId, String type, Pageable pageable);
    
    /**
     * 查找特定课程下的所有活跃签到任务（带分页）
     * 
     * @param parentCourseId 父课程ID
     * @param type 类型 (CHECKIN)
     * @param active 是否活跃（未删除）
     * @param pageable 分页参数
     * @return 签到任务分页列表
     */
    Page<Course> findByParentCourseIdAndTypeAndActive(String parentCourseId, String type, Boolean active, Pageable pageable);
} 