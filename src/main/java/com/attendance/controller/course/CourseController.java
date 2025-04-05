package com.attendance.controller.course;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.common.model.ApiResponse;
import com.attendance.common.model.PageRequestDTO;
import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.dto.course.CreateCourseRequest;
import com.attendance.model.dto.course.CreateAttendanceRequest;
import com.attendance.model.dto.course.CourseRecordDTO;
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
     * @param requestDTO 分页请求参数
     * @return 课程列表
     */
    @PostMapping("/list")
    public ApiResponse<Map<String, Object>> getMyCourses(@Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取我的课程列表: page={}, size={}", requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getMyCourses(requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
    }
    
    /**
     * 获取课程下的所有签到任务列表
     * 
     * @param courseId 课程ID
     * @param requestDTO 分页请求参数
     * @return 签到任务列表
     */
    @PostMapping("/attendance/list")
    public ApiResponse<Map<String, Object>> getAttendanceList(
            @RequestParam String courseId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取课程签到任务列表: courseId={}, page={}, size={}", 
            courseId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getAttendanceList(
            courseId, requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
    }
    
    /**
     * 获取课程的签到统计信息
     * 
     * @param courseId 课程ID
     * @return 课程签到统计信息
     */
    @GetMapping("/attendance/detail")
    public ApiResponse<Map<String, Object>> getCourseAttendanceDetail(
            @RequestParam String courseId) {
        log.info("获取课程签到统计: courseId={}", courseId);
        Map<String, Object> response = courseService.getCourseAttendanceDetail(courseId);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取单个签到任务的详细信息（教师看到学生签到统计，学生看到自己的签到状态）
     * 
     * @param checkinId 签到任务ID
     * @param requestDTO 分页请求参数
     * @return 签到任务详情
     */
    @PostMapping("/attendance/details")
    public ApiResponse<Map<String, Object>> getCheckinDetails(
            @RequestParam String checkinId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取签到任务详情: checkinId={}, page={}, size={}", 
            checkinId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getCheckinDetails(
            checkinId, requestDTO.getPage(), requestDTO.getSize());
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
     * @param requestDTO 分页请求参数
     * @return 成员列表
     */
    @PostMapping("/members")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#courseId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getCourseMembers(
            @RequestParam String courseId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取课程成员: courseId={}, page={}, size={}", 
            courseId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = userService.getCourseUsers(
            courseId, requestDTO.getPage(), requestDTO.getSize());
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
     * 提交签到
     * 
     * @param request 签到请求
     * @return 签到结果
     */
    @PostMapping("/attendance/check-in")
    public ApiResponse<CourseRecordDTO> submitCheckIn(@Valid @RequestBody CheckInRequest request) {
        log.info("提交签到: courseId={}, verifyMethod={}", request.getCourseId(), request.getVerifyMethod());
        CourseRecordDTO record = courseService.submitCheckIn(
                request.getCourseId(),
                request.getVerifyMethod(),
                request.getLocation(),
                request.getDevice(),
                request.getVerifyData()
        );
        return ApiResponse.success("签到成功", record);
    }
    
    /**
     * 获取用户在课程中的所有签到记录
     * 
     * @param courseId 课程ID
     * @param userId 用户ID（可选，不传则查当前用户）
     * @param requestDTO 分页请求参数
     * @return 签到记录列表
     */
    @PostMapping("/attendance/user/records")
    public ApiResponse<Map<String, Object>> getUserCourseRecords(
            @RequestParam String courseId,
            @RequestParam(required = false) String userId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取用户课程签到记录: courseId={}, userId={}, page={}, size={}", 
            courseId, userId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getUserCourseRecords(
            courseId, userId, requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
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
            
            // 直接使用邀请码作为二维码内容
            String inviteCode = course.getCode();
            
            // 创建二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 提高错误校正级别
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(inviteCode, BarcodeFormat.QR_CODE, 300, 300, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成课程邀请二维码失败", e);
            throw new BusinessException("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取签到二维码图片(直接返回图片数据)
     * 
     * @param checkinId 签到任务ID
     * @return 二维码图片
     */
    @GetMapping(value = "/attendance/qrcode", produces = "image/png")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public byte[] generateCheckinQRCode(@RequestParam String checkinId) {
        log.info("生成签到二维码: 任务ID={}", checkinId);
        
        try {
            // 验证签到任务是否存在
            CourseDTO checkinTask = courseService.getCourse(checkinId);
            if (checkinTask == null || !SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
                throw new BusinessException("签到任务不存在");
            }
            
            // 获取签到码 - 直接使用任务ID
            String checkinContent = checkinId;
            
            // 创建二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(checkinContent, BarcodeFormat.QR_CODE, 300, 300, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成签到二维码失败", e);
            throw new BusinessException("生成签到二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取签到记录（教师视角）
     * 
     * @param checkinId 签到任务ID
     * @param requestDTO 分页请求参数
     * @return 签到记录
     */
    @PostMapping("/attendance/records")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getCheckinRecords(
            @RequestParam String checkinId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取签到记录(老师视角): checkinId={}, page={}, size={}", 
            checkinId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getCheckinTeacherView(
            checkinId, requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
    }
    
    /**
     * 获取我的签到记录
     * （学生视角：查看自己在所有签到任务的状态）
     * 
     * @param courseId 课程ID
     * @return 签到记录列表
     */
    @GetMapping("/attendance/mystatus")
    public ApiResponse<Map<String, Object>> getMyCheckinStatus(@RequestParam String courseId) {
        log.info("获取我的签到记录(学生视角): courseId={}", courseId);
        Map<String, Object> response = courseService.getCheckinStudentView(courseId);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取签到任务统计信息
     * （老师视角：统计已签到和未签到学生情况）
     * 
     * @param checkinId 签到任务ID
     * @return 签到统计信息
     */
    @GetMapping("/attendance/stats")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getCheckinStatistics(@RequestParam String checkinId) {
        log.info("获取签到统计信息: checkinId={}", checkinId);
        Map<String, Object> response = courseService.getCheckinStatistics(checkinId);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取课程签到统计信息
     * 
     * @param courseId 课程ID
     * @return 签到统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#courseId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getCourseStatistics(@RequestParam String courseId) {
        log.info("获取课程签到统计: courseId={}", courseId);
        
        Map<String, Object> statistics = courseService.getCourseStatistics(courseId);
        return ApiResponse.success(statistics);
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