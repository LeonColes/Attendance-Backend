package com.attendance.controller;

import com.attendance.model.common.Result;
import com.attendance.model.dto.TaskDTO;
import com.attendance.security.SecurityUserDetails;
import com.attendance.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 考勤任务控制器 - 简化版
 * 专注于教师发布签到的核心流程
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    
    /**
     * 教师创建签到任务
     * 
     * @param taskDTO 任务信息
     * @return 创建的任务
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'SYSTEM_ADMIN')")
    @PostMapping
    public ResponseEntity<Result<TaskDTO>> createTask(@Valid @RequestBody TaskDTO taskDTO) {
        // 获取当前教师ID
        String teacherId = getCurrentUserId();
        // 验证当前用户是否为教师
        validateTeacherRole();
        
        TaskDTO result = taskService.createTask(taskDTO, teacherId);
        return ResponseEntity.ok(Result.success("签到任务创建成功", result));
    }
    
    /**
     * 获取签到任务详情
     * 
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping
    public ResponseEntity<Result<TaskDTO>> getTask(@RequestParam String taskId) {
        // 获取任务详情
        TaskDTO result = taskService.getTaskById(taskId);
        
        // 检查权限 - 如果是学生，确保任务是活跃状态的
        if (hasStudentRole() && result.getStatus() != com.attendance.model.entity.Task.TaskStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Result.error(403, "您无权查看非活跃状态的任务"));
        }
        
        // 如果是教师，确保任务是自己创建的
        if (hasTeacherRole() && !hasAdminRole() && !result.getCreatorId().equals(getCurrentUserId())) {
            return ResponseEntity.badRequest().body(Result.error(403, "您只能查看自己创建的任务"));
        }
        
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取活跃的签到任务列表
     * 所有用户都可以查看当前活跃的任务，但会根据角色进行适当过滤
     * 
     * @return 活跃的签到任务列表
     */
    @GetMapping("/active")
    public ResponseEntity<Result<List<TaskDTO>>> getActiveTasks() {
        String currentUserId = getCurrentUserId();
        log.info("获取活跃任务列表，请求用户ID: {}", currentUserId);
        
        List<TaskDTO> activeTasks = taskService.findActiveTasks();
        
        // 如果是教师，只返回自己创建的活跃任务
        if (hasTeacherRole() && !hasAdminRole()) {
            activeTasks = activeTasks.stream()
                .filter(task -> task.getCreatorId().equals(currentUserId))
                .collect(Collectors.toList());
            
            log.info("教师用户，过滤后返回{}个活跃任务", activeTasks.size());
        } else {
            log.info("学生或管理员用户，返回{}个活跃任务", activeTasks.size());
        }
        
        return ResponseEntity.ok(Result.success(activeTasks));
    }
    
    /**
     * 教师激活签到任务
     * 学生可以开始签到
     * 
     * @param taskId 任务ID
     * @return 激活后的任务
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'SYSTEM_ADMIN')")
    @PutMapping("/activate")
    public ResponseEntity<Result<TaskDTO>> activateTask(@RequestParam String taskId) {
        // 验证当前用户是否为教师
        validateTeacherRole();
        
        // 检查这个任务是否由当前老师创建
        TaskDTO task = taskService.getTaskById(taskId);
        if (!task.getCreatorId().equals(getCurrentUserId()) && !hasAdminRole()) {
            return ResponseEntity.badRequest().body(Result.error(403, "您只能激活自己创建的任务"));
        }
        
        TaskDTO result = taskService.activateTask(taskId);
        return ResponseEntity.ok(Result.success("签到任务已激活，学生可以开始签到", result));
    }
    
    /**
     * 教师结束签到任务
     * 学生不能再签到
     * 
     * @param taskId 任务ID
     * @return 完成后的任务
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'SYSTEM_ADMIN')")
    @PutMapping("/complete")
    public ResponseEntity<Result<TaskDTO>> completeTask(@RequestParam String taskId) {
        // 验证当前用户是否为教师
        validateTeacherRole();
        
        // 检查这个任务是否由当前老师创建
        TaskDTO task = taskService.getTaskById(taskId);
        if (!task.getCreatorId().equals(getCurrentUserId()) && !hasAdminRole()) {
            return ResponseEntity.badRequest().body(Result.error(403, "您只能完成自己创建的任务"));
        }
        
        TaskDTO result = taskService.completeTask(taskId);
        return ResponseEntity.ok(Result.success("签到任务已结束，学生不能再签到", result));
    }
    
    /**
     * 查询教师创建的所有签到任务
     * 
     * @return 教师创建的签到任务列表
     */
    @PreAuthorize("hasAnyAuthority('TEACHER', 'SYSTEM_ADMIN')")
    @GetMapping("/my-tasks")
    public ResponseEntity<Result<List<TaskDTO>>> getMyTasks() {
        // 验证当前用户是否为教师
        validateTeacherRole();
        
        String teacherId = getCurrentUserId();
        List<TaskDTO> result = taskService.findAllTasksByCreator(teacherId);
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
    
    /**
     * 验证当前用户是否为教师，如果不是则抛出异常
     */
    private void validateTeacherRole() {
        if (!hasTeacherRole() && !hasAdminRole()) {
            throw new com.attendance.exception.BusinessException(403, "只有教师才能执行此操作");
        }
    }
    
    /**
     * 检查当前用户是否为学生
     * 
     * @return 是否为学生
     */
    private boolean hasStudentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("STUDENT"));
    }
    
    /**
     * 检查当前用户是否为教师
     * 
     * @return 是否为教师
     */
    private boolean hasTeacherRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("TEACHER"));
    }
    
    /**
     * 检查当前用户是否为管理员
     * 
     * @return 是否为管理员
     */
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("SYSTEM_ADMIN"));
    }
}