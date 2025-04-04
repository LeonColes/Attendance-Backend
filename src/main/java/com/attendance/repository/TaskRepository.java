package com.attendance.repository;

import com.attendance.model.entity.Task;
import com.attendance.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤任务数据访问接口
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    
    /**
     * 根据创建者查询考勤任务
     */
    Page<Task> findByCreator(User creator, Pageable pageable);
    
    /**
     * 根据创建者查询考勤任务（不分页）
     */
    List<Task> findByCreator(User creator);
    
    /**
     * 查询当前活跃的考勤任务
     */
    List<Task> findByStatusAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Task.TaskStatus status, 
            LocalDateTime now, 
            LocalDateTime sameNow);
    
    /**
     * 查询时间段内的考勤任务
     */
    List<Task> findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            LocalDateTime startTime, 
            LocalDateTime endTime);
    
    /**
     * 根据状态查询考勤任务
     */
    Page<Task> findByStatus(Task.TaskStatus status, Pageable pageable);
    
    /**
     * 根据状态查询考勤任务（不分页）
     */
    List<Task> findByStatus(Task.TaskStatus status);
    
    /**
     * 根据状态查询考勤任务，按开始时间倒序排序
     */
    List<Task> findByStatusOrderByStartTimeDesc(Task.TaskStatus status);
}