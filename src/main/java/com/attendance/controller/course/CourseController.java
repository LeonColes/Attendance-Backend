package com.attendance.controller.course;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.common.model.ApiResponse;
import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.dto.course.CreateCourseRequest;
import com.attendance.model.dto.course.CreateAttendanceRequest;
import com.attendance.model.dto.user.UserDTO;
import com.attendance.service.course.CourseService;
import com.attendance.service.user.UserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;
    
    /**
     * 创建课程
     * 
     * @param request 创建请求
     * @return 创建的课程
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<CourseDTO> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        log.info("创建课程请求: {}", request);
        
        CourseDTO courseDTO = courseService.createCourse(
            request.getName(),
            request.getDescription(),
            request.getStartDate(),
            request.getEndDate()
        );
        return ApiResponse.success("课程创建成功", courseDTO);
    }
    
    /**
     * 创建签到任务
     * 
     * @param request 创建签到请求
     * @return 创建的签到任务
     */
    @PostMapping("/attendance/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<CourseDTO> createAttendance(@Valid @RequestBody CreateAttendanceRequest request) {
        log.info("创建签到任务请求: {}", request);
        
        CourseDTO courseDTO = courseService.createCheckinTask(
            request.getCourseId(),
            request.getTitle(),
            request.getDescription(),
            request.getStartTime(),
            request.getEndTime(),
            request.getCheckInType(),
            request.getVerifyParams()
        );
        return ApiResponse.success("签到任务创建成功", courseDTO);
    }
    
    /**
     * 获取课程详情
     * 
     * @param id 课程ID
     * @return 课程信息
     */
    @GetMapping("/detail")
    public ApiResponse<CourseDTO> getCourse(@RequestParam String id) {
        log.info("获取课程详情: {}", id);
        CourseDTO courseDTO = courseService.getCourse(id);
        return ApiResponse.success(courseDTO);
    }
    
    /**
     * 获取用户所有课程（老师只能看自己创建的，学生只能看自己加入的）
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 课程列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("获取我的课程列表: page={}, size={}", page, size);
        Map<String, Object> response = courseService.getMyCourses(page, size);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取课程下的所有签到任务
     * 
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 签到任务列表
     */
    @GetMapping("/{courseId}/checkins")
    public ApiResponse<Map<String, Object>> getCourseCheckinTasks(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("获取课程下的签到任务: courseId={}, page={}, size={}", courseId, page, size);
        Map<String, Object> response = courseService.getCourseCheckinTasks(courseId, page, size);
        return ApiResponse.success(response);
    }
    
    /**
     * 通过邀请码加入课程
     * 
     * @param code 邀请码
     * @return 加入结果
     */
    @PostMapping("/members/join")
    public ApiResponse<CourseUserDTO> joinCourseByCode(@RequestParam String code) {
        log.info("通过邀请码加入课程: {}", code);
        CourseUserDTO courseUserDTO = courseService.joinCourseByCode(code);
        return ApiResponse.success("成功加入课程", courseUserDTO);
    }
    
    /**
     * 获取课程成员列表
     * 
     * @param courseId 课程ID
     * @param page 页码
     * @param size 每页大小
     * @return 成员列表
     */
    @GetMapping("/members/list")
    public ApiResponse<Map<String, Object>> getCourseMembers(
            @RequestParam String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("获取课程成员列表: courseId={}, page={}, size={}", courseId, page, size);
        Map<String, Object> response = userService.getCourseUsers(courseId, page, size);
        return ApiResponse.success(response);
    }
    
    /**
     * 添加课程成员
     * 
     * @param request 添加成员请求
     * @return 添加结果
     */
    @PostMapping("/members/add")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#request.courseId) or hasRole('ADMIN')")
    public ApiResponse<AddMembersResponse> addCourseMembers(@Valid @RequestBody AddMembersRequest request) {
        log.info("添加课程成员: 请求={}", request);
        
        int successCount = courseService.addCourseMembers(
            request.getCourseId(), 
            request.getUserIds(), 
            request.getRole()
        );
        
        AddMembersResponse response = new AddMembersResponse();
        response.setSuccessful(successCount);
        response.setFailed(request.getUserIds().size() - successCount);
        response.setNewMemberCount(courseService.getCourseMemberCount(request.getCourseId()));
        
        return ApiResponse.success(String.format("成功添加%d名成员", successCount), response);
    }
    
    /**
     * 学生提交签到
     * 
     * @param request 签到请求
     * @return 签到结果
     */
    @PostMapping("/attendance/signin")
    public ApiResponse<CheckinResponse> submitCheckin(@Valid @RequestBody SubmitCheckinRequest request) {
        log.info("提交签到: 请求={}", request);
        
        boolean success = courseService.submitCheckin(
            request.getTaskId(),
            request.getVerifyData(),
            request.getLocation(),
            request.getDevice()
        );
        
        CheckinResponse response = new CheckinResponse();
        response.setCheckinId(request.getTaskId());
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
    @GetMapping("/attendance/code")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<CheckinCodeResponse> generateCheckinCode(@RequestParam String checkinId) {
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
    @PostMapping("/attendance/end")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<CourseDTO> endCheckinTask(@RequestParam String checkinId) {
        log.info("结束签到任务: 任务ID={}", checkinId);
        
        CourseDTO courseDTO = courseService.updateCourseStatus(checkinId, SystemConstants.TaskStatus.ENDED);
        return ApiResponse.success("签到任务已结束", courseDTO);
    }
    
    /**
     * 移除课程成员
     * 
     * @param request 移除成员请求
     * @return 结果
     */
    @PostMapping("/members/remove")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#request.courseId) or hasRole('ADMIN')")
    public ApiResponse<String> removeCourseMembers(@Valid @RequestBody RemoveMemberRequest request) {
        log.info("移除课程成员: 请求={}", request);
        
        boolean success = courseService.removeCourseMember(request.getCourseId(), request.getUserId(), request.getReason());
        
        return ApiResponse.success(success ? "成功移除成员" : "移除成员失败");
    }
    
    /**
     * 获取课程邀请二维码图片(直接返回图片数据)
     * 
     * @param courseId 课程ID
     * @return 二维码图片
     */
    @GetMapping(value = "/qrcode", produces = "image/png")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#courseId) or hasRole('ADMIN')")
    public byte[] generateCourseQRCode(@RequestParam String courseId) {
        try {
            CourseDTO course = courseService.getCourse(courseId);
            if (course == null || !SystemConstants.CourseType.COURSE.equals(course.getType())) {
                throw new BusinessException("课程不存在");
            }
            
            // 生成邀请链接
            String inviteUrl = "attendance://join?code=" + course.getCode();
            
            // 创建二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(inviteUrl, BarcodeFormat.QR_CODE, 250, 250, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("生成二维码失败: " + e.getMessage());
        }
    }
    
    // ========== 请求/响应数据类 ==========
    
    /**
     * 添加成员请求
     */
    @Data
    public static class AddMembersRequest {
        /**
         * 课程ID
         */
        @NotBlank(message = "课程ID不能为空")
        private String courseId;
        
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
         * 当前成员总数
         */
        private int newMemberCount;
    }
    
    /**
     * 移除成员请求
     */
    @Data
    public static class RemoveMemberRequest {
        /**
         * 课程ID
         */
        @NotBlank(message = "课程ID不能为空")
        private String courseId;
        
        /**
         * 用户ID
         */
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        /**
         * 移除原因(可选)
         */
        private String reason;
    }
    
    /**
     * 提交签到请求
     */
    @Data
    public static class SubmitCheckinRequest {
        /**
         * 签到任务ID
         */
        @NotBlank(message = "签到任务ID不能为空")
        private String taskId;
        
        /**
         * 签到验证数据
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
        
        /**
         * 签到方式
         */
        @NotBlank(message = "签到方式不能为空")
        private String verifyMethod;
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