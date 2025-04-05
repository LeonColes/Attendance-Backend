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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
        
        List<Course> courses = new ArrayList<>();
        
        // 检查用户角色
        boolean isTeacher = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + SystemConstants.UserRole.TEACHER));
        
        boolean isStudent = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + SystemConstants.UserRole.STUDENT));
        
        // 教师只能看到自己创建的课程
        if (isTeacher) {
            courses = courseRepository.findByCreatorIdAndType(
                currentUser.getId(), SystemConstants.CourseType.COURSE);
        } 
        // 学生只能看到自己加入的课程
        else if (isStudent) {
            List<CourseUser> joinedCourseUsers = courseUserRepository.findByUserIdAndActiveTrue(currentUser.getId());
            List<String> joinedCourseIds = joinedCourseUsers.stream()
                .map(CourseUser::getCourseId)
                .collect(Collectors.toList());
            
            if (!joinedCourseIds.isEmpty()) {
                courses = courseRepository.findAllById(joinedCourseIds).stream()
                    .filter(course -> SystemConstants.CourseType.COURSE.equals(course.getType()))
                    .collect(Collectors.toList());
            }
        }
        // 管理员可以看到所有课程
        else {
            courses = courseRepository.findByType(SystemConstants.CourseType.COURSE);
        }
        
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
    public Map<String, Object> getMyCourses(int page, int size) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 创建分页请求，添加按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<Course> coursePageResult = null;
        
        // 检查用户角色
        boolean isTeacher = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + SystemConstants.UserRole.TEACHER));
        
        boolean isStudent = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + SystemConstants.UserRole.STUDENT));
        
        // 教师只能看到自己创建的课程
        if (isTeacher) {
            coursePageResult = courseRepository.findByCreatorIdAndType(
                currentUser.getId(), SystemConstants.CourseType.COURSE, pageable);
        } 
        // 学生只能看到自己加入的活跃课程
        else if (isStudent) {
            List<CourseUser> joinedCourseUsers = courseUserRepository.findByUserIdAndActiveTrue(currentUser.getId());
            List<String> joinedCourseIds = joinedCourseUsers.stream()
                .map(CourseUser::getCourseId)
                .collect(Collectors.toList());
            
            if (!joinedCourseIds.isEmpty()) {
                // 这里由于没有直接的分页方法，先获取指定ID的所有课程，然后手动分页
                Page<Course> allMatchingCourses = courseRepository.findByTypeAndIdIn(
                    SystemConstants.CourseType.COURSE, joinedCourseIds, pageable);
                coursePageResult = allMatchingCourses;
            } else {
                // 如果学生没有加入任何课程，返回空分页结果
                coursePageResult = Page.empty(pageable);
            }
        }
        // 管理员可以看到所有课程
        else {
            coursePageResult = courseRepository.findByType(SystemConstants.CourseType.COURSE, pageable);
        }
        
        // 将课程列表转换为DTO
        List<CourseDTO> courseDTOs = coursePageResult.getContent().stream()
            .map(course -> {
                User creator = userRepository.findById(course.getCreatorId()).orElse(null);
                CourseDTO dto = convertToDTO(course, creator);
                dto.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(course.getId()));
                return dto;
            })
            .collect(Collectors.toList());
        
        // 创建分页结果
        Map<String, Object> response = new HashMap<>();
        response.put("courses", courseDTOs);
        response.put("currentPage", coursePageResult.getNumber());
        response.put("totalItems", coursePageResult.getTotalElements());
        response.put("totalPages", coursePageResult.getTotalPages());
        
        return response;
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
    public Map<String, Object> getCourseCheckinTasks(String courseId, int page, int size) {
        Course parentCourse = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 创建分页请求，添加按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        
        // 获取分页数据
        Page<Course> checkinTasksPage = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN, pageable);
        
        User creator = userRepository.findById(parentCourse.getCreatorId())
            .orElse(null);
        
        // 转换为DTO列表
        List<CourseDTO> checkinTaskDTOs = checkinTasksPage.getContent().stream()
            .map(checkin -> {
                CourseDTO dto = convertToDTO(checkin, creator);
                dto.setParentCourseName(parentCourse.getName());
                return dto;
            })
            .collect(Collectors.toList());
        
        // 创建分页结果
        Map<String, Object> response = new HashMap<>();
        response.put("checkinTasks", checkinTaskDTOs);
        response.put("currentPage", checkinTasksPage.getNumber());
        response.put("totalItems", checkinTasksPage.getTotalElements());
        response.put("totalPages", checkinTasksPage.getTotalPages());
        
        return response;
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
        
        // 查找父课程
        Course parentCourse = courseRepository.findById(parentCourseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        // 验证用户是否为课程创建者
        if (!parentCourse.getCreatorId().equals(creator.getId()) && 
            !isAdmin(authentication)) {
            throw new BusinessException("只有课程创建者可以创建签到任务");
        }
        
        // 验证课程类型
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("只能在普通课程下创建签到任务");
        }
        
        // 验证签到时间
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
        
        // 对于QR_CODE类型，验证参数稍后设置，需要保存后获取ID
        if (!SystemConstants.CheckInType.QR_CODE.equals(checkinType)) {
            checkinTask.setVerifyParams(verifyParams);
        }
        
        // 生成一个唯一邀请码(仅用于确保数据库一致性)
        checkinTask.setCode(generateUniqueCode());
        
        // 设置初始状态
        checkinTask.setStatus(SystemConstants.TaskStatus.CREATED);
        
        // 如果开始时间已到，立即激活任务
        if (startTime != null && (startTime.isBefore(LocalDateTime.now()) || startTime.isEqual(LocalDateTime.now()))) {
            checkinTask.setStatus(SystemConstants.TaskStatus.ACTIVE);
        }
        
        // 保存任务获取ID
        Course savedCheckinTask = courseRepository.save(checkinTask);
        
        // 对于QR_CODE类型，设置验证参数为任务ID
        if (SystemConstants.CheckInType.QR_CODE.equals(checkinType)) {
            String taskVerifyParams = "CHECKIN:" + savedCheckinTask.getId();
            savedCheckinTask.setVerifyParams(taskVerifyParams);
            savedCheckinTask = courseRepository.save(savedCheckinTask);
        }
        
        CourseDTO courseDTO = convertToDTO(savedCheckinTask, creator);
        courseDTO.setParentCourseName(parentCourse.getName());
        
        return courseDTO;
    }
    
    // 检查是否为管理员
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        // 检查课程类型
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("无效的课程");
        }
        
        // 检查教师是否尝试加入自己创建的课程
        if (course.getCreatorId().equals(currentUser.getId())) {
            throw new BusinessException("您是该课程的创建者，无需加入");
        }
        
        // 检查用户角色 - 只有学生可以加入课程
        boolean isStudent = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + SystemConstants.UserRole.STUDENT));
            
        if (!isStudent) {
            throw new BusinessException("只有学生可以加入课程");
        }
        
        // 检查是否已加入该课程
        Optional<CourseUser> existingMember = courseUserRepository.findByCourseIdAndUserId(course.getId(), currentUser.getId());
        if (existingMember.isPresent()) {
            CourseUser existing = existingMember.get();
            
            // 如果之前已退出，则重新激活
            if (!existing.getActive()) {
                existing.setActive(true);
                existing.setJoinedAt(LocalDateTime.now());
                CourseUser updated = courseUserRepository.save(existing);
                
                return convertToCourseUserDTO(updated, currentUser, course);
            }
            
            throw new BusinessException("您已加入该课程");
        }
        
        // 创建课程-用户关系
        CourseUser courseUser = new CourseUser();
        courseUser.setCourseId(course.getId());
        courseUser.setUserId(currentUser.getId());
        courseUser.setRole(SystemConstants.UserRole.STUDENT);
        courseUser.setJoinedAt(LocalDateTime.now());
        courseUser.setJoinMethod(SystemConstants.JoinMethod.CODE);
        courseUser.setActive(true);
        
        CourseUser saved = courseUserRepository.save(courseUser);
        
        return convertToCourseUserDTO(saved, currentUser, course);
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
        
        // 检查用户是否为课程创建者
        if (course.getCreatorId().equals(currentUser.getId())) {
            throw new BusinessException("您是该课程的创建者，无需加入");
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
        dto.setJoinedAt(courseUser.getJoinedAt());
        
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
    public boolean removeCourseMember(String courseId, String userId, String reason) {
        log.info("移除课程成员: courseId={}, userId={}, reason={}", courseId, userId, reason);
        
        // 检查课程是否存在
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + courseId));
        
        // 检查用户是否是课程成员
        CourseUser courseUser = courseUserRepository.findByCourseIdAndUserId(courseId, userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不是课程成员"));
        
        // 不能移除课程创建者
        if (course.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("不能移除课程创建者");
        }
        
        // 移除课程成员
        courseUserRepository.delete(courseUser);
        
        // 记录日志(如果有需要)
        if (reason != null && !reason.trim().isEmpty()) {
            log.info("移除课程成员原因: {}", reason);
        }
        
        return true;
    }
    
    @Override
    @Transactional
    public Map<String, Object> submitCheckin(String taskId, String verifyData, String verifyMethod,
                                            String location, String device) {
        // 验证签到任务
        Course checkinTask = courseRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        if (!SystemConstants.TaskStatus.ACTIVE.equals(checkinTask.getStatus())) {
            throw new BusinessException("签到任务未开始或已结束");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证用户是否为课程成员
        if (checkinTask.getParentCourseId() != null) {
            boolean isMember = courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(
                checkinTask.getParentCourseId(), currentUser.getId());
            
            if (!isMember) {
                throw new BusinessException("您不是该课程的成员，无法签到");
            }
        }
        
        // 检查是否重复签到
        Optional<Record> existingRecord = recordRepository.findByUserIdAndCourseId(currentUser.getId(), taskId);
        if (existingRecord.isPresent()) {
            throw new BusinessException("您已经签到过了");
        }
        
        // 根据签到类型验证签到数据
        boolean verified = false;
        String validationError = "";
        
        try {
            if (SystemConstants.CheckInType.QR_CODE.equals(checkinTask.getCheckinType())) {
                verified = verifyQRCode(checkinTask, verifyData);
            } 
            else if (SystemConstants.CheckInType.LOCATION.equals(checkinTask.getCheckinType())) {
                verified = verifyLocation(checkinTask, location);
                if (!verified) validationError = "位置验证失败";
            } 
            else if (SystemConstants.CheckInType.WIFI.equals(checkinTask.getCheckinType())) {
                verified = verifyWifi(checkinTask, verifyData);
                if (!verified) validationError = "WiFi验证失败";
            } 
            else if (SystemConstants.CheckInType.MANUAL.equals(checkinTask.getCheckinType())) {
                // 手动签到直接通过
                verified = true;
            }
        } catch (Exception e) {
            log.error("签到验证过程发生异常", e);
            validationError = "验证过程发生错误: " + e.getMessage();
        }
        
        // 签到未验证通过
        if (!verified) {
            Map<String, Object> result = new HashMap<>();
            result.put("checkinId", taskId);
            result.put("success", false);
            result.put("error", validationError.isEmpty() ? "验证未通过" : validationError);
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
        
        // 处理签到时间，确定签到状态
        LocalDateTime now = LocalDateTime.now();
        String status = SystemConstants.RecordStatus.NORMAL;
        
        // 如果超出签到时间但在结束时间内，标记为迟到
        if (checkinTask.getCheckinStartTime() != null && now.isAfter(checkinTask.getCheckinStartTime().plusMinutes(10))) {
            status = SystemConstants.RecordStatus.LATE;
        }
        
        // 创建签到记录
        Record record = new Record();
        record.setUserId(currentUser.getId());
        record.setCourseId(taskId);
        record.setParentCourseId(checkinTask.getParentCourseId());
        record.setStatus(status);
        record.setCheckInTime(now);
        record.setLocation(location);
        record.setDevice(device);
        record.setVerifyMethod(verifyMethod);
        record.setVerifyData(verifyData);
        
        recordRepository.save(record);
        
        // 返回签到结果
        Map<String, Object> result = new HashMap<>();
        result.put("checkinId", taskId);
        result.put("status", status);
        result.put("checkInTime", now);
        result.put("success", true);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * 验证二维码签到数据
     */
    private boolean verifyQRCode(Course checkinTask, String verifyData) {
        if (verifyData == null || verifyData.isEmpty()) {
            log.warn("签到验证失败: 验证数据为空");
            return false;
        }
        
        // 获取任务ID和存储的验证参数
        String checkinId = checkinTask.getId();
        
        log.info("签到验证: 任务ID={}, 提交验证数据={}", checkinId, verifyData);
        
        // 直接对比任务ID
        if (checkinId.equals(verifyData)) {
            log.info("验证通过: 提交ID与任务ID匹配");
            return true;
        }
        
        log.warn("验证未通过: 提交ID与任务ID不匹配 [提交ID={}, 任务ID={}]", verifyData, checkinId);
        
        // 尝试作为不同任务ID处理
        try {
            // 查找verifyData中ID对应的签到任务
            Course scannedTask = courseRepository.findById(verifyData).orElse(null);
            if (scannedTask != null && 
                SystemConstants.CourseType.CHECKIN.equals(scannedTask.getType()) &&
                SystemConstants.TaskStatus.ACTIVE.equals(scannedTask.getStatus())) {
                // 检查是否同属一个父课程
                if (scannedTask.getParentCourseId() != null && 
                    scannedTask.getParentCourseId().equals(checkinTask.getParentCourseId())) {
                    log.info("验证通过: 扫描了同一课程的其他有效签到任务");
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("验证其他任务ID时出错", e);
        }
        
        return false;
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
    
    // 验证位置
    private boolean verifyLocation(Course checkinTask, String location) {
        // 实际项目中应该使用JSON库和地理位置库计算距离
        // 这里做简单模拟
        return true;
    }
    
    // 验证WiFi
    private boolean verifyWifi(Course checkinTask, String verifyData) {
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

    private CourseUserDTO convertToCourseUserDTO(CourseUser courseUser, User currentUser, Course course) {
        CourseUserDTO dto = new CourseUserDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());
        dto.setUserId(currentUser.getId());
        dto.setUsername(currentUser.getUsername());
        dto.setUserFullName(currentUser.getFullName());
        dto.setRole(courseUser.getRole());
        dto.setJoinedAt(courseUser.getJoinedAt());
        return dto;
    }

    @Override
    public Map<String, Object> getCheckinRecords(String checkinId, int page, int size) {
        // 验证签到任务是否存在
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
            
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        // 创建分页请求，添加按签到时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("checkInTime").descending());
        
        // 获取签到记录
        Page<Record> recordsPage = recordRepository.findByCourseId(checkinId, pageable);
        
        // 转换为DTO
        List<Map<String, Object>> recordsList = recordsPage.getContent().stream()
            .map(record -> {
                User user = userRepository.findById(record.getUserId()).orElse(null);
                
                Map<String, Object> recordMap = new HashMap<>();
                recordMap.put("id", record.getId());
                recordMap.put("userId", record.getUserId());
                recordMap.put("username", user != null ? user.getUsername() : null);
                recordMap.put("fullName", user != null ? user.getFullName() : null);
                recordMap.put("checkInTime", record.getCheckInTime());
                recordMap.put("status", record.getStatus());
                recordMap.put("location", record.getLocation());
                recordMap.put("device", record.getDevice());
                
                return recordMap;
            })
            .collect(Collectors.toList());
        
        // 创建响应
        Map<String, Object> response = new HashMap<>();
        response.put("records", recordsList);
        response.put("currentPage", recordsPage.getNumber());
        response.put("totalItems", recordsPage.getTotalElements());
        response.put("totalPages", recordsPage.getTotalPages());
        response.put("checkinInfo", convertToDTO(checkinTask, null));
        
        return response;
    }

    @Override
    public Map<String, Object> getCourseAttendanceStats(String courseId, int page, int size) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
            
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证用户是否有权限查看（必须是课程成员或创建者）
        boolean isCreator = course.getCreatorId().equals(currentUser.getId());
        boolean isMember = courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(courseId, currentUser.getId());
        
        if (!isCreator && !isMember && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您不是该课程的成员，无权查看");
        }
        
        // 创建分页请求，按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        
        // 获取课程下的所有签到任务
        Page<Course> checkinTasksPage = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN, pageable);
        
        // 准备返回数据
        List<Map<String, Object>> statsItems = new ArrayList<>();
        
        for (Course checkin : checkinTasksPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("checkinId", checkin.getId());
            item.put("title", checkin.getName());
            item.put("description", checkin.getDescription());
            item.put("startTime", checkin.getCheckinStartTime());
            item.put("endTime", checkin.getCheckinEndTime());
            item.put("status", checkin.getStatus());
            item.put("checkinType", checkin.getCheckinType());
            
            // 获取签到统计数据
            Map<String, Object> statistics = getCheckinStatistics(checkin.getId());
            item.put("statistics", statistics);
            
            // 如果是学生，添加个人签到状态
            if (!isCreator && !hasRole(authentication, "ADMIN")) {
                Optional<Record> record = recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
                String personalStatus = record.isPresent() ? record.get().getStatus() : SystemConstants.RecordStatus.ABSENT;
                item.put("personalStatus", personalStatus);
            }
            
            statsItems.add(item);
        }
        
        // 创建响应
        Map<String, Object> response = new HashMap<>();
        response.put("items", statsItems);
        response.put("currentPage", checkinTasksPage.getNumber());
        response.put("totalItems", checkinTasksPage.getTotalElements());
        response.put("totalPages", checkinTasksPage.getTotalPages());
        response.put("courseInfo", convertToDTO(course, null));
        
        return response;
    }
    
    @Override
    public Map<String, Object> getCheckinDetail(String checkinId, int page, int size) {
        // 验证签到任务是否存在
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
            
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 获取父课程
        Course parentCourse = null;
        if (checkinTask.getParentCourseId() != null) {
            parentCourse = courseRepository.findById(checkinTask.getParentCourseId())
                .orElse(null);
        }
        
        // 验证用户是否有权限查看
        boolean isCreator = checkinTask.getCreatorId().equals(currentUser.getId());
        boolean isParentCreator = parentCourse != null && parentCourse.getCreatorId().equals(currentUser.getId());
        boolean isMember = parentCourse != null && 
            courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(parentCourse.getId(), currentUser.getId());
        
        if (!isCreator && !isParentCreator && !isMember && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您没有权限查看此签到任务");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("checkinInfo", convertToDTO(checkinTask, null));
        
        // 如果是创建者或管理员，返回所有学生的签到情况
        if (isCreator || isParentCreator || hasRole(authentication, "ADMIN")) {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("checkInTime").descending());
            
            // 获取签到记录
            Page<Record> recordsPage = recordRepository.findByCourseId(checkinId, pageable);
            
            // 转换为统计数据
            List<Map<String, Object>> records = recordsPage.getContent().stream()
                .map(record -> {
                    User user = userRepository.findById(record.getUserId()).orElse(null);
                    
                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("id", record.getId());
                    recordMap.put("userId", record.getUserId());
                    recordMap.put("username", user != null ? user.getUsername() : null);
                    recordMap.put("fullName", user != null ? user.getFullName() : null);
                    recordMap.put("checkInTime", record.getCheckInTime());
                    recordMap.put("status", record.getStatus());
                    recordMap.put("location", record.getLocation());
                    recordMap.put("device", record.getDevice());
                    
                    return recordMap;
                })
                .collect(Collectors.toList());
            
            response.put("records", records);
            response.put("currentPage", recordsPage.getNumber());
            response.put("totalItems", recordsPage.getTotalElements());
            response.put("totalPages", recordsPage.getTotalPages());
            
            // 添加签到统计
            response.put("statistics", getCheckinStatistics(checkinId));
        } 
        // 如果是普通学生，只返回该学生的签到状态
        else {
            Optional<Record> record = recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkinId);
            Map<String, Object> personalRecord = new HashMap<>();
            
            if (record.isPresent()) {
                Record r = record.get();
                personalRecord.put("status", r.getStatus());
                personalRecord.put("checkInTime", r.getCheckInTime());
                personalRecord.put("device", r.getDevice());
                personalRecord.put("location", r.getLocation());
            } else {
                personalRecord.put("status", SystemConstants.RecordStatus.ABSENT);
            }
            
            response.put("personalRecord", personalRecord);
        }
        
        return response;
    }
    
    /**
     * 获取签到任务的统计数据
     * 不计入老师，并提供已签到和未签到学生列表
     */
    private Map<String, Object> getCheckinStatistics(String checkinId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取签到任务
        Course checkinTask = courseRepository.findById(checkinId).orElse(null);
        if (checkinTask == null || checkinTask.getParentCourseId() == null) {
            return statistics;
        }
        
        String parentCourseId = checkinTask.getParentCourseId();
        
        // 获取课程下的所有学生ID列表
        List<String> studentIds = courseUserRepository.findUserIdsByCourseIdAndRole(
            parentCourseId, SystemConstants.CourseUserRole.STUDENT);
        
        // 学生总数
        long totalStudents = studentIds.size();
        statistics.put("totalStudents", totalStudents);
        
        // 已签到学生记录
        List<Record> checkinRecords = recordRepository.findByCourseId(checkinId);
        
        // 统计不同状态的签到人数
        long normalCount = checkinRecords.stream()
            .filter(r -> SystemConstants.RecordStatus.NORMAL.equals(r.getStatus()))
            .count();
            
        long lateCount = checkinRecords.stream()
            .filter(r -> SystemConstants.RecordStatus.LATE.equals(r.getStatus()))
            .count();
            
        long totalPresent = normalCount + lateCount;
        long absentCount = totalStudents - totalPresent;
        
        statistics.put("normalCount", normalCount);
        statistics.put("lateCount", lateCount);
        statistics.put("presentCount", totalPresent);
        statistics.put("absentCount", Math.max(0, absentCount));
        
        // 收集已签到学生ID
        Set<String> signedUserIds = checkinRecords.stream()
            .map(Record::getUserId)
            .collect(Collectors.toSet());
            
        // 未签到学生ID列表 = 所有学生ID - 已签到学生ID
        List<String> absentUserIds = studentIds.stream()
            .filter(id -> !signedUserIds.contains(id))
            .collect(Collectors.toList());
            
        // 转换为学生信息列表 (仅教师可见)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(authentication, "TEACHER") || hasRole(authentication, "ADMIN")) {
            List<Map<String, Object>> absentStudents = new ArrayList<>();
            for (String userId : absentUserIds) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    Map<String, Object> student = new HashMap<>();
                    student.put("userId", user.getId());
                    student.put("username", user.getUsername());
                    student.put("fullName", user.getFullName());
                    absentStudents.add(student);
                }
            }
            statistics.put("absentStudents", absentStudents);
        }
        
        return statistics;
    }
    
    // 检查用户是否具有指定角色
    private boolean hasRole(Authentication authentication, String roleName) {
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }

    @Override
    public Map<String, Object> getAttendanceList(String courseId, int page, int size) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
            
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证用户是否有权限查看（必须是课程成员或创建者）
        boolean isCreator = course.getCreatorId().equals(currentUser.getId());
        boolean isMember = courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(courseId, currentUser.getId());
        
        if (!isCreator && !isMember && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您不是该课程的成员，无权查看");
        }
        
        // 创建分页请求，按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        
        // 获取课程下的所有签到任务
        Page<Course> checkinTasksPage = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN, pageable);
            
        // 当前时间，用于判断签到状态
        LocalDateTime now = LocalDateTime.now();
        
        // 转换为DTO
        List<Map<String, Object>> items = new ArrayList<>();
        for (Course checkin : checkinTasksPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", checkin.getId());
            item.put("name", checkin.getName());
            item.put("description", checkin.getDescription());
            item.put("startTime", checkin.getCheckinStartTime());
            item.put("endTime", checkin.getCheckinEndTime());
            item.put("status", checkin.getStatus());
            item.put("checkinType", checkin.getCheckinType());
            item.put("createdAt", checkin.getCreatedAt());
            
            // 如果是学生，添加个人签到状态
            if (!isCreator && !hasRole(authentication, "ADMIN")) {
                Optional<Record> record = recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
                String recordStatus = record.isPresent() ? record.get().getStatus() : SystemConstants.RecordStatus.ABSENT;
                
                // 根据签到任务状态和时间判断当前可读状态
                String displayStatus;
                if (record.isPresent()) {
                    // 已签到
                    if (SystemConstants.RecordStatus.NORMAL.equals(recordStatus)) {
                        displayStatus = "已签到";
                    } else if (SystemConstants.RecordStatus.LATE.equals(recordStatus)) {
                        displayStatus = "已签到(迟到)";
                    } else if (SystemConstants.RecordStatus.LEAVE.equals(recordStatus)) {
                        displayStatus = "已请假";
                    } else {
                        displayStatus = "已签到";
                    }
                } else {
                    // 未签到
                    if (SystemConstants.TaskStatus.CREATED.equals(checkin.getStatus())) {
                        displayStatus = "未开始";
                    } else if (SystemConstants.TaskStatus.ACTIVE.equals(checkin.getStatus())) {
                        if (checkin.getCheckinStartTime() != null && checkin.getCheckinEndTime() != null) {
                            if (now.isBefore(checkin.getCheckinStartTime())) {
                                displayStatus = "未开始";
                            } else if (now.isAfter(checkin.getCheckinEndTime())) {
                                displayStatus = "已结束(缺席)";
                            } else {
                                displayStatus = "待签到";
                            }
                        } else {
                            displayStatus = "待签到";
                        }
                    } else if (SystemConstants.TaskStatus.ENDED.equals(checkin.getStatus()) || 
                               SystemConstants.TaskStatus.COMPLETED.equals(checkin.getStatus())) {
                        displayStatus = "已结束(缺席)";
                    } else {
                        displayStatus = "已取消";
                    }
                }
                
                item.put("personalStatus", recordStatus);  // 保留原始状态码
                item.put("displayStatus", displayStatus);  // 添加可读状态
            }
            
            items.add(item);
        }
        
        // 创建响应
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("currentPage", checkinTasksPage.getNumber());
        response.put("totalItems", checkinTasksPage.getTotalElements());
        response.put("totalPages", checkinTasksPage.getTotalPages());
        response.put("courseInfo", convertToDTO(course, null));
        
        return response;
    }
    
    @Override
    public Map<String, Object> getCourseAttendanceDetail(String courseId) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
            
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证用户是否有权限查看（必须是课程成员或创建者）
        boolean isCreator = course.getCreatorId().equals(currentUser.getId());
        boolean isMember = courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(courseId, currentUser.getId());
        
        if (!isCreator && !isMember && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您不是该课程的成员，无权查看");
        }
        
        // 获取课程下的所有签到任务
        List<Course> allCheckinTasks = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN);
            
        // 计算总体统计数据
        Map<String, Object> statistics = new HashMap<>();
        
        long totalStudents = courseUserRepository.countByCourseIdAndActiveTrue(courseId);
        statistics.put("totalStudents", totalStudents);
        
        long totalCheckins = allCheckinTasks.size();
        statistics.put("totalCheckins", totalCheckins);
        
        // 计算各签到状态的人次
        long totalNormalCount = 0;
        long totalLateCount = 0;
        long totalAbsentCount = 0;
        
        for (Course checkin : allCheckinTasks) {
            long normalCount = recordRepository.countByCourseIdAndStatus(checkin.getId(), SystemConstants.RecordStatus.NORMAL);
            long lateCount = recordRepository.countByCourseIdAndStatus(checkin.getId(), SystemConstants.RecordStatus.LATE);
            long absentCount = totalStudents - (normalCount + lateCount);
            
            totalNormalCount += normalCount;
            totalLateCount += lateCount;
            totalAbsentCount += Math.max(0, absentCount);
        }
        
        statistics.put("totalNormalCount", totalNormalCount);
        statistics.put("totalLateCount", totalLateCount);
        statistics.put("totalAbsentCount", totalAbsentCount);
        
        // 计算出勤率
        if (totalCheckins > 0 && totalStudents > 0) {
            double attendanceRate = (double) (totalNormalCount + totalLateCount) / (totalCheckins * totalStudents) * 100;
            statistics.put("attendanceRate", Math.round(attendanceRate * 100) / 100.0); // 保留两位小数
        } else {
            statistics.put("attendanceRate", 0.0);
        }
        
        // 如果是学生，添加个人统计
        if (!isCreator && !hasRole(authentication, "ADMIN")) {
            Map<String, Object> personalStats = new HashMap<>();
            
            long personalNormalCount = 0;
            long personalLateCount = 0;
            long personalAbsentCount = 0;
            
            for (Course checkin : allCheckinTasks) {
                Optional<Record> record = recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
                if (record.isPresent()) {
                    String status = record.get().getStatus();
                    if (SystemConstants.RecordStatus.NORMAL.equals(status)) {
                        personalNormalCount++;
                    } else if (SystemConstants.RecordStatus.LATE.equals(status)) {
                        personalLateCount++;
                    }
                } else {
                    personalAbsentCount++;
                }
            }
            
            personalStats.put("normalCount", personalNormalCount);
            personalStats.put("lateCount", personalLateCount);
            personalStats.put("absentCount", personalAbsentCount);
            
            if (totalCheckins > 0) {
                double personalAttendanceRate = (double) (personalNormalCount + personalLateCount) / totalCheckins * 100;
                personalStats.put("attendanceRate", Math.round(personalAttendanceRate * 100) / 100.0); // 保留两位小数
            } else {
                personalStats.put("attendanceRate", 0.0);
            }
            
            statistics.put("personalStats", personalStats);
        }
        
        // 创建响应
        Map<String, Object> response = new HashMap<>();
        response.put("courseInfo", convertToDTO(course, null));
        response.put("statistics", statistics);
        
        return response;
    }
    
    @Override
    public Map<String, Object> getCheckinDetails(String checkinId, int page, int size) {
        // 验证签到任务是否存在
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
            
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 获取父课程
        Course parentCourse = null;
        if (checkinTask.getParentCourseId() != null) {
            parentCourse = courseRepository.findById(checkinTask.getParentCourseId())
                .orElse(null);
        }
        
        // 验证用户是否有权限查看
        boolean isCreator = checkinTask.getCreatorId().equals(currentUser.getId());
        boolean isParentCreator = parentCourse != null && parentCourse.getCreatorId().equals(currentUser.getId());
        boolean isMember = parentCourse != null && 
            courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(parentCourse.getId(), currentUser.getId());
        
        if (!isCreator && !isParentCreator && !isMember && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您没有权限查看此签到任务");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("checkinInfo", convertToDTO(checkinTask, null));
        
        if (parentCourse != null) {
            response.put("courseInfo", convertToDTO(parentCourse, null));
        }
        
        // 如果是创建者或管理员，返回所有学生的签到情况
        if (isCreator || isParentCreator || hasRole(authentication, "ADMIN")) {
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("checkInTime").descending());
            
            // 获取签到记录
            Page<Record> recordsPage = recordRepository.findByCourseId(checkinId, pageable);
            
            // 转换为统计数据
            List<Map<String, Object>> records = recordsPage.getContent().stream()
                .map(record -> {
                    User user = userRepository.findById(record.getUserId()).orElse(null);
                    
                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("id", record.getId());
                    recordMap.put("userId", record.getUserId());
                    recordMap.put("username", user != null ? user.getUsername() : null);
                    recordMap.put("fullName", user != null ? user.getFullName() : null);
                    recordMap.put("checkInTime", record.getCheckInTime());
                    recordMap.put("status", record.getStatus());
                    recordMap.put("location", record.getLocation());
                    recordMap.put("device", record.getDevice());
                    
                    return recordMap;
                })
                .collect(Collectors.toList());
            
            response.put("records", records);
            response.put("currentPage", recordsPage.getNumber());
            response.put("totalItems", recordsPage.getTotalElements());
            response.put("totalPages", recordsPage.getTotalPages());
            
            // 计算签到统计
            long totalStudents = 0;
            if (parentCourse != null) {
                totalStudents = courseUserRepository.countByCourseIdAndActiveTrue(parentCourse.getId());
            }
            
            long normalCount = recordRepository.countByCourseIdAndStatus(checkinId, SystemConstants.RecordStatus.NORMAL);
            long lateCount = recordRepository.countByCourseIdAndStatus(checkinId, SystemConstants.RecordStatus.LATE);
            long presentCount = normalCount + lateCount;
            long absentCount = Math.max(0, totalStudents - presentCount);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalStudents", totalStudents);
            statistics.put("normalCount", normalCount);
            statistics.put("lateCount", lateCount);
            statistics.put("presentCount", presentCount);
            statistics.put("absentCount", absentCount);
            
            if (totalStudents > 0) {
                double attendanceRate = (double) presentCount / totalStudents * 100;
                statistics.put("attendanceRate", Math.round(attendanceRate * 100) / 100.0);
            } else {
                statistics.put("attendanceRate", 0.0);
            }
            
            response.put("statistics", statistics);
        } 
        // 如果是普通学生，只返回该学生的签到状态
        else {
            Optional<Record> record = recordRepository.findByUserIdAndCourseId(currentUser.getId(), checkinId);
            Map<String, Object> personalRecord = new HashMap<>();
            
            if (record.isPresent()) {
                Record r = record.get();
                personalRecord.put("status", r.getStatus());
                personalRecord.put("checkInTime", r.getCheckInTime());
                personalRecord.put("device", r.getDevice());
                personalRecord.put("location", r.getLocation());
            } else {
                personalRecord.put("status", SystemConstants.RecordStatus.ABSENT);
            }
            
            response.put("personalRecord", personalRecord);
        }
        
        return response;
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
        
        // 直接使用签到任务ID作为签到码，移除前缀
        checkinTask.setVerifyParams(checkinId);
        courseRepository.save(checkinTask);
        
        return checkinId;
    }

    @Override
    public Map<String, Object> getCourseStatistics(String courseId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
            
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        // 获取课程学生列表
        List<String> studentIds = courseUserRepository.findUserIdsByCourseIdAndRole(
            courseId, SystemConstants.CourseUserRole.STUDENT);
            
        // 学生总数
        int totalStudents = studentIds.size();
        statistics.put("totalStudents", totalStudents);
        
        // 获取课程所有签到任务
        List<Course> checkinTasks = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN);
            
        // 签到任务总数
        int totalTasks = checkinTasks.size();
        statistics.put("totalCheckinTasks", totalTasks);
        
        // 进行中的签到任务数
        long activeTasks = checkinTasks.stream()
            .filter(task -> SystemConstants.TaskStatus.ACTIVE.equals(task.getStatus()))
            .count();
        statistics.put("activeCheckinTasks", activeTasks);
        
        // 已结束的签到任务数
        long endedTasks = checkinTasks.stream()
            .filter(task -> SystemConstants.TaskStatus.ENDED.equals(task.getStatus()))
            .count();
        statistics.put("endedCheckinTasks", endedTasks);
        
        // 统计每个学生的签到情况
        List<Map<String, Object>> studentStatsList = new ArrayList<>();
        
        for (String studentId : studentIds) {
            User student = userRepository.findById(studentId).orElse(null);
            if (student == null) continue;
            
            Map<String, Object> studentStats = new HashMap<>();
            studentStats.put("userId", studentId);
            studentStats.put("username", student.getUsername());
            studentStats.put("fullName", student.getFullName());
            
            // 统计该学生各种状态的签到次数
            long normalCount = 0;
            long lateCount = 0;
            long absentCount = 0;
            
            for (Course task : checkinTasks) {
                // 只统计已结束的签到任务
                if (!SystemConstants.TaskStatus.ENDED.equals(task.getStatus())) {
                    continue;
                }
                
                Optional<Record> record = recordRepository.findByUserIdAndCourseId(studentId, task.getId());
                if (record.isPresent()) {
                    if (SystemConstants.RecordStatus.NORMAL.equals(record.get().getStatus())) {
                        normalCount++;
                    } else if (SystemConstants.RecordStatus.LATE.equals(record.get().getStatus())) {
                        lateCount++;
                    }
                } else {
                    absentCount++;
                }
            }
            
            studentStats.put("normalCount", normalCount);
            studentStats.put("lateCount", lateCount);
            studentStats.put("absentCount", absentCount);
            studentStats.put("attendanceRate", endedTasks > 0 
                ? Math.round((normalCount + lateCount) * 100.0 / endedTasks)
                : 0);
            
            studentStatsList.add(studentStats);
        }
        
        // 按出勤率降序排序
        studentStatsList.sort((a, b) -> 
            ((Integer)b.get("attendanceRate")).compareTo((Integer)a.get("attendanceRate")));
        
        statistics.put("studentStatistics", studentStatsList);
        
        // 总体统计
        statistics.put("overallAttendanceRate", totalStudents > 0 && endedTasks > 0 
            ? calculateOverallAttendanceRate(courseId, studentIds, checkinTasks)
            : 0);
        
        return statistics;
    }
    
    // 计算总体出勤率
    private int calculateOverallAttendanceRate(String courseId, List<String> studentIds, List<Course> checkinTasks) {
        // 只统计已结束的任务
        long endedTasksCount = checkinTasks.stream()
            .filter(task -> SystemConstants.TaskStatus.ENDED.equals(task.getStatus()))
            .count();
            
        if (endedTasksCount == 0 || studentIds.isEmpty()) {
            return 0;
        }
        
        // 理论上应该有的总签到次数 = 学生数 * 签到任务数
        long totalPossibleRecords = studentIds.size() * endedTasksCount;
        
        // 实际签到次数（正常+迟到）
        long actualRecords = 0;
        
        for (Course task : checkinTasks) {
            if (SystemConstants.TaskStatus.ENDED.equals(task.getStatus())) {
                // 获取该任务的签到记录数
                actualRecords += recordRepository.countByCourseId(task.getId());
            }
        }
        
        // 计算百分比
        return (int) Math.round(actualRecords * 100.0 / totalPossibleRecords);
    }
} 