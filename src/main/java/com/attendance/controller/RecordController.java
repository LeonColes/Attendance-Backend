package com.attendance.controller;

import com.attendance.model.common.Result;
import com.attendance.model.dto.RecordDTO;
import com.attendance.model.dto.TaskDTO;
import com.attendance.model.entity.Task;
import com.attendance.security.SecurityUserDetails;
import com.attendance.service.RecordService;
import com.attendance.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考勤记录控制器
 * 专注于学生签到流程
 */
@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@Slf4j
public class RecordController {

    private final RecordService recordService;
    private final TaskService taskService;
    
    /**
     * 学生签到
     * 
     * @param recordDTO 签到信息
     * @return 签到结果
     */
    @PostMapping
    public ResponseEntity<Result<RecordDTO>> signIn(@Valid @RequestBody RecordDTO recordDTO) {
        // 验证当前用户是否为学生
        validateStudentRole();
        
        // 设置为当前用户ID
        String studentId = getCurrentUserId();
        recordDTO.setUserId(studentId);
        
        // 检查任务状态是否为活跃状态
        String taskId = recordDTO.getTaskId();
        Task.TaskStatus taskStatus = taskService.getTaskById(taskId).getStatus();
        if (taskStatus != Task.TaskStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Result.error(400, "只能签到活跃状态的任务"));
        }
        
        // 检查是否已经签到过
        boolean alreadySignedIn = recordService.hasUserSignedIn(studentId, taskId);
        if (alreadySignedIn) {
            return ResponseEntity.badRequest().body(Result.error(400, "您已经签到过此任务"));
        }
        
        RecordDTO result = recordService.createRecord(recordDTO);
        return ResponseEntity.ok(Result.success("签到成功", result));
    }
    
    /**
     * 学生签到 - 使用URL参数版本
     * 
     * @param taskId 任务ID
     * @param checkInLocation 签到位置
     * @param ipAddress IP地址
     * @param deviceInfo 设备信息
     * @return 签到结果
     */
    @PostMapping("/sign-in")
    public ResponseEntity<Result<RecordDTO>> signInWithParams(
            @RequestParam String taskId,
            @RequestParam(required = false) String checkInLocation,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String deviceInfo,
            @RequestParam(required = false) String remark) {
        
        // 验证当前用户是否为学生
        validateStudentRole();
        
        // 设置为当前用户ID
        String studentId = getCurrentUserId();
        
        // 检查任务状态是否为活跃状态
        Task.TaskStatus taskStatus = taskService.getTaskById(taskId).getStatus();
        if (taskStatus != Task.TaskStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Result.error(400, "只能签到活跃状态的任务"));
        }
        
        // 检查是否已经签到过
        boolean alreadySignedIn = recordService.hasUserSignedIn(studentId, taskId);
        if (alreadySignedIn) {
            return ResponseEntity.badRequest().body(Result.error(400, "您已经签到过此任务"));
        }
        
        // 创建签到记录DTO
        RecordDTO recordDTO = new RecordDTO();
        recordDTO.setUserId(studentId);
        recordDTO.setTaskId(taskId);
        recordDTO.setCheckInLocation(checkInLocation);
        recordDTO.setIpAddress(ipAddress);
        recordDTO.setDeviceInfo(deviceInfo);
        recordDTO.setRemark(remark);
        
        RecordDTO result = recordService.createRecord(recordDTO);
        return ResponseEntity.ok(Result.success("签到成功", result));
    }
    
    /**
     * 获取学生的签到记录
     * 
     * @return 签到记录列表
     */
    @GetMapping("/my")
    public ResponseEntity<Result<List<RecordDTO>>> getMyRecords() {
        String userId = getCurrentUserId();
        log.info("获取用户签到记录，用户ID: {}", userId);
        
        List<RecordDTO> result = recordService.getAllRecordsByUserId(userId);
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取特定任务的签到记录
     * 教师用于查看某次签到的所有记录
     * 
     * @param taskId 任务ID
     * @return 签到记录列表
     */
    @GetMapping
    public ResponseEntity<Result<List<RecordDTO>>> getTaskRecords(@RequestParam String taskId) {
        String currentUserId = getCurrentUserId();
        log.info("获取任务签到记录，任务ID: {}, 请求用户ID: {}", taskId, currentUserId);
        
        TaskDTO task = taskService.getTaskById(taskId);
        
        // 权限检查：管理员可以查看所有，教师只能查看自己创建的，学生只能查看自己参与的
        if (hasAdminRole()) {
            // 管理员可以查看所有任务的记录
            log.info("管理员查看任务记录");
        } else if (hasTeacherRole()) {
            // 教师只能查看自己创建的任务的记录
            if (!task.getCreatorId().equals(currentUserId)) {
                log.warn("权限拒绝：教师尝试查看不是自己创建的任务记录");
                return ResponseEntity.badRequest().body(Result.error(403, "您只能查看自己创建的任务的签到记录"));
            }
            log.info("教师查看自己创建的任务记录");
        } else {
            // 学生只能查看自己的签到记录
            log.info("学生查看任务记录，将只返回自己的记录");
            List<RecordDTO> myRecords = recordService.getAllRecordsByUserIdAndTaskId(currentUserId, taskId);
            return ResponseEntity.ok(Result.success(myRecords));
        }
        
        List<RecordDTO> result = recordService.getAllRecordsByTaskId(taskId);
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取任务的签到统计
     * 教师用于查看签到情况统计
     * 
     * @param taskId 任务ID
     * @return 签到统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Result<Map<String, Long>>> getTaskStatistics(@RequestParam String taskId) {
        String currentUserId = getCurrentUserId();
        log.info("获取任务签到统计，任务ID: {}, 请求用户ID: {}", taskId, currentUserId);
        
        TaskDTO task = taskService.getTaskById(taskId);
        
        // 权限检查：管理员和创建者可以查看统计
        if (!hasAdminRole() && hasTeacherRole() && !task.getCreatorId().equals(currentUserId)) {
            log.warn("权限拒绝：用户尝试查看不是自己创建的任务统计");
            return ResponseEntity.badRequest().body(Result.error(403, "您只能查看自己创建的任务的签到统计"));
        }
        
        // 学生不能查看统计
        if (hasStudentRole() && !hasTeacherRole() && !hasAdminRole()) {
            log.warn("权限拒绝：学生尝试查看任务统计");
            return ResponseEntity.badRequest().body(Result.error(403, "学生无权查看签到统计"));
        }
        
        Map<String, Long> result = recordService.getTaskStatistics(taskId);
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
     * 验证当前用户是否为学生，如果不是则抛出异常
     */
    private void validateStudentRole() {
        if (!hasStudentRole()) {
            throw new com.attendance.exception.BusinessException(403, "只有学生才能执行此操作");
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