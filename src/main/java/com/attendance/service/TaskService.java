package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.dto.TaskDTO;
import com.attendance.model.entity.Task;
import com.attendance.model.entity.User;
import com.attendance.repository.TaskRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 考勤任务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    /**
     * 创建考勤任务
     * 
     * @param taskDTO 任务信息
     * @param creatorId 创建者ID
     * @return 创建的任务
     */
    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO, String creatorId) {
        log.info("创建考勤任务: {}, 创建者ID: {}", taskDTO.getTitle(), creatorId);
        
        // 获取创建者
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + creatorId));
        
        // 检查权限
        if (creator.getRole() == User.Role.STUDENT) {
            throw new BusinessException(403, "您没有权限创建考勤任务");
        }
        
        // 校验时间
        if (taskDTO.getEndTime().isBefore(taskDTO.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        
        // 创建任务
        Task task = taskDTO.toEntity();
        task.setCreator(creator);
        task.setStatus(Task.TaskStatus.CREATED);
        
        // 保存任务
        Task savedTask = taskRepository.save(task);
        log.info("考勤任务创建成功，ID: {}", savedTask.getId());
        
        return TaskDTO.fromEntity(savedTask);
    }
    
    /**
     * 更新考勤任务
     * 
     * @param id 任务ID
     * @param taskDTO 任务信息
     * @return 更新后的任务
     */
    @Transactional
    public TaskDTO updateTask(String id, TaskDTO taskDTO) {
        log.info("更新考勤任务，ID: {}", id);
        
        // 查找任务
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + id));
        
        // 校验时间
        if (taskDTO.getEndTime().isBefore(taskDTO.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        
        // 更新任务
        existingTask.setTitle(taskDTO.getTitle());
        existingTask.setDescription(taskDTO.getDescription());
        existingTask.setStartTime(taskDTO.getStartTime());
        existingTask.setEndTime(taskDTO.getEndTime());
        existingTask.setLocationRequirement(taskDTO.getLocationRequirement());
        existingTask.setCheckInType(taskDTO.getCheckInType());
        
        // 保存更新
        Task updatedTask = taskRepository.save(existingTask);
        log.info("考勤任务更新成功，ID: {}", updatedTask.getId());
        
        return TaskDTO.fromEntity(updatedTask);
    }
    
    /**
     * 获取考勤任务详情
     * 
     * @param id 任务ID
     * @return 任务详情
     */
    public TaskDTO getTaskById(String id) {
        log.info("获取考勤任务详情，ID: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + id));
        
        return TaskDTO.fromEntity(task);
    }
    
    /**
     * 分页查询考勤任务
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<TaskDTO> findTasks(Pageable pageable) {
        log.info("分页查询考勤任务");
        
        return taskRepository.findAll(pageable)
                .map(TaskDTO::fromEntity);
    }
    
    /**
     * 根据创建者查询考勤任务
     * 
     * @param creatorId 创建者ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<TaskDTO> findTasksByCreator(String creatorId, Pageable pageable) {
        log.info("查询创建者的考勤任务，创建者ID: {}", creatorId);
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + creatorId));
        
        return taskRepository.findByCreator(creator, pageable)
                .map(TaskDTO::fromEntity);
    }
    
    /**
     * 根据创建者查询所有考勤任务（不分页）
     * 
     * @param creatorId 创建者ID
     * @return 任务列表
     */
    public List<TaskDTO> findAllTasksByCreator(String creatorId) {
        log.info("查询创建者的所有考勤任务，创建者ID: {}", creatorId);
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + creatorId));
        
        return taskRepository.findByCreator(creator)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询当前活跃的考勤任务
     * 
     * @return 活跃的考勤任务列表，按开始时间倒序排列
     */
    public List<TaskDTO> findActiveTasks() {
        log.info("查询当前活跃的考勤任务");
        
        // 直接使用已排序的查询方法
        List<Task> activeTasks = taskRepository.findByStatusOrderByStartTimeDesc(Task.TaskStatus.ACTIVE);
        
        return activeTasks.stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 激活考勤任务
     * 
     * @param id 任务ID
     * @return 激活后的任务
     */
    @Transactional
    public TaskDTO activateTask(String id) {
        log.info("激活考勤任务，ID: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + id));
        
        if (task.getStatus() != Task.TaskStatus.CREATED) {
            throw new BusinessException("只有处于已创建状态的任务才能被激活");
        }
        
        task.setStatus(Task.TaskStatus.ACTIVE);
        Task activatedTask = taskRepository.save(task);
        
        log.info("考勤任务已激活，ID: {}", activatedTask.getId());
        
        return TaskDTO.fromEntity(activatedTask);
    }
    
    /**
     * 完成考勤任务
     * 
     * @param id 任务ID
     * @return 完成后的任务
     */
    @Transactional
    public TaskDTO completeTask(String id) {
        log.info("完成考勤任务，ID: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + id));
        
        if (task.getStatus() != Task.TaskStatus.ACTIVE) {
            throw new BusinessException("只有处于进行中状态的任务才能被完成");
        }
        
        task.setStatus(Task.TaskStatus.COMPLETED);
        Task completedTask = taskRepository.save(task);
        
        log.info("考勤任务已完成，ID: {}", completedTask.getId());
        
        return TaskDTO.fromEntity(completedTask);
    }
    
    /**
     * 取消考勤任务
     * 
     * @param id 任务ID
     * @return 取消后的任务
     */
    @Transactional
    public TaskDTO cancelTask(String id) {
        log.info("取消考勤任务，ID: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + id));
        
        if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            throw new BusinessException("已完成的任务不能被取消");
        }
        
        task.setStatus(Task.TaskStatus.CANCELLED);
        Task cancelledTask = taskRepository.save(task);
        
        log.info("考勤任务已取消，ID: {}", cancelledTask.getId());
        
        return TaskDTO.fromEntity(cancelledTask);
    }
}