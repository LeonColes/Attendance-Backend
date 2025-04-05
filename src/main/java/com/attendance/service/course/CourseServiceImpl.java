package com.attendance.service.course;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.entity.Course;
import com.attendance.model.entity.CourseUser;
import com.attendance.model.entity.Record;
import com.attendance.model.entity.User;
import com.attendance.repository.course.CourseRepository;
import com.attendance.repository.course.CourseUserRepository;
import com.attendance.repository.record.RecordRepository;
import com.attendance.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 课程/签到任务服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final CourseUserRepository courseUserRepository;
    
    @Override
    public CourseDTO getCourse(String id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new BusinessException("课程或签到任务不存在"));
        
        User creator = userRepository.findById(course.getCreatorId())
            .orElse(null);
        
        CourseDTO dto = convertToDTO(course, creator);
        
        // 如果是课程，添加成员数量
        if (SystemConstants.CourseType.COURSE.equals(course.getType())) {
            dto.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(course.getId()));
        }
        
        return dto;
    }
    
    @Override
    public List<CourseDTO> getAllCourses() {
        List<Course> courses = courseRepository.findByType(SystemConstants.CourseType.COURSE);
        
        return courses.stream()
            .map(course -> {
                User creator = userRepository.findById(course.getCreatorId())
                    .orElse(null);
                CourseDTO dto = convertToDTO(course, creator);
                dto.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(course.getId()));
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<CourseDTO> getMyCourses() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 获取用户创建的课程
        List<Course> createdCourses = courseRepository.findByCreatorIdAndType(
            currentUser.getId(), SystemConstants.CourseType.COURSE);
        
        // 获取用户参与的课程
        List<CourseUser> joinedCourseUsers = courseUserRepository.findByUserId(currentUser.getId());
        List<String> joinedCourseIds = joinedCourseUsers.stream()
            .map(CourseUser::getCourseId)
            .collect(Collectors.toList());
        
        List<Course> joinedCourses = courseRepository.findAllById(joinedCourseIds);
        
        // 合并两个列表（确保没有重复）
        joinedCourses.removeAll(createdCourses);
        createdCourses.addAll(joinedCourses);
        
        return createdCourses.stream()
            .map(course -> {
                User creator = userRepository.findById(course.getCreatorId())
                    .orElse(null);
                CourseDTO dto = convertToDTO(course, creator);
                dto.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(course.getId()));
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<CourseDTO> getCourseCheckinTasks(String courseId) {
        Course parentCourse = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        List<Course> checkinTasks = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN);
        
        User creator = userRepository.findById(parentCourse.getCreatorId())
            .orElse(null);
        
        return checkinTasks.stream()
            .map(checkin -> {
                CourseDTO dto = convertToDTO(checkin, creator);
                dto.setParentCourseName(parentCourse.getName());
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public CourseDTO createCourse(String name, String description, LocalDate startDate, LocalDate endDate) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User creator = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证日期
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        
        // 生成唯一的课程邀请码
        String code = generateUniqueCode();
        
        // 创建课程
        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        course.setCreatorId(creator.getId());
        course.setCode(code);
        course.setStartDate(startDate);
        course.setEndDate(endDate);
        course.setType(SystemConstants.CourseType.COURSE);
        course.setStatus(SystemConstants.CourseStatus.ACTIVE);
        
        Course savedCourse = courseRepository.save(course);
        
        // 创建课程-用户关联（创建者自动成为成员）
        CourseUser courseUser = new CourseUser();
        courseUser.setCourseId(savedCourse.getId());
        courseUser.setUserId(creator.getId());
        courseUser.setRole(SystemConstants.CourseUserRole.CREATOR);
        courseUser.setJoinedAt(LocalDateTime.now());
        courseUser.setJoinMethod(SystemConstants.JoinMethod.CREATED);
        courseUser.setActive(true);
        
        courseUserRepository.save(courseUser);
        
        CourseDTO courseDTO = convertToDTO(savedCourse, creator);
        courseDTO.setMemberCount(1); // 创建者默认为第一个成员
        
        return courseDTO;
    }
    
    @Override
    @Transactional
    public CourseDTO createCheckinTask(String parentCourseId, String name, String description, 
                                      LocalDateTime startTime, LocalDateTime endTime, 
                                      String checkinType, String verifyParams) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User creator = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证父课程
        Course parentCourse = courseRepository.findById(parentCourseId)
            .orElseThrow(() -> new BusinessException("父课程不存在"));
        
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 验证创建者权限
        if (!parentCourse.getCreatorId().equals(creator.getId())) {
            // 检查用户是否为课程助教
            boolean isAssistant = courseUserRepository.findByCourseIdAndUserId(parentCourseId, creator.getId())
                .map(cu -> SystemConstants.CourseUserRole.ASSISTANT.equals(cu.getRole()))
                .orElse(false);
                
            if (!isAssistant) {
                throw new BusinessException("只有课程创建者或助教才能创建签到任务");
            }
        }
        
        // 验证时间
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        
        if (endTime != null && endTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("结束时间不能早于当前时间");
        }
        
        // 验证签到类型
        if (!SystemConstants.CheckInType.isValidType(checkinType)) {
            throw new BusinessException("无效的签到类型");
        }
        
        // 为QR_CODE类型生成唯一码
        String finalVerifyParams = verifyParams;
        if (SystemConstants.CheckInType.QR_CODE.equals(checkinType) && 
            (verifyParams == null || verifyParams.isEmpty())) {
            finalVerifyParams = generateQRCodeVerifyParams();
        }
        
        // 创建签到任务
        Course checkinTask = new Course();
        checkinTask.setName(name);
        checkinTask.setDescription(description);
        checkinTask.setCreatorId(creator.getId());
        checkinTask.setParentCourseId(parentCourseId);
        checkinTask.setType(SystemConstants.CourseType.CHECKIN);
        checkinTask.setCheckinStartTime(startTime);
        checkinTask.setCheckinEndTime(endTime);
        checkinTask.setCheckinType(checkinType);
        checkinTask.setVerifyParams(finalVerifyParams);
        
        // 生成一个唯一邀请码(仅用于确保数据库一致性)
        checkinTask.setCode(generateUniqueCode());
        
        // 设置初始状态
        checkinTask.setStatus(SystemConstants.TaskStatus.CREATED);
        
        // 如果开始时间已到，立即激活任务
        if (startTime != null && (startTime.isBefore(LocalDateTime.now()) || startTime.isEqual(LocalDateTime.now()))) {
            checkinTask.setStatus(SystemConstants.TaskStatus.ACTIVE);
        }
        
        Course savedCheckinTask = courseRepository.save(checkinTask);
        
        CourseDTO courseDTO = convertToDTO(savedCheckinTask, creator);
        courseDTO.setParentCourseName(parentCourse.getName());
        
        return courseDTO;
    }
    
    @Override
    @Transactional
    public CourseDTO updateCourseStatus(String id, String status) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new BusinessException("课程或签到任务不存在"));
        
        if (SystemConstants.CourseType.COURSE.equals(course.getType())) {
            // 课程状态验证
            if (!SystemConstants.CourseStatus.isValidStatus(status)) {
                throw new BusinessException("非法的课程状态");
            }
        } else {
            // 签到任务状态验证
            if (!SystemConstants.TaskStatus.isValidStatus(status)) {
                throw new BusinessException("非法的签到任务状态");
            }
        }
        
        course.setStatus(status);
        Course updatedCourse = courseRepository.save(course);
        
        User creator = userRepository.findById(updatedCourse.getCreatorId())
            .orElse(null);
        
        return convertToDTO(updatedCourse, creator);
    }
    
    @Override
    @Transactional
    public CourseUserDTO joinCourseByCode(String code) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 查找课程
        Course course = courseRepository.findByCode(code)
            .orElseThrow(() -> new BusinessException("课程不存在或邀请码无效"));
        
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("邀请码不是有效的课程");
        }
        
        // 检查课程状态
        if (!SystemConstants.CourseStatus.ACTIVE.equals(course.getStatus())) {
            throw new BusinessException("该课程当前不可加入");
        }
        
        // 检查用户是否已加入该课程
        if (courseUserRepository.existsByCourseIdAndUserId(course.getId(), currentUser.getId())) {
            throw new BusinessException("您已经是该课程的成员");
        }
        
        // 创建课程-用户关联
        CourseUser courseUser = new CourseUser();
        courseUser.setCourseId(course.getId());
        courseUser.setUserId(currentUser.getId());
        courseUser.setRole(SystemConstants.CourseUserRole.STUDENT);
        courseUser.setJoinedAt(LocalDateTime.now());
        courseUser.setJoinMethod(SystemConstants.JoinMethod.CODE);
        courseUser.setActive(true);
        
        courseUserRepository.save(courseUser);
        
        // 返回课程用户关联信息
        CourseUserDTO dto = new CourseUserDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());
        dto.setUserId(currentUser.getId());
        dto.setUsername(currentUser.getUsername());
        dto.setUserFullName(currentUser.getFullName());
        dto.setRole(SystemConstants.CourseUserRole.STUDENT);
        dto.setJoinedAt(LocalDateTime.now());
        
        return dto;
    }
    
    @Override
    @Transactional
    public CourseUserDTO joinCourseByQRCode(String qrCode) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 将二维码解析为课程ID
        // 注：实际应用中可能需要更复杂的解析和验证逻辑
        String courseId = qrCode;
        
        // 查找课程
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在或二维码无效"));
        
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("二维码不是有效的课程");
        }
        
        // 检查课程状态
        if (!SystemConstants.CourseStatus.ACTIVE.equals(course.getStatus())) {
            throw new BusinessException("该课程当前不可加入");
        }
        
        // 检查用户是否已加入该课程
        if (courseUserRepository.existsByCourseIdAndUserId(course.getId(), currentUser.getId())) {
            throw new BusinessException("您已经是该课程的成员");
        }
        
        // 创建课程-用户关联
        CourseUser courseUser = new CourseUser();
        courseUser.setCourseId(course.getId());
        courseUser.setUserId(currentUser.getId());
        courseUser.setRole(SystemConstants.CourseUserRole.STUDENT);
        courseUser.setJoinedAt(LocalDateTime.now());
        courseUser.setJoinMethod(SystemConstants.JoinMethod.QR_CODE);
        courseUser.setActive(true);
        
        courseUserRepository.save(courseUser);
        
        // 返回课程用户关联信息
        CourseUserDTO dto = new CourseUserDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());
        dto.setUserId(currentUser.getId());
        dto.setUsername(currentUser.getUsername());
        dto.setUserFullName(currentUser.getFullName());
        dto.setRole(SystemConstants.CourseUserRole.STUDENT);
        dto.setJoinedAt(LocalDateTime.now());
        
        return dto;
    }
    
    @Override
    @Transactional
    public int addCourseMembers(String courseId, List<String> userIds, String role) {
        // 验证课程
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 验证角色
        if (!SystemConstants.CourseUserRole.isValidRole(role)) {
            throw new BusinessException("无效的课程成员角色");
        }
        
        // 验证当前用户是否有权添加成员
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 只有创建者或助教可以添加成员
        if (!course.getCreatorId().equals(currentUser.getId())) {
            boolean isAssistant = courseUserRepository.findByCourseIdAndUserId(courseId, currentUser.getId())
                .map(cu -> SystemConstants.CourseUserRole.ASSISTANT.equals(cu.getRole()))
                .orElse(false);
                
            if (!isAssistant) {
                throw new BusinessException("只有课程创建者或助教才能添加成员");
            }
        }
        
        // 批量添加用户
        int successCount = 0;
        for (String userId : userIds) {
            // 验证用户是否存在
            if (!userRepository.existsById(userId)) {
                continue;
            }
            
            // 检查用户是否已加入该课程
            if (courseUserRepository.existsByCourseIdAndUserId(courseId, userId)) {
                continue;
            }
            
            // 创建课程-用户关联
            CourseUser courseUser = new CourseUser();
            courseUser.setCourseId(courseId);
            courseUser.setUserId(userId);
            courseUser.setRole(role);
            courseUser.setJoinedAt(LocalDateTime.now());
            courseUser.setJoinMethod(SystemConstants.JoinMethod.ADDED);
            courseUser.setActive(true);
            
            courseUserRepository.save(courseUser);
            successCount++;
        }
        
        return successCount;
    }
    
    @Override
    public int getCourseMemberCount(String courseId) {
        return (int) courseUserRepository.countByCourseIdAndActiveTrue(courseId);
    }
    
    @Override
    @Transactional
    public boolean submitCheckin(String checkinId, String verifyData, String location, String device) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 查找签到任务
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        // 验证签到任务状态
        if (!SystemConstants.TaskStatus.ACTIVE.equals(checkinTask.getStatus())) {
            throw new BusinessException("签到任务当前不可用");
        }
        
        // 验证签到时间
        LocalDateTime now = LocalDateTime.now();
        if (checkinTask.getCheckinStartTime() != null && 
            now.isBefore(checkinTask.getCheckinStartTime())) {
            throw new BusinessException("签到尚未开始");
        }
        
        if (checkinTask.getCheckinEndTime() != null && 
            now.isAfter(checkinTask.getCheckinEndTime())) {
            throw new BusinessException("签到已结束");
        }
        
        // 检查用户是否已签到
        if (recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkinId).isPresent()) {
            throw new BusinessException("您已经签到过该任务");
        }
        
        // 检查用户是否为课程成员
        if (checkinTask.getParentCourseId() != null) {
            boolean isMember = courseUserRepository.existsByCourseIdAndUserId(
                checkinTask.getParentCourseId(), currentUser.getId());
                
            if (!isMember) {
                throw new BusinessException("您不是该课程的成员，无法签到");
            }
        }
        
        // 根据签到类型验证签到数据
        boolean verifySuccess = verifyCheckinData(checkinTask, verifyData, location);
        
        if (!verifySuccess) {
            return false;
        }
        
        // 创建签到记录
        Record record = new Record();
        record.setUserId(currentUser.getId());
        record.setCourseId(checkinId);
        record.setParentCourseId(checkinTask.getParentCourseId());
        record.setLocation(location);
        record.setDevice(device);
        record.setVerifyMethod(checkinTask.getCheckinType());
        record.setVerifyData(verifyData);
        record.setCheckInTime(now);
        
        // 判断签到状态
        String status = SystemConstants.RecordStatus.NORMAL;
        
        // 如果接近结束时间，标记为迟到
        if (checkinTask.getCheckinEndTime() != null) {
            long totalDuration = java.time.Duration.between(
                checkinTask.getCheckinStartTime(), 
                checkinTask.getCheckinEndTime()
            ).toMinutes();
            
            long elapsedDuration = java.time.Duration.between(
                checkinTask.getCheckinStartTime(), 
                now
            ).toMinutes();
            
            // 如果已经过去了签到时间的80%，标记为迟到
            if (elapsedDuration > totalDuration * 0.8) {
                status = SystemConstants.RecordStatus.LATE;
            }
        }
        
        record.setStatus(status);
        recordRepository.save(record);
        
        return true;
    }
    
    @Override
    public String generateCheckinCode(String checkinId) {
        // 查找签到任务
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        if (!SystemConstants.CheckInType.QR_CODE.equals(checkinTask.getCheckinType())) {
            throw new BusinessException("该签到任务不是二维码签到类型");
        }
        
        // 如果没有验证参数或需要刷新，生成新的
        if (checkinTask.getVerifyParams() == null || checkinTask.getVerifyParams().isEmpty()) {
            String code = generateQRCodeVerifyParams();
            checkinTask.setVerifyParams(code);
            courseRepository.save(checkinTask);
            return extractCodeFromVerifyParams(code);
        }
        
        return extractCodeFromVerifyParams(checkinTask.getVerifyParams());
    }
    
    // 生成唯一的邀请码 (6位字母数字组合)
    private String generateUniqueCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 检查是否已存在
        if (courseRepository.findByCode(code.toString()).isPresent()) {
            return generateUniqueCode(); // 递归直到生成唯一码
        }
        
        return code.toString();
    }
    
    // 生成二维码签到的验证参数
    private String generateQRCodeVerifyParams() {
        // 使用固定token
        return "{\"code\":\"{{token}}\"}";
    }
    
    // 从验证参数中提取二维码
    private String extractCodeFromVerifyParams(String verifyParams) {
        // 简单实现，实际项目中应该使用JSON库解析
        if (verifyParams.contains("\"code\":")) {
            String code = verifyParams.split("\"code\":\"")[1];
            return code.substring(0, code.indexOf("\""));
        }
        return "";
    }
    
    // 验证签到数据
    private boolean verifyCheckinData(Course checkinTask, String verifyData, String location) {
        String checkinType = checkinTask.getCheckinType();
        
        if (SystemConstants.CheckInType.QR_CODE.equals(checkinType)) {
            return verifyQRCode(checkinTask.getVerifyParams(), verifyData);
        } else if (SystemConstants.CheckInType.LOCATION.equals(checkinType)) {
            return verifyLocation(checkinTask.getVerifyParams(), location);
        } else if (SystemConstants.CheckInType.WIFI.equals(checkinType)) {
            return verifyWifi(checkinTask.getVerifyParams(), verifyData);
        } else if (SystemConstants.CheckInType.MANUAL.equals(checkinType)) {
            return true; // 手动签到直接返回成功
        }
        
        return false;
    }
    
    // 验证二维码
    private boolean verifyQRCode(String taskVerifyParams, String userVerifyData) {
        String taskCode = extractCodeFromVerifyParams(taskVerifyParams);
        String userCode = extractCodeFromVerifyParams(userVerifyData);
        
        return taskCode.equals(userCode);
    }
    
    // 验证位置
    private boolean verifyLocation(String taskVerifyParams, String userLocation) {
        // 实际项目中应该使用JSON库和地理位置库计算距离
        // 这里做简单模拟
        return true;
    }
    
    // 验证WiFi
    private boolean verifyWifi(String taskVerifyParams, String userVerifyData) {
        // 实际项目中应该使用JSON库比较SSID和BSSID
        // 这里做简单模拟
        return true;
    }
    
    // 将实体转换为DTO
    private CourseDTO convertToDTO(Course course, User creator) {
        CourseDTO dto = CourseDTO.builder()
            .id(course.getId())
            .name(course.getName())
            .description(course.getDescription())
            .creatorId(course.getCreatorId())
            .type(course.getType())
            .status(course.getStatus())
            .createdAt(course.getCreatedAt())
            .updatedAt(course.getUpdatedAt())
            .build();
        
        // 填充创建者信息
        if (creator != null) {
            dto.setCreatorUsername(creator.getUsername());
            dto.setCreatorFullName(creator.getFullName());
        }
        
        // 根据类型填充不同字段
        if (SystemConstants.CourseType.COURSE.equals(course.getType())) {
            dto.setCode(course.getCode());
            dto.setStartDate(course.getStartDate());
            dto.setEndDate(course.getEndDate());
            // 在这里不设置成员数量，会在调用方统一设置
        } else if (SystemConstants.CourseType.CHECKIN.equals(course.getType())) {
            dto.setParentCourseId(course.getParentCourseId());
            dto.setCheckinStartTime(course.getCheckinStartTime());
            dto.setCheckinEndTime(course.getCheckinEndTime());
            dto.setCheckinType(course.getCheckinType());
            dto.setVerifyParams(course.getVerifyParams());
            
            // 如果是二维码类型，隐藏验证参数以增强安全性
            if (SystemConstants.CheckInType.QR_CODE.equals(course.getCheckinType())) {
                dto.setVerifyParams("{\"code\":\"[PROTECTED]\"}");
            }
        }
        
        return dto;
    }
} 