package com.attendance.repository;

import com.attendance.model.entity.Record;
import com.attendance.model.entity.Task;
import com.attendance.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 考勤记录数据访问接口
 */
@Repository
public interface RecordRepository extends JpaRepository<Record, String> {
    
    /**
     * 查找用户在特定任务的考勤记录
     */
    Optional<Record> findByUserAndTask(User user, Task task);
    
    /**
     * 根据用户ID查询考勤记录
     */
    Page<Record> findByUserId(String userId, Pageable pageable);
    
    /**
     * 根据用户ID查询所有考勤记录（不分页）
     */
    List<Record> findByUserId(String userId);
    
    /**
     * 根据用户ID查询所有考勤记录（按签到时间倒序排列）
     */
    List<Record> findByUserIdOrderByCheckInTimeDesc(String userId);
    
    /**
     * 根据任务ID查询考勤记录
     */
    Page<Record> findByTaskId(String taskId, Pageable pageable);
    
    /**
     * 根据任务ID查询所有考勤记录（不分页）
     */
    List<Record> findByTaskId(String taskId);
    
    /**
     * 根据任务ID查询所有考勤记录（按签到时间倒序排列）
     */
    List<Record> findByTaskIdOrderByCheckInTimeDesc(String taskId);
    
    /**
     * 根据用户ID和任务ID查询考勤记录
     */
    Optional<Record> findByUserIdAndTaskId(String userId, String taskId);
    
    /**
     * 查询时间范围内的考勤记录
     */
    @Query("SELECT r FROM Record r WHERE r.checkInTime BETWEEN :startTime AND :endTime")
    List<Record> findByCheckInTimeBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询用户时间范围内的考勤记录
     */
    @Query("SELECT r FROM Record r WHERE r.user.id = :userId AND r.checkInTime BETWEEN :startTime AND :endTime")
    List<Record> findByUserIdAndCheckInTimeBetween(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据签到状态查询考勤记录
     */
    Page<Record> findByStatus(Record.RecordStatus status, Pageable pageable);
    
    /**
     * 查询某任务所有考勤状态统计
     */
    @Query("SELECT r.status, COUNT(r) FROM Record r WHERE r.task.id = :taskId GROUP BY r.status")
    List<Object[]> countByTaskIdGroupByStatus(@Param("taskId") String taskId);
    
    /**
     * 查询某任务所有未签到学生人数（用于统计）
     */
    @Query(value = "SELECT COUNT(u.id) FROM users u " +
           "WHERE u.role = 'STUDENT' AND u.id NOT IN " +
           "(SELECT r.user_id FROM records r WHERE r.task_id = :taskId)", nativeQuery = true)
    long countAbsentStudentsForTask(@Param("taskId") String taskId);
    
    /**
     * 查询用户在时间范围内的考勤记录数量
     */
    @Query("SELECT COUNT(r) FROM Record r WHERE r.user.id = :userId AND r.status = :status AND r.checkInTime BETWEEN :startTime AND :endTime")
    long countByUserIdAndStatusAndCheckInTimeBetween(
            @Param("userId") String userId, 
            @Param("status") Record.RecordStatus status,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
}