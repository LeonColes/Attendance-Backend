package com.attendance.repository.course;

import com.attendance.model.entity.CourseRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 课程签到记录数据访问接口
 */
@Repository
public interface CourseRecordRepository extends JpaRepository<CourseRecord, String> {
    
    /**
     * 根据用户ID查找签到记录
     * 
     * @param userId 用户ID
     * @return 签到记录列表
     */
    List<CourseRecord> findByUserId(String userId);
    
    /**
     * 根据签到任务ID查找签到记录
     * 
     * @param courseId 签到任务ID
     * @return 签到记录列表
     */
    List<CourseRecord> findByCourseId(String courseId);
    
    /**
     * 根据用户ID和签到任务ID查找签到记录
     * 
     * @param userId 用户ID
     * @param courseId 签到任务ID
     * @return 签到记录（可选）
     */
    Optional<CourseRecord> findByUserIdAndCourseId(String userId, String courseId);
    
    /**
     * 通过签到任务ID查找所有签到记录（分页）
     * 
     * @param courseId 签到任务ID
     * @param pageable 分页参数
     * @return 签到记录分页结果
     */
    Page<CourseRecord> findByCourseId(String courseId, Pageable pageable);
    
    /**
     * 通过用户ID查找所有签到记录（分页）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 签到记录分页结果
     */
    Page<CourseRecord> findByUserId(String userId, Pageable pageable);
    
    /**
     * 通过签到任务ID和签到状态查找签到记录
     * 
     * @param courseId 签到任务ID
     * @param status 签到状态
     * @return 签到记录列表
     */
    List<CourseRecord> findByCourseIdAndStatus(String courseId, String status);
    
    /**
     * 统计签到任务的签到状态数量
     * 
     * @param courseId 签到任务ID
     * @return 各状态的签到数量
     */
    @Query("SELECT r.status, COUNT(r) FROM CourseRecord r WHERE r.courseId = :courseId GROUP BY r.status")
    List<Object[]> countByCourseIdGroupByStatus(@Param("courseId") String courseId);
    
    /**
     * 查找特定课程下的所有签到记录
     * 
     * @param parentCourseId 所属课程ID
     * @return 签到记录列表
     */
    List<CourseRecord> findByParentCourseId(String parentCourseId);
    
    /**
     * 通过所属课程ID查找所有签到记录（分页）
     * 
     * @param parentCourseId 所属课程ID
     * @param pageable 分页参数
     * @return 签到记录分页结果
     */
    Page<CourseRecord> findByParentCourseId(String parentCourseId, Pageable pageable);
    
    /**
     * 统计特定状态的签到记录数量
     * 
     * @param courseId 签到任务ID
     * @param status 签到状态
     * @return 记录数量
     */
    long countByCourseIdAndStatus(String courseId, String status);
    
    /**
     * 统计特定课程的签到记录数
     * 
     * @param courseId 签到任务ID
     * @return 签到记录数
     */
    long countByCourseId(String courseId);
    
    /**
     * 删除指定签到任务的所有签到记录
     * 
     * @param courseId 签到任务ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CourseRecord cr WHERE cr.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") String courseId);
} 