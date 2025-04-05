package com.attendance.service.security;

import com.attendance.common.constants.SystemConstants;
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

import java.util.Optional;

/**
 * 统一安全服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseUserRepository courseUserRepository;
    private final RecordRepository recordRepository;
    
    /**
     * 检查是否为当前用户
     * 
     * @param userId 用户ID
     * @return 是否为当前用户
     */
    @Override
    public boolean isCurrentUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        return currentUsername.equals(userOpt.get().getUsername());
    }
    
    /**
     * 检查是否可以访问课程任务
     * 
     * @param courseId 课程或签到任务ID
     * @return 是否可以访问课程任务
     */
    @Override
    public boolean canAccessCourse(String courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查用户角色
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.ADMIN));
        
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.TEACHER));
        
        // 管理员和教师拥有访问所有课程的权限
        if (isAdmin || isTeacher) {
            return true;
        }
        
        // 检查是否为课程成员
        return isCourseMember(courseId);
    }
    
    /**
     * 检查是否可以访问签到记录
     * 
     * @param recordId 记录ID
     * @return 是否可以访问签到记录
     */
    @Override
    public boolean canAccessRecord(String recordId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查用户角色
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.ADMIN));
        
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.TEACHER));
        
        // 管理员和教师拥有访问所有记录的权限
        if (isAdmin || isTeacher) {
            return true;
        }
        
        // 查询记录
        Optional<Record> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }
        
        Record record = recordOpt.get();
        
        // 普通用户只能访问自己的记录
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 用户只能访问自己的签到记录
        return record.getUserId().equals(user.getId());
    }
    
    /**
     * 检查当前用户是否为课程创建者
     *
     * @param courseId 课程ID
     * @return 是否为课程创建者
     */
    @Override
    public boolean isCourseCreator(String courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            return false;
        }
        
        // 管理员可以管理所有课程
        if (authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        
        String username = authentication.getName();
        
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        Course course = courseOpt.get();
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        
        return user.getId().equals(course.getCreatorId());
    }
    
    /**
     * 检查当前用户是否为课程管理员(创建者或助教)
     *
     * @param courseId 课程ID
     * @return 是否为课程管理员
     */
    @Override
    public boolean isCourseAdmin(String courseId) {
        if (courseId == null || courseId.isEmpty()) {
            return false;
        }

        // 管理员可以管理所有课程
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.ADMIN));
        
        if (isAdmin) {
            return true;
        }
        
        // 先检查是否为创建者
        if (isCourseCreator(courseId)) {
            return true;
        }
        
        // 获取当前用户
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 检查是否为助教
        Optional<CourseUser> courseUserOpt = courseUserRepository.findByCourseIdAndUserId(courseId, user.getId());
        return courseUserOpt.isPresent() && 
               SystemConstants.CourseUserRole.ASSISTANT.equals(courseUserOpt.get().getRole());
    }
    
    /**
     * 检查当前用户是否为课程成员
     *
     * @param courseId 课程ID
     * @return 是否为课程成员
     */
    @Override
    public boolean isCourseMember(String courseId) {
        if (courseId == null || courseId.isEmpty()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 获取用户信息
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 查询用户-课程关联
        Optional<CourseUser> courseUserOpt = courseUserRepository.findByCourseIdAndUserId(courseId, user.getId());
        return courseUserOpt.isPresent() && courseUserOpt.get().getActive();
    }
    
    /**
     * 检查用户是否可以加入课程
     *
     * @param courseId 课程ID
     * @return 是否可以加入课程
     */
    @Override
    public boolean canJoinCourse(String courseId) {
        if (courseId == null || courseId.isEmpty()) {
            return false;
        }

        // 获取课程信息
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        Course course = courseOpt.get();
        
        // 如果课程不是COURSE类型或者不是活跃状态，不能加入
        if (!SystemConstants.CourseType.COURSE.equals(course.getType()) || 
            !SystemConstants.CourseStatus.ACTIVE.equals(course.getStatus())) {
            return false;
        }
        
        // 检查用户是否已经是课程成员
        if (isCourseMember(courseId)) {
            return false;
        }
        
        return true;
    }
} 