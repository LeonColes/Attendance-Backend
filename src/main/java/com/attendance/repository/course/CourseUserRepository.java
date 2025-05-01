package com.attendance.repository.course;

import com.attendance.model.entity.CourseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
     * 根据用户ID查找活跃的课程-用户关联
     * 
     * @param userId 用户ID
     * @return 活跃的课程-用户关联列表
     */
    List<CourseUser> findByUserIdAndActiveTrue(String userId);
    
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
    
    /**
     * 根据课程ID和用户名查找关联
     *
     * @param courseId 课程ID
     * @param username 用户名
     * @return 可选的课程用户关联
     */
    @Query("SELECT cu FROM CourseUser cu JOIN User u ON cu.userId = u.id WHERE cu.courseId = :courseId AND u.username = :username")
    Optional<CourseUser> findByCourseIdAndUsername(@Param("courseId") String courseId, @Param("username") String username);
    
    /**
     * 检查用户是否是活跃的课程成员
     *
     * @param courseId 课程ID
     * @param username 用户名
     * @return 是否是活跃成员
     */
    @Query("SELECT COUNT(cu) > 0 FROM CourseUser cu JOIN User u ON cu.userId = u.id WHERE cu.courseId = :courseId AND u.username = :username AND cu.active = true")
    boolean existsByCourseIdAndUsernameAndActiveTrue(@Param("courseId") String courseId, @Param("username") String username);
    
    /**
     * 检查用户是否为活跃的课程成员
     *
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 是否为活跃成员
     */
    boolean existsByCourseIdAndUserIdAndActiveTrue(String courseId, String userId);
    
    /**
     * 统计课程中特定角色的活跃成员数量
     *
     * @param courseId 课程ID
     * @param role 角色
     * @return 活跃成员数量
     */
    long countByCourseIdAndRoleAndActiveTrue(String courseId, String role);
    
    /**
     * 查询指定课程和角色的用户ID列表
     *
     * @param courseId 课程ID
     * @param role 角色
     * @return 用户ID列表
     */
    @Query("SELECT cu.userId FROM CourseUser cu WHERE cu.courseId = :courseId AND cu.role = :role AND cu.active = true")
    List<String> findUserIdsByCourseIdAndRole(@Param("courseId") String courseId, @Param("role") String role);
    
    /**
     * 删除指定课程的所有成员关系
     * 
     * @param courseId 课程ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CourseUser cu WHERE cu.courseId = :courseId")
    void deleteAllByCourseId(@Param("courseId") String courseId);
}