package com.attendance.service.course;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.common.util.DateTimeUtil;
import com.attendance.model.dto.course.CourseDTO;
import com.attendance.model.dto.course.CourseUserDTO;
import com.attendance.model.dto.course.CourseRecordDTO;
import com.attendance.model.entity.Course;
import com.attendance.model.entity.CourseUser;
import com.attendance.model.entity.CourseRecord;
import com.attendance.model.entity.User;
import com.attendance.repository.course.CourseRepository;
import com.attendance.repository.course.CourseUserRepository;
import com.attendance.repository.course.CourseRecordRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.Duration;
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
    private final CourseUserRepository courseUserRepository;
    private final CourseRecordRepository courseRecordRepository;
    
    @Override
    public CourseDTO getCourse(String id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new BusinessException("课程或签到任务不存在"));
        
        // 如果是课程类型，检查是否需要更新状态
        if (SystemConstants.CourseType.COURSE.equals(course.getType())) {
            checkAndUpdateCourseStatus(course);
        }
        
        User creator = userRepository.findById(course.getCreatorId())
            .orElse(null);
        
        CourseDTO dto = convertToDTO(course, creator);
        
        // 如果是课程，添加成员数量
        if (SystemConstants.CourseType.COURSE.equals(course.getType())) {
            dto.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(course.getId()));
        }
        
        return dto;
    }
    
    /**
     * 检查并更新课程状态
     * 根据课程的结束日期和当前日期，自动更新课程状态
     *
     * @param course 需要检查的课程
     * @return 如果状态发生更改则返回true
     */
    private boolean checkAndUpdateCourseStatus(Course course) {
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        boolean updated = false;
        
        // 检查ACTIVE状态的过期课程
        if (SystemConstants.CourseStatus.ACTIVE.equals(course.getStatus()) && 
            course.getEndDate() != null && today.isAfter(course.getEndDate())) {
            
            log.info("自动更新课程状态: 课程 [{}] ({}) 已过结束日期 [{}]，状态由 [{}] 更新为 [{}]", 
                course.getId(), course.getName(), course.getEndDate(), 
                course.getStatus(), SystemConstants.CourseStatus.FINISHED);
            
            course.setStatus(SystemConstants.CourseStatus.FINISHED);
            courseRepository.save(course);
            updated = true;
        }
        
        // 检查FINISHED状态的长期未使用课程
        else if (SystemConstants.CourseStatus.FINISHED.equals(course.getStatus()) && 
            course.getEndDate() != null && today.minusDays(30).isAfter(course.getEndDate())) {
            
            log.info("自动归档课程: 课程 [{}] ({}) 已结束超过30天，状态由 [{}] 更新为 [{}]", 
                course.getId(), course.getName(), course.getStatus(), 
                SystemConstants.CourseStatus.ARCHIVED);
            
            course.setStatus(SystemConstants.CourseStatus.ARCHIVED);
            courseRepository.save(course);
            updated = true;
        }
        
        return updated;
    }
    
    @Override
    public List<CourseDTO> getAllCourses() {
        List<Course> courses = courseRepository.findByTypeAndActive(SystemConstants.CourseType.COURSE, true);
        
        // 检查并更新课程状态
        courses.forEach(this::checkAndUpdateCourseStatus);
        
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
            courses = courseRepository.findByCreatorIdAndTypeAndActive(
                currentUser.getId(), SystemConstants.CourseType.COURSE, true);
            log.debug("教师用户[{}]查询创建的课程: 找到{}个", username, courses.size());
        } 
        // 学生只能看到自己加入的课程
        else if (isStudent) {
            List<CourseUser> joinedCourseUsers = courseUserRepository.findByUserIdAndActiveTrue(currentUser.getId());
            List<String> joinedCourseIds = joinedCourseUsers.stream()
                .map(CourseUser::getCourseId)
                .collect(Collectors.toList());
            
            if (!joinedCourseIds.isEmpty()) {
                // 从所有已加入课程ID中筛选出类型为COURSE且active=true的课程
                courses = courseRepository.findAllById(joinedCourseIds).stream()
                    .filter(course -> 
                        SystemConstants.CourseType.COURSE.equals(course.getType()) && 
                        course.getActive())
                    .collect(Collectors.toList());
                log.debug("学生用户[{}]查询加入的课程: 找到{}个", username, courses.size());
            }
        }
        // 管理员可以看到所有课程
        else {
            courses = courseRepository.findByTypeAndActive(SystemConstants.CourseType.COURSE, true);
            log.debug("管理员用户[{}]查询所有课程: 找到{}个", username, courses.size());
        }
        
        // 检查并更新课程状态
        courses.forEach(this::checkAndUpdateCourseStatus);
        
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
            coursePageResult = courseRepository.findByCreatorIdAndTypeAndActive(
                currentUser.getId(), SystemConstants.CourseType.COURSE, true, pageable);
        } 
        // 学生只能看到自己加入的活跃课程
        else if (isStudent) {
            List<CourseUser> joinedCourseUsers = courseUserRepository.findByUserIdAndActiveTrue(currentUser.getId());
            List<String> joinedCourseIds = joinedCourseUsers.stream()
                .map(CourseUser::getCourseId)
                .collect(Collectors.toList());
            
            if (!joinedCourseIds.isEmpty()) {
                // 这里由于没有直接的分页方法，先获取指定ID的所有课程，然后手动分页
                Page<Course> allMatchingCourses = courseRepository.findByTypeAndIdInAndActive(
                    SystemConstants.CourseType.COURSE, joinedCourseIds, true, pageable);
                coursePageResult = allMatchingCourses;
            } else {
                // 如果学生没有加入任何课程，返回空分页结果
                coursePageResult = Page.empty(pageable);
            }
        }
        // 管理员可以看到所有课程
        else {
            coursePageResult = courseRepository.findByTypeAndActive(SystemConstants.CourseType.COURSE, true, pageable);
        }
        
        // 将课程列表转换为DTO
        List<CourseDTO> courseDTOs = coursePageResult.getContent().stream()
            .map(course -> {
                // 检查并更新课程状态
                checkAndUpdateCourseStatus(course);
                
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
            .orElseThrow(() -> new BusinessException("用户验证失败：无法获取当前用户信息，请重新登录"));
        
        // 查找父课程
        Course parentCourse = courseRepository.findById(parentCourseId)
            .orElseThrow(() -> new BusinessException("课程不存在：未找到ID为" + parentCourseId + "的课程，请确认课程ID是否正确"));
        
        // 验证用户是否为课程创建者
        if (!parentCourse.getCreatorId().equals(creator.getId()) && 
            !isAdmin(authentication)) {
            throw new BusinessException("权限不足：只有课程创建者可以创建签到任务，请确认您是否有权限操作该课程");
        }
        
        // 验证课程类型
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("操作失败：只能在普通课程下创建签到任务，当前课程类型为" + parentCourse.getType());
        }
        
        // 验证签到时间
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        
        // 添加时间验证日志
        log.info("时间验证 - 当前系统时间: {}, 时区: {}", 
            DateTimeUtil.formatDateTime(LocalDateTime.now()),
            TimeZone.getDefault().getID());
        log.info("时间验证 - 开始时间: {}, 结束时间: {}", 
            startTime != null ? DateTimeUtil.formatDateTime(startTime) : "null",
            endTime != null ? DateTimeUtil.formatDateTime(endTime) : "null");
        
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && startTime.isBefore(now)) {
            log.error("时间验证失败 - 开始时间早于当前时间: {} < {}", 
                DateTimeUtil.formatDateTime(startTime),
                DateTimeUtil.formatDateTime(now));
            throw new BusinessException("开始时间(" + 
                DateTimeUtil.formatDateTime(startTime) + 
                ")不能早于当前时间(" + 
                DateTimeUtil.formatDateTime(now) + ")");
        }
        
        if (endTime != null && endTime.isBefore(now)) {
            log.error("时间验证失败 - 结束时间早于当前时间: {} < {}", 
                DateTimeUtil.formatDateTime(endTime),
                DateTimeUtil.formatDateTime(now));
            throw new BusinessException("结束时间(" + 
                DateTimeUtil.formatDateTime(endTime) + 
                ")不能早于当前时间(" + 
                DateTimeUtil.formatDateTime(now) + ")");
        }
        
        // 验证时间范围合理性
        if (startTime != null && endTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            if (duration.toMinutes() < 5) {
                throw new BusinessException("签到时间范围过短：开始时间(" + 
                    DateTimeUtil.formatDateTime(startTime) + 
                    ")到结束时间(" + 
                    DateTimeUtil.formatDateTime(endTime) + 
                    ")至少需要5分钟");
            }
            if (duration.toHours() > 24) {
                throw new BusinessException("签到时间范围过长：开始时间(" + 
                    DateTimeUtil.formatDateTime(startTime) + 
                    ")到结束时间(" + 
                    DateTimeUtil.formatDateTime(endTime) + 
                    ")不能超过24小时");
            }
        }
        
        // 验证签到类型
        if (!SystemConstants.CheckInType.isValidType(checkinType)) {
            throw new BusinessException("签到类型错误：无效的签到类型" + checkinType + 
                "，支持的签到类型包括：QR_CODE（二维码签到）、LOCATION（位置签到）、WIFI（WiFi签到）、MANUAL（手动签到）");
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
    public CourseDTO updateCourse(String courseId, String name, String description, LocalDate startDate, LocalDate endDate) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 查找课程
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        // 验证权限：只有课程创建者和管理员可以更新课程
        if (!course.getCreatorId().equals(currentUser.getId()) && !isAdmin(authentication)) {
            throw new BusinessException("您没有权限更新该课程");
        }
        
        // 验证课程类型必须是COURSE
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("只能更新课程，不能更新签到任务");
        }
        
        // 验证日期
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        
        // 更新课程信息
        if (name != null && !name.trim().isEmpty()) {
            course.setName(name);
        }
        
        if (description != null) {
            course.setDescription(description);
        }
        
        if (startDate != null) {
            course.setStartDate(startDate);
        }
        
        if (endDate != null) {
            course.setEndDate(endDate);
        }
        
        // 保存更新
        Course updatedCourse = courseRepository.save(course);
        
        // 返回更新后的课程DTO
        User creator = userRepository.findById(updatedCourse.getCreatorId())
            .orElse(null);
        
        CourseDTO courseDTO = convertToDTO(updatedCourse, creator);
        courseDTO.setMemberCount((int) courseUserRepository.countByCourseIdAndActiveTrue(updatedCourse.getId()));
        
        return courseDTO;
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
        // 处理带有时间戳的二维码内容 (格式：courseId:timestamp)
        String courseId;
        if (qrCode.contains(":")) {
            // 从带时间戳的二维码中提取课程ID
            courseId = qrCode.split(":")[0];
            log.debug("从带时间戳二维码中提取课程ID: {}", courseId);
        } else {
            // 兼容旧版二维码格式
            courseId = qrCode;
        }
        
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
    public Map<String, Object> submitCheckin(String taskId, String verifyData, String verifyMethod, String location, String device) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        try {
            // 调用新的实现方法
            CourseRecordDTO recordDTO = submitCheckIn(taskId, verifyMethod, location, device, verifyData);
            result.put("success", true);
            result.put("recordId", recordDTO.getId());
        } catch (BusinessException e) {
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("message", "签到失败：" + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getCheckinDetail(String checkinId, int page, int size) {
        // 验证签到任务是否存在
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
            
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        // 创建分页请求，添加按签到时间降序排序
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("checkInTime").descending());
        
        // 获取签到记录
        Page<CourseRecord> recordsPage = courseRecordRepository.findByCourseId(checkinId, pageable);
        
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
        
        // 获取签到统计信息
        Map<String, Object> statistics = getCheckinStatistics(checkinId);
        response.put("statistics", statistics);
        
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
                Optional<CourseRecord> record = courseRecordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
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
    public Map<String, Object> getCheckinStatistics(String checkinId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取签到任务
        Course checkinTask = courseRepository.findById(checkinId).orElse(null);
        if (checkinTask == null || checkinTask.getParentCourseId() == null) {
            throw new BusinessException("签到任务不存在");
        }
        
        String parentCourseId = checkinTask.getParentCourseId();
        
        // 获取课程下的所有学生ID列表
        List<String> studentIds = courseUserRepository.findUserIdsByCourseIdAndRole(
            parentCourseId, SystemConstants.CourseUserRole.STUDENT);
            
        // 学生总数
        long totalStudents = studentIds.size();
        statistics.put("totalStudents", totalStudents);
        
        // 基本任务信息
        statistics.put("checkinId", checkinId);
        statistics.put("title", checkinTask.getName());
        statistics.put("description", checkinTask.getDescription());
        statistics.put("startTime", checkinTask.getCheckinStartTime());
        statistics.put("endTime", checkinTask.getCheckinEndTime());
        statistics.put("status", checkinTask.getStatus());
        statistics.put("checkinType", checkinTask.getCheckinType());
        
        // 获取签到记录
        List<CourseRecord> checkinRecords = courseRecordRepository.findByCourseId(checkinId);
        
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
        
        // 出勤率
        if (totalStudents > 0) {
            statistics.put("attendanceRate", Math.round(totalPresent * 100.0 / totalStudents));
        } else {
            statistics.put("attendanceRate", 0);
        }
        
        // 收集已签到学生ID
        Set<String> signedUserIds = checkinRecords.stream()
            .map(CourseRecord::getUserId)
            .collect(Collectors.toSet());
            
        // 未签到学生ID列表 = 所有学生ID - 已签到学生ID
        List<String> absentUserIds = studentIds.stream()
            .filter(id -> !signedUserIds.contains(id))
            .collect(Collectors.toList());
        
        // 已签到学生列表
        List<Map<String, Object>> presentStudents = new ArrayList<>();
        for (CourseRecord record : checkinRecords) {
            User user = userRepository.findById(record.getUserId()).orElse(null);
            if (user != null) {
                Map<String, Object> student = new HashMap<>();
                student.put("userId", user.getId());
                student.put("username", user.getUsername());
                student.put("fullName", user.getFullName());
                student.put("checkInTime", record.getCheckInTime());
                student.put("status", record.getStatus());
                student.put("location", record.getLocation());
                student.put("device", record.getDevice());
                presentStudents.add(student);
            }
        }
        
        // 按签到时间排序
        presentStudents.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("checkInTime");
            LocalDateTime timeB = (LocalDateTime) b.get("checkInTime");
            if (timeA == null || timeB == null) {
                return 0;
            }
            return timeA.compareTo(timeB);
        });
        
        statistics.put("presentStudents", presentStudents);
        
        // 未签到学生列表
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
        
        return statistics;
    }
    
    /**
     * 获取签到任务的统计数据
     * 不计入老师，并提供已签到和未签到学生列表
     * @deprecated 使用 {@link #getCheckinStatistics(String)} 代替
     */
    @Deprecated
    @SuppressWarnings("unused")
    private Map<String, Object> getCheckinStatisticsInternal(String checkinId) {
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
        List<CourseRecord> checkinRecords = courseRecordRepository.findByCourseId(checkinId);
        
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
            .map(CourseRecord::getUserId)
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
        
        // 获取课程下的所有活跃签到任务（未被删除的）
        Page<Course> checkinTasksPage = courseRepository.findByParentCourseIdAndTypeAndActive(
            courseId, SystemConstants.CourseType.CHECKIN, true, pageable);
        
        log.debug("获取课程[{}]签到任务列表，过滤条件active=true，找到{}个任务", courseId, checkinTasksPage.getContent().size());
            
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
                Optional<CourseRecord> record = courseRecordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
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
        
        // 创建响应 - 移除多余的课程信息
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("currentPage", checkinTasksPage.getNumber());
        response.put("totalItems", checkinTasksPage.getTotalElements());
        response.put("totalPages", checkinTasksPage.getTotalPages());
        
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
        
        // 检查并更新课程状态
        checkAndUpdateCourseStatus(course);
        
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
            long normalCount = courseRecordRepository.countByCourseIdAndStatus(checkin.getId(), SystemConstants.RecordStatus.NORMAL);
            long lateCount = courseRecordRepository.countByCourseIdAndStatus(checkin.getId(), SystemConstants.RecordStatus.LATE);
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
                Optional<CourseRecord> record = courseRecordRepository.findByUserIdAndCourseId(currentUser.getId(), checkin.getId());
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
    public CourseRecordDTO submitCheckIn(String courseId, String verifyMethod, String location, String device, String verifyData) {
        // 验证签到任务是否存在
        Course checkinTask = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("签到任务不存在：请检查签到任务ID是否正确"));
            
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("无效的签到操作：提供的ID不是签到任务，请确认您使用了正确的签到ID");
        }
        
        // 检查任务状态
        if (!SystemConstants.TaskStatus.ACTIVE.equals(checkinTask.getStatus())) {
            if (SystemConstants.TaskStatus.CREATED.equals(checkinTask.getStatus())) {
                throw new BusinessException("签到尚未开始：该签到任务已创建但尚未激活，请等待教师开始签到");
            } else if (SystemConstants.TaskStatus.ENDED.equals(checkinTask.getStatus()) || 
                     SystemConstants.TaskStatus.COMPLETED.equals(checkinTask.getStatus())) {
                throw new BusinessException("签到已结束：该签到任务已经关闭，无法继续签到");
            } else {
                throw new BusinessException("签到已取消：该签到任务已被教师取消");
            }
        }
        
        // 检查签到时间
        LocalDateTime now = LocalDateTime.now();
        if (checkinTask.getCheckinStartTime() != null && now.isBefore(checkinTask.getCheckinStartTime())) {
            throw new BusinessException("签到未开始：请在" + 
                DateTimeUtil.formatDateTime(checkinTask.getCheckinStartTime()) + "之后再尝试签到");
        }
        
        if (checkinTask.getCheckinEndTime() != null && now.isAfter(checkinTask.getCheckinEndTime())) {
            throw new BusinessException("签到已截止：签到时间已于" + 
                DateTimeUtil.formatDateTime(checkinTask.getCheckinEndTime()) + "结束，请联系教师处理");
        }
        
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户验证失败：无法获取当前用户信息，请重新登录"));
            
        // 检查是否为课程成员
        if (!courseUserRepository.existsByCourseIdAndUserIdAndActiveTrue(
                checkinTask.getParentCourseId(), currentUser.getId())) {
            throw new BusinessException("签到失败：您不是该课程的成员，只有课程成员才能进行签到");
        }
        
        // 检查是否重复签到
        Optional<CourseRecord> existingRecord = courseRecordRepository.findByUserIdAndCourseId(
            currentUser.getId(), courseId);
        if (existingRecord.isPresent()) {
            throw new BusinessException("重复签到：您已经完成了本次签到，无需重复操作");
        }
        
        // 验证签到数据 (根据不同的签到方式)
        boolean isLate = false;
        if (checkinTask.getCheckinStartTime() != null && 
            now.isAfter(checkinTask.getCheckinStartTime().plusMinutes(10))) {
            isLate = true;
        }
        
        // 处理不同签到方式的验证逻辑
        if (SystemConstants.CheckInType.QR_CODE.equals(verifyMethod)) {
            // 二维码签到 - 需要处理带有时间戳的二维码内容
            if (verifyData != null && !verifyData.isEmpty()) {
                // 如果二维码内容包含时间戳（格式：checkinId:timestamp），提取courseId部分
                if (verifyData.contains(":")) {
                    String extractedCourseId = verifyData.split(":")[0];
                    // 验证提取的courseId是否匹配
                    if (!courseId.equals(extractedCourseId)) {
                throw new BusinessException("二维码验证失败：扫描的二维码数据无效，请确认您扫描了正确的签到二维码");
            }
                } else if (!courseId.equals(verifyData)) {
                    // 兼容旧版二维码（不包含时间戳）
                    throw new BusinessException("二维码验证失败：扫描的二维码数据无效，请确认您扫描了正确的签到二维码");
                }
            }
            // 如果未提供verifyData，使用courseId作为默认值（与二维码生成逻辑一致）
            if (verifyData == null || verifyData.isEmpty()) {
                verifyData = courseId;
            }
        } else if (SystemConstants.CheckInType.LOCATION.equals(verifyMethod)) {
            // 位置签到逻辑
            if (location == null || location.isEmpty()) {
                throw new BusinessException("位置签到失败：位置信息缺失，请允许应用获取您的位置信息后重试");
            }
            // 可以在这里添加位置验证逻辑，例如检查是否在课堂附近
        } else if (SystemConstants.CheckInType.WIFI.equals(verifyMethod)) {
            // WIFI签到逻辑
            if (verifyData == null || verifyData.isEmpty()) {
                throw new BusinessException("WIFI签到失败：未提供WIFI信息，请确保连接到指定WIFI并允许应用获取网络信息");
            }
            // 可以在这里添加WIFI验证逻辑
        }
        
        // 如果未提供设备信息，尝试获取基本设备信息
        if (device == null || device.isEmpty()) {
            // 尝试从请求中获取设备信息
            device = getBasicDeviceInfo();
        }
        
        // 创建签到记录
        CourseRecord record = new CourseRecord();
        record.setUserId(currentUser.getId());
        record.setCourseId(courseId);
        record.setParentCourseId(checkinTask.getParentCourseId());
        record.setStatus(isLate ? SystemConstants.RecordStatus.LATE : SystemConstants.RecordStatus.NORMAL);
        record.setCheckInTime(now);
        record.setLocation(location); // 可能为null
        record.setDevice(device);     // 设备信息（可能为"Unknown Device"）
        record.setVerifyMethod(verifyMethod);
        record.setRemark(verifyData); // 可能为null
        
        CourseRecord savedRecord = courseRecordRepository.save(record);
        
        // 返回DTO
        CourseRecordDTO dto = convertToRecordDTO(savedRecord, checkinTask);
        return dto;
    }
    
    /**
     * 获取基本设备信息，在未提供设备信息时使用
     * 实际环境中可以从请求头或其他来源获取更详细的信息
     * @return 基本设备信息字符串
     */
    private String getBasicDeviceInfo() {
        try {
            // 这里可以从RequestContextHolder获取当前请求，然后提取User-Agent等信息
            // 简化实现，实际应用中可以根据需要扩展
            return "Unknown Device";
        } catch (Exception e) {
            return "Unknown Device";
        }
    }
    
    /**
     * 将签到记录实体转换为DTO
     * 
     * @param record 签到记录实体
     * @param checkinTask 签到任务（可选）
     * @return 签到记录DTO
     */
    private CourseRecordDTO convertToRecordDTO(CourseRecord record, Course checkinTask) {
        User user = null;
        Course parentCourse = null;
        
        if (record.getUserId() != null) {
            user = userRepository.findById(record.getUserId()).orElse(null);
        }
        
        Course task = checkinTask;
        if (task == null && record.getCourseId() != null) {
            task = courseRepository.findById(record.getCourseId()).orElse(null);
        }
        
        if (task != null && task.getParentCourseId() != null) {
            parentCourse = courseRepository.findById(task.getParentCourseId()).orElse(null);
        }
        
        CourseRecordDTO dto = CourseRecordDTO.builder()
            .id(record.getId())
            .userId(record.getUserId())
            .username(user != null ? user.getUsername() : null)
            .fullName(user != null ? user.getFullName() : null)
            .courseId(record.getCourseId())
            .courseName(task != null ? task.getName() : null)
            .parentCourseId(task != null ? task.getParentCourseId() : null)
            .parentCourseName(parentCourse != null ? parentCourse.getName() : null)
            .status(record.getStatus())
            .checkInTime(record.getCheckInTime())
            .location(record.getLocation())
            .device(record.getDevice())
            .verifyMethod(record.getVerifyMethod())
            .remark(record.getRemark())
            .active(record.isActive())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
            
        return dto;
    }

    /**
     * 生成唯一的邀请码 (6位字母数字组合)
     */
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
    
    /**
     * 将实体转换为DTO
     */
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
    
    /**
     * 转换为CourseUserDTO
     */
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
    public Map<String, Object> getUserCourseRecords(String courseId, String userId, int page, int size) {
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
            
        // 确定查询的用户
        String targetUserId = userId;
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            targetUserId = currentUser.getId();
        }
        
        // 检查权限：自己只能查看自己的记录，老师和管理员可以查看所有学生记录
        boolean isCreator = course.getCreatorId().equals(currentUser.getId());
        if (!targetUserId.equals(currentUser.getId()) && !isCreator && !hasRole(authentication, "ADMIN")) {
            throw new BusinessException("您无权查看其他用户的签到记录");
        }
        
        // 获取用户信息
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new BusinessException("目标用户不存在"));
            
        // 获取课程下的所有签到任务
        List<Course> checkinTasks = courseRepository.findByParentCourseIdAndType(
            courseId, SystemConstants.CourseType.CHECKIN);
            
        // 将所有签到任务ID放入列表
        List<String> checkinTaskIds = checkinTasks.stream()
            .map(Course::getId)
            .collect(Collectors.toList());
            
        // 查询用户在这些签到任务的记录
        List<CourseRecord> allRecords = new ArrayList<>();
        for (String checkinId : checkinTaskIds) {
            Optional<CourseRecord> recordOpt = courseRecordRepository.findByUserIdAndCourseId(targetUserId, checkinId);
            if (recordOpt.isPresent()) {
                allRecords.add(recordOpt.get());
            }
        }
        
        // 手动分页
        int start = page * size;
        int end = Math.min(start + size, allRecords.size());
        List<CourseRecord> pagedRecords = start < end ? allRecords.subList(start, end) : new ArrayList<>();
        
        // 转换为DTO列表
        List<Map<String, Object>> records = new ArrayList<>();
        for (CourseRecord record : pagedRecords) {
            Course checkinTask = courseRepository.findById(record.getCourseId()).orElse(null);
            if (checkinTask != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("recordId", record.getId());
                item.put("checkinId", record.getCourseId());
                item.put("checkinName", checkinTask.getName());
                item.put("status", record.getStatus());
                item.put("checkInTime", record.getCheckInTime());
                item.put("location", record.getLocation());
                item.put("device", record.getDevice());
                item.put("verifyMethod", record.getVerifyMethod());
                records.add(item);
            }
        }
        
        // 为签到记录计算未签到记录
        List<Map<String, Object>> allRecordsWithAbsent = new ArrayList<>(records);
        for (Course task : checkinTasks) {
            boolean found = allRecords.stream()
                .anyMatch(r -> r.getCourseId().equals(task.getId()));
                
            if (!found && (task.getCheckinEndTime() == null || 
                task.getCheckinEndTime().isBefore(LocalDateTime.now()))) {
                Map<String, Object> absentRecord = new HashMap<>();
                absentRecord.put("checkinId", task.getId());
                absentRecord.put("checkinName", task.getName());
                absentRecord.put("status", SystemConstants.RecordStatus.ABSENT);
                absentRecord.put("checkInTime", null);
                allRecordsWithAbsent.add(absentRecord);
            }
        }
        
        // 创建响应
        Map<String, Object> response = new HashMap<>();
        response.put("records", records);
        response.put("currentPage", page);
        response.put("totalItems", allRecords.size());
        response.put("totalPages", (int) Math.ceil((double) allRecords.size() / size));
        
        // 添加用户和课程信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", targetUser.getId());
        userInfo.put("username", targetUser.getUsername());
        userInfo.put("fullName", targetUser.getFullName());
        
        // 将CourseDTO转换为Map
        Map<String, Object> courseInfo = new HashMap<>();
        CourseDTO courseDto = convertToDTO(course, null);
        courseInfo.put("id", courseDto.getId());
        courseInfo.put("name", courseDto.getName());
        courseInfo.put("description", courseDto.getDescription());
        courseInfo.put("creatorId", courseDto.getCreatorId());
        courseInfo.put("status", courseDto.getStatus());
        
        response.put("userInfo", userInfo);
        response.put("courseInfo", courseInfo);
        
        return response;
    }

    /**
     * 实现删除课程方法（逻辑删除）
     */
    @Override
    @Transactional
    public boolean deleteCourse(String courseId) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 查找课程
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException("课程不存在"));
        
        // 验证权限：只有课程创建者和管理员可以删除课程
        if (!course.getCreatorId().equals(currentUser.getId()) && !isAdmin(authentication)) {
            throw new BusinessException("您没有权限删除该课程");
        }
        
        // 验证课程类型必须是COURSE
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            throw new BusinessException("只能删除课程，不能删除签到任务");
        }
        
        // 检查课程是否已经被删除
        if (!course.getActive()) {
            throw new BusinessException("课程已被删除，无需重复操作");
        }
        
        try {
            // 1. 逻辑删除课程下的所有签到任务
            List<Course> checkinTasks = courseRepository.findByParentCourseIdAndActive(courseId, true);
            for (Course checkinTask : checkinTasks) {
                checkinTask.setActive(false);
                checkinTask.setStatus(SystemConstants.CourseStatus.DELETED);
                courseRepository.save(checkinTask);
                
                // 逻辑删除签到记录
                List<CourseRecord> records = courseRecordRepository.findByCourseId(checkinTask.getId());
                for (CourseRecord record : records) {
                    record.setActive(false);
                    courseRecordRepository.save(record);
                }
            }
            
            // 2. 逻辑删除课程
            course.setActive(false);
            course.setStatus(SystemConstants.CourseStatus.DELETED);
            courseRepository.save(course);
            
            log.info("成功删除课程(逻辑删除): ID={}, 名称={}, 创建者={}", courseId, course.getName(), username);
            return true;
        } catch (Exception e) {
            log.error("删除课程失败: ID={}, 原因={}", courseId, e.getMessage(), e);
            throw new BusinessException("删除课程失败: " + e.getMessage());
        }
    }
    
    /**
     * 实现删除签到任务方法（逻辑删除）
     */
    @Override
    @Transactional
    public boolean deleteCheckinTask(String checkinId) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 查找签到任务
        Course checkinTask = courseRepository.findById(checkinId)
            .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        // 验证任务类型必须是CHECKIN
        if (!SystemConstants.CourseType.CHECKIN.equals(checkinTask.getType())) {
            throw new BusinessException("只能删除签到任务，不能删除课程");
        }
        
        // 获取父课程
        Course parentCourse = courseRepository.findById(checkinTask.getParentCourseId())
            .orElseThrow(() -> new BusinessException("所属课程不存在"));
        
        // 验证权限：只有课程创建者和管理员可以删除签到任务
        if (!parentCourse.getCreatorId().equals(currentUser.getId()) && !isAdmin(authentication)) {
            throw new BusinessException("您没有权限删除该签到任务");
        }
        
        // 检查签到任务是否已经被删除
        if (!checkinTask.getActive()) {
            throw new BusinessException("签到任务已被删除，无需重复操作");
        }
        
        try {
            // 1. 逻辑删除签到记录
            List<CourseRecord> records = courseRecordRepository.findByCourseId(checkinId);
            for (CourseRecord record : records) {
                record.setActive(false);
                courseRecordRepository.save(record);
            }
            
            // 2. 逻辑删除签到任务
            checkinTask.setActive(false);
            checkinTask.setStatus(SystemConstants.TaskStatus.DELETED);
            courseRepository.save(checkinTask);
            
            log.info("成功删除签到任务(逻辑删除): ID={}, 名称={}, 创建者={}", checkinId, checkinTask.getName(), username);
            return true;
        } catch (Exception e) {
            log.error("删除签到任务失败: ID={}, 原因={}", checkinId, e.getMessage(), e);
            throw new BusinessException("删除签到任务失败: " + e.getMessage());
        }
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
} 