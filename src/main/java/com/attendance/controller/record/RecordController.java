package com.attendance.controller.record;

import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.record.RecordDTO;
import com.attendance.service.record.RecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 签到记录控制器
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
public class RecordController {

    private final RecordService recordService;
    
    /**
     * 获取签到记录
     * 
     * @param id 记录ID
     * @return 签到记录
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.canAccessRecord(#id)")
    public ApiResponse<RecordDTO> getRecord(@PathVariable String id) {
        RecordDTO recordDTO = recordService.getRecord(id);
        return ApiResponse.success(recordDTO);
    }
    
    /**
     * 获取签到任务的所有签到记录
     * 
     * @param courseId 签到任务ID
     * @return 签到记录列表
     */
    @GetMapping("/checkin/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or @courseSecurityService.isCheckinCreator(#courseId)")
    public ApiResponse<List<RecordDTO>> getRecordsByCourse(@PathVariable String courseId) {
        List<RecordDTO> records = recordService.getRecordsByCourse(courseId);
        return ApiResponse.success(records);
    }
    
    /**
     * 获取用户的所有签到记录
     * 
     * @param userId 用户ID
     * @return 签到记录列表
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or @securityService.isCurrentUser(#userId)")
    public ApiResponse<List<RecordDTO>> getRecordsByUser(@PathVariable String userId) {
        List<RecordDTO> records = recordService.getRecordsByUser(userId);
        return ApiResponse.success(records);
    }
    
    /**
     * 获取课程的所有签到记录
     * 
     * @param courseId 课程ID
     * @return 签到记录列表
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or @courseSecurityService.isCourseCreator(#courseId)")
    public ApiResponse<List<RecordDTO>> getRecordsByParentCourse(@PathVariable String courseId) {
        List<RecordDTO> records = recordService.getRecordsByParentCourse(courseId);
        return ApiResponse.success(records);
    }
    
    /**
     * 提交签到
     * 
     * @param request 签到请求
     * @return 签到结果
     */
    @PostMapping("/check-in")
    public ApiResponse<RecordDTO> submitCheckIn(@Valid @RequestBody CheckInRequest request) {
        RecordDTO record = recordService.submitCheckIn(
                request.getCourseId(),
                request.getVerifyMethod(),
                request.getLocation(),
                request.getDevice(),
                request.getVerifyData()
        );
        return ApiResponse.success("签到成功", record);
    }
    
    /**
     * 签到请求
     */
    @Data
    public static class CheckInRequest {
        /**
         * 签到任务ID
         */
        @NotBlank(message = "签到任务ID不能为空")
        private String courseId;
        
        /**
         * 验证方式
         */
        @NotBlank(message = "验证方式不能为空")
        private String verifyMethod;
        
        /**
         * 位置信息
         */
        private String location;
        
        /**
         * 设备信息
         */
        private String device;
        
        /**
         * 验证数据
         */
        private String verifyData;
    }
} 