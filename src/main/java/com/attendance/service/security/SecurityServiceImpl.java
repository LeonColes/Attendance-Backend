package com.attendance.service.security;

import com.attendance.common.constants.SystemConstants;
import com.attendance.model.entity.Course;
import com.attendance.model.entity.Record;
import com.attendance.model.entity.User;
import com.attendance.repository.course.CourseRepository;
import com.attendance.repository.record.RecordRepository;
import com.attendance.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 安全服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
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
     * @param courseId 课程或任务ID
     * @return 是否可以访问课程任务
     */
    @Override
    public boolean canAccessTask(String courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        // 检查用户角色
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.ADMIN));
        
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_" + SystemConstants.UserRole.TEACHER));
        
        // 管理员和教师拥有访问所有任务的权限
        if (isAdmin || isTeacher) {
            return true;
        }
        
        // 查询任务
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        Course course = courseOpt.get();
        
        // 普通用户只能访问与自己相关的任务
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 任务创建者可以访问
        if (course.getCreatorId().equals(user.getId())) {
            return true;
        }
        
        // 检查用户是否参与了该任务（有签到记录）
        return recordRepository.findByUserIdAndCourseId(user.getId(), courseId).isPresent();
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
        String currentUsername = authentication.getName();
        
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
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 用户只能访问自己的签到记录
        return record.getUserId().equals(user.getId());
    }
} 