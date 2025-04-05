package com.attendance.controller.course;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.dto.course.CreateCourseRequest;
import com.attendance.service.course.CourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 课程/签到任务控制器
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;
    
    /**
     * 创建课程/签到任务
     * 
     * @param request 创建请求
     * @return 创建的课程/签到任务
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<CourseDTO> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        log.info("创建课程/签到任务请求: {}", request);
        
        CourseDTO courseDTO;
        
        // 根据类型区分处理逻辑
        if (SystemConstants.CourseType.COURSE.equals(request.getType())) {
            courseDTO = courseService.createCourse(
                request.getName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate()
            );
            return ApiResponse.success("课程创建成功", courseDTO);
        } else if (SystemConstants.CourseType.CHECKIN.equals(request.getType())) {
            courseDTO = courseService.createCheckinTask(
                request.getParentCourseId(),
                request.getName(),
                request.getDescription(),
                request.getCheckinStartTime(),
                request.getCheckinEndTime(),
                request.getCheckinType(),
                request.getVerifyParams()
            );
            return ApiResponse.success("签到任务创建成功", courseDTO);
        } else {
            return ApiResponse.error("不支持的类型: " + request.getType());
        }
    }
    
    /**
     * 获取课程/签到任务详情
     * 
     * @param id 课程/签到任务ID
     * @return 课程/签到任务信息
     */
    @GetMapping("/{id}")
    public ApiResponse<CourseDTO> getCourse(@PathVariable String id) {
        log.info("获取课程/签到任务详情: {}", id);
        CourseDTO courseDTO = courseService.getCourse(id);
        return ApiResponse.success(courseDTO);
    }
    
    /**
     * 获取所有课程
     * 
     * @return 课程列表
     */
    @GetMapping
    public ApiResponse<List<CourseDTO>> getAllCourses() {
        log.info("获取所有课程");
        List<CourseDTO> courses = courseService.getAllCourses();
        return ApiResponse.success(courses);
    }
    
    /**
     * 获取用户所有课程
     * 
     * @return 课程列表
     */
    @GetMapping("/my")
    public ApiResponse<List<CourseDTO>> getMyCourses() {
        log.info("获取我的课程");
        List<CourseDTO> courses = courseService.getMyCourses();
        return ApiResponse.success(courses);
    }
    
    /**
     * 获取课程下的所有签到任务
     * 
     * @param courseId 课程ID
     * @return 签到任务列表
     */
    @GetMapping("/{courseId}/checkins")
    public ApiResponse<List<CourseDTO>> getCourseCheckinTasks(@PathVariable String courseId) {
        log.info("获取课程下的签到任务: {}", courseId);
        List<CourseDTO> checkinTasks = courseService.getCourseCheckinTasks(courseId);
        return ApiResponse.success(checkinTasks);
    }
    
    /**
     * 通过邀请码加入课程
     * 
     * @param code 邀请码
     * @return 加入结果
     */
    @PostMapping("/join/{code}")
    public ApiResponse<CourseUserDTO> joinCourseByCode(@PathVariable String code) {
        log.info("通过邀请码加入课程: {}", code);
        CourseUserDTO courseUserDTO = courseService.joinCourseByCode(code);
        return ApiResponse.success("成功加入课程", courseUserDTO);
    }
    
    /**
     * 添加课程成员
     * 
     * @param courseId 课程ID
     * @param request 添加成员请求
     * @return 添加结果
     */
    @PostMapping("/{courseId}/members")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#courseId) or hasRole('ADMIN')")
    public ApiResponse<AddMembersResponse> addCourseMembers(
            @PathVariable String courseId,
            @Valid @RequestBody AddMembersRequest request) {
        log.info("添加课程成员: 课程ID={}, 请求={}", courseId, request);
        
        int successCount = courseService.addCourseMembers(courseId, request.getUserIds(), request.getRole());
        
        AddMembersResponse response = new AddMembersResponse();
        response.setSuccessful(successCount);
        response.setFailed(request.getUserIds().size() - successCount);
        response.setNewMemberCount(courseService.getCourseMemberCount(courseId));
        
        return ApiResponse.success(String.format("成功添加%d名成员", successCount), response);
    }
    
    /**
     * 提交签到
     * 
     * @param checkinId 签到任务ID
     * @param request 签到请求
     * @return 签到结果
     */
    @PostMapping("/checkin/{checkinId}")
    public ApiResponse<CheckinResponse> submitCheckin(
            @PathVariable String checkinId,
            @Valid @RequestBody SubmitCheckinRequest request) {
        log.info("提交签到: 任务ID={}, 请求={}", checkinId, request);
        
        boolean success = courseService.submitCheckin(
            checkinId,
            request.getVerifyData(),
            request.getLocation(),
            request.getDevice()
        );
        
        CheckinResponse response = new CheckinResponse();
        response.setCheckinId(checkinId);
        response.setSuccess(success);
        response.setTimestamp(System.currentTimeMillis());
        
        return ApiResponse.success(success ? "签到成功" : "签到失败", response);
    }
    
    /**
     * 根据签到类型生成签到二维码内容
     * 
     * @param checkinId 签到任务ID
     * @return 签到码
     */
    @GetMapping("/checkin/{checkinId}/code")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<CheckinCodeResponse> generateCheckinCode(@PathVariable String checkinId) {
        log.info("生成签到二维码: 任务ID={}", checkinId);
        
        String checkinCode = courseService.generateCheckinCode(checkinId);
        
        CheckinCodeResponse response = new CheckinCodeResponse();
        response.setCheckinId(checkinId);
        response.setCode(checkinCode);
        response.setTimestamp(System.currentTimeMillis());
        
        return ApiResponse.success("生成签到码成功", response);
    }
    
    /**
     * 结束签到任务
     * 
     * @param checkinId 签到任务ID
     * @return 结果
     */
    @PostMapping("/checkin/{checkinId}/end")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<CourseDTO> endCheckinTask(@PathVariable String checkinId) {
        log.info("结束签到任务: 任务ID={}", checkinId);
        
        CourseDTO courseDTO = courseService.updateCourseStatus(checkinId, SystemConstants.TaskStatus.ENDED);
        return ApiResponse.success("签到任务已结束", courseDTO);
    }
    
    // ========== 请求/响应数据类 ==========
    
    /**
     * 添加成员请求
     */
    @Data
    public static class AddMembersRequest {
        /**
         * 用户ID列表
         */
        @NotEmpty(message = "用户ID列表不能为空")
        private List<String> userIds;
        
        /**
         * 角色
         */
        @NotBlank(message = "角色不能为空")
        private String role;
    }
    
    /**
     * 添加成员响应
     */
    @Data
    public static class AddMembersResponse {
        /**
         * 成功添加数量
         */
        private int successful;
        
        /**
         * 添加失败数量
         */
        private int failed;
        
        /**
         * 新的成员总数
         */
        private int newMemberCount;
    }
    
    /**
     * 签到请求
     */
    @Data
    public static class SubmitCheckinRequest {
        /**
         * 验证数据 (根据签到类型不同，结构不同)
         */
        private String verifyData;
        
        /**
         * 位置信息
         */
        private String location;
        
        /**
         * 设备信息
         */
        private String device;
    }
    
    /**
     * 签到响应
     */
    @Data
    public static class CheckinResponse {
        /**
         * 签到任务ID
         */
        private String checkinId;
        
        /**
         * 是否成功
         */
        private boolean success;
        
        /**
         * 时间戳
         */
        private long timestamp;
    }
    
    /**
     * 签到码响应
     */
    @Data
    public static class CheckinCodeResponse {
        /**
         * 签到任务ID
         */
        private String checkinId;
        
        /**
         * 签到码
         */
        private String code;
        
        /**
         * 时间戳
         */
        private long timestamp;
    }
} 