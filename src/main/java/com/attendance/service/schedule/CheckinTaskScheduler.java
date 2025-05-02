package com.attendance.service.schedule;

import com.attendance.common.constants.SystemConstants;
import com.attendance.model.entity.Course;
import com.attendance.repository.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 签到任务状态自动更新调度器
 * 用于定期检查和更新签到任务状态
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckinTaskScheduler {

    private final CourseRepository courseRepository;

    /**
     * 定时更新签到任务状态
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    @Transactional
    public void updateTaskStatus() {
        log.debug("开始执行签到任务状态自动更新...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 激活已到开始时间的任务
        activateScheduledTasks(now);
        
        // 2. 结束已到结束时间的任务
        endExpiredTasks(now);
        
        log.debug("签到任务状态自动更新完成");
    }
    
    /**
     * 激活已到开始时间的签到任务
     * 将CREATED状态且开始时间已到的任务更新为ACTIVE状态
     */
    private void activateScheduledTasks(LocalDateTime now) {
        // 查找开始时间已到的待激活任务
        List<Course> tasksToActivate = courseRepository.findByCheckinStartTimeBeforeAndTypeAndStatus(
            now,
            SystemConstants.CourseType.CHECKIN,
            SystemConstants.TaskStatus.CREATED
        );
        
        if (!tasksToActivate.isEmpty()) {
            log.info("发现{}个需要激活的签到任务", tasksToActivate.size());
            
            for (Course task : tasksToActivate) {
                task.setStatus(SystemConstants.TaskStatus.ACTIVE);
                courseRepository.save(task);
                
                log.info("自动激活签到任务: 任务 [{}] ({}) 开始时间 [{}]，状态由 [{}] 更新为 [{}]", 
                    task.getId(), task.getName(), task.getCheckinStartTime(), 
                    SystemConstants.TaskStatus.CREATED, SystemConstants.TaskStatus.ACTIVE);
            }
        }
    }
    
    /**
     * 结束已到结束时间的签到任务
     * 将ACTIVE状态且结束时间已到的任务更新为ENDED状态
     */
    private void endExpiredTasks(LocalDateTime now) {
        // 查找结束时间已到的待结束任务
        List<Course> tasksToEnd = courseRepository.findByCheckinEndTimeBeforeAndTypeAndStatus(
            now, 
            SystemConstants.CourseType.CHECKIN,
            SystemConstants.TaskStatus.ACTIVE
        );
        
        if (!tasksToEnd.isEmpty()) {
            log.info("发现{}个需要结束的签到任务", tasksToEnd.size());
            
            for (Course task : tasksToEnd) {
                task.setStatus(SystemConstants.TaskStatus.ENDED);
                courseRepository.save(task);
                
                log.info("自动结束签到任务: 任务 [{}] ({}) 结束时间 [{}]，状态由 [{}] 更新为 [{}]", 
                    task.getId(), task.getName(), task.getCheckinEndTime(), 
                    SystemConstants.TaskStatus.ACTIVE, SystemConstants.TaskStatus.ENDED);
            }
        }
    }
} 