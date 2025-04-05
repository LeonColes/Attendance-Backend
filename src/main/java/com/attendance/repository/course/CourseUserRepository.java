package com.attendance.repository.course;

import com.attendance.model.entity.CourseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程用户关系数据访问接口
 */
@Repository
public interface CourseUserRepository extends JpaRepository<CourseUser, String> {
    
    /**
     * 根据课程ID查找课程-用户关联
     * 
     * @param courseId 课程ID
     * @return 课程-用户关联列表
     */
    List<CourseUser> findByCourseId(String courseId);
    
    /**
     * 根据用户ID查找课程-用户关联
     * 
     * @param userId 用户ID
     * @return 课程-用户关联列表
     */
    List<CourseUser> findByUserId(String userId);
    
    /**
     * 根据课程ID和用户ID查找课程-用户关联
     * 
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 课程-用户关联（可选）
     */
    Optional<CourseUser> findByCourseIdAndUserId(String courseId, String userId);
    
    /**
     * 根据课程ID和角色查找课程-用户关联
     * 
     * @param courseId 课程ID
     * @param role 角色
     * @return 课程-用户关联列表
     */
    List<CourseUser> findByCourseIdAndRole(String courseId, String role);
    
    /**
     * 根据用户ID和角色查找课程-用户关联
     * 
     * @param userId 用户ID
     * @param role 角色
     * @return 课程-用户关联列表
     */
    List<CourseUser> findByUserIdAndRole(String userId, String role);
    
    /**
     * 统计课程成员数量
     * 
     * @param courseId 课程ID
     * @return 成员数量
     */
    long countByCourseId(String courseId);
    
    /**
     * 统计课程中特定角色的成员数量
     * 
     * @param courseId 课程ID
     * @param role 角色
     * @return 成员数量
     */
    long countByCourseIdAndRole(String courseId, String role);
    
    /**
     * 检查用户是否为课程成员
     * 
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 是否为课程成员
     */
    boolean existsByCourseIdAndUserId(String courseId, String userId);
    
    /**
     * 删除课程-用户关联
     * 
     * @param courseId 课程ID
     * @param userId 用户ID
     */
    void deleteByCourseIdAndUserId(String courseId, String userId);
    
    /**
     * 查找活跃的课程-用户关联
     * 
     * @param courseId 课程ID
     * @return 活跃的课程-用户关联列表
     */
    List<CourseUser> findByCourseIdAndActiveTrue(String courseId);
    
    /**
     * 统计活跃的成员数量
     * 
     * @param courseId 课程ID
     * @return 活跃成员数量
     */
    long countByCourseIdAndActiveTrue(String courseId);
    
    /**
     * 根据课程ID查找课程用户关系，并返回用户ID列表
     * 
     * @param courseId 课程ID
     * @return 用户ID列表
     */
    @Query("SELECT cu.userId FROM CourseUser cu WHERE cu.courseId = :courseId AND cu.active = true")
    List<String> findUserIdsByCourseId(@Param("courseId") String courseId);
} 