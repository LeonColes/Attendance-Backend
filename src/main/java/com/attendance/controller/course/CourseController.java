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
import java.time.LocalDate;
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
     * 获取课程下的所有签到任务列表（老师查看课程的签到任务列表，学生查看自己在任务中的签到状态）
     * 
     * @param courseId 课程ID
     * @param requestDTO 分页请求参数
     * @return 签到任务列表和状态
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
    @PostMapping("/members/list")
    @PreAuthorize("@courseSecurityService.isCourseMember(#courseId)")
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
     * 提交签到
     * 
     * 支持多种签到方式：
     * - QR_CODE: 二维码签到，可选提供verifyData，默认使用checkinId
     * - LOCATION: 位置签到，需要提供location参数（经纬度坐标）
     * - WIFI: WIFI签到，需要提供verifyData参数（WIFI信息）
     * 
     * 设备信息会被记录用于签到分析和安全审计，推荐始终提供此信息
     * 
     * @param request 签到请求，包含签到任务ID和验证方式等信息
     * @return 签到结果，包含签到状态和时间等信息
     */
    @PostMapping("/attendance/check-in")
    public ApiResponse<CourseRecordDTO> submitCheckIn(@Valid @RequestBody CheckInRequest request) {
        log.info("提交签到: checkinId={}, verifyMethod={}, device={}", 
                request.getCheckinId(), request.getVerifyMethod(), request.getDevice());
        CourseRecordDTO record = courseService.submitCheckIn(
                request.getCheckinId(),
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
     * 老师查看特定签到任务的所有学生签到记录
     * 
     * @param checkinId 签到任务ID
     * @param requestDTO 分页请求参数
     * @return 学生签到记录列表
     */
    @PostMapping("/attendance/record/list")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getAttendanceRecordList(
            @RequestParam String checkinId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取签到任务的学生记录(教师视角): checkinId={}, page={}, size={}", 
            checkinId, requestDTO.getPage(), requestDTO.getSize());
        Map<String, Object> response = courseService.getCheckinDetail(
            checkinId, requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
    }
    
    /**
     * 学生查看自己在课程中的所有签到状态
     * 
     * @param courseId 课程ID
     * @param requestDTO 分页请求参数
     * @return 学生签到状态列表
     */
    @PostMapping("/attendance/record/status")
    public ApiResponse<Map<String, Object>> getAttendanceRecordStatus(
            @RequestParam String courseId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取学生在课程中的签到状态: courseId={}, page={}, size={}", 
            courseId, requestDTO.getPage(), requestDTO.getSize());
            
        // 获取签到任务列表和学生的签到状态
        Map<String, Object> response = courseService.getAttendanceList(
            courseId, requestDTO.getPage(), requestDTO.getSize());
        return ApiResponse.success(response);
    }
    
    /**
     * 获取签到任务的统计信息
     * 
     * @param checkinId 签到任务ID
     * @param requestDTO 分页请求参数
     * @return 签到统计信息
     */
    @PostMapping("/attendance/record/statistics")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getAttendanceRecordStatistics(
            @RequestParam String checkinId,
            @Valid @RequestBody PageRequestDTO requestDTO) {
        log.info("获取签到统计信息: checkinId={}", checkinId);
        Map<String, Object> response = courseService.getCheckinStatistics(checkinId);
        return ApiResponse.success(response);
    }
    
    /**
     * 删除课程（逻辑删除）
     * 只有课程创建者或管理员可以删除课程
     * 
     * @param courseId 课程ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#courseId) or hasRole('ADMIN')")
    public ApiResponse<Boolean> deleteCourse(@RequestParam String courseId) {
        log.info("删除课程请求(逻辑删除): courseId={}", courseId);
        boolean result = courseService.deleteCourse(courseId);
        return ApiResponse.success("课程删除成功", result);
    }
    
    /**
     * 删除签到任务（逻辑删除）
     * 只有签到任务创建者或管理员可以删除签到任务
     * 
     * @param checkinId 签到任务ID
     * @return 删除结果
     */
    @PostMapping("/attendance/delete")
    @PreAuthorize("@courseSecurityService.isCheckinCreator(#checkinId) or hasRole('ADMIN')")
    public ApiResponse<Boolean> deleteAttendance(@RequestParam String checkinId) {
        log.info("删除签到任务请求(逻辑删除): checkinId={}", checkinId);
        boolean result = courseService.deleteCheckinTask(checkinId);
        return ApiResponse.success("签到任务删除成功", result);
    }
    
    /**
     * 移除课程成员
     * 只有课程创建者或管理员可以移除成员
     * 
     * @param request 移除成员请求
     * @return 移除结果
     */
    @PostMapping("/members/remove")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#request.courseId) or hasRole('ADMIN')")
    public ApiResponse<Boolean> removeMember(@Valid @RequestBody RemoveMemberRequest request) {
        log.info("移除课程成员请求: courseId={}, userId={}", request.getCourseId(), request.getUserId());
        boolean result = courseService.removeCourseMember(
            request.getCourseId(), 
            request.getUserId(), 
            request.getReason()
        );
        return ApiResponse.success("成员移除成功", result);
    }
    
    /**
     * 更新课程信息
     * 只有课程创建者或管理员可以更新课程
     * 
     * @param request 更新课程请求
     * @return 更新后的课程
     */
    @PostMapping("/update")
    @PreAuthorize("@courseSecurityService.isCourseCreator(#request.courseId) or hasRole('ADMIN')")
    public ApiResponse<CourseDTO> updateCourse(@Valid @RequestBody UpdateCourseRequest request) {
        log.info("更新课程请求: {}", request);
        
        CourseDTO courseDTO = courseService.updateCourse(
            request.getCourseId(),
            request.getName(),
            request.getDescription(),
            request.getStartDate(),
            request.getEndDate()
        );
        
        return ApiResponse.success("课程更新成功", courseDTO);
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
        private String checkinId;
        
        /**
         * 验证方式 (如QR_CODE, LOCATION, WIFI等)
         */
        @NotBlank(message = "验证方式不能为空")
        private String verifyMethod;
        
        /**
         * 位置信息 (可选)，例如经纬度坐标 "118.803,32.064"
         */
        private String location;
        
        /**
         * 设备信息，例如设备类型、浏览器、操作系统等
         * 推荐格式: "设备类型/浏览器/操作系统"，如 "Mobile/Chrome/Android" 或 "Desktop/Firefox/Windows"
         */
        private String device;
        
        /**
         * 验证数据 (可选)，根据验证方式不同可能包含不同内容
         * - QR_CODE: 可以为空，会默认使用checkinId
         * - LOCATION: 可包含精度信息
         * - WIFI: 可包含WiFi名称或MAC地址等
         */
        private String verifyData;
    }
    
    /**
     * 更新课程请求类
     */
    @Data
    public static class UpdateCourseRequest {
        
        /**
         * 课程ID
         */
        @NotBlank(message = "课程ID不能为空")
        private String courseId;
        
        /**
         * 课程名称
         */
        private String name;
        
        /**
         * 课程描述
         */
        private String description;
        
        /**
         * 开始日期
         */
        private LocalDate startDate;
        
        /**
         * 结束日期
         */
        private LocalDate endDate;
    }
} 