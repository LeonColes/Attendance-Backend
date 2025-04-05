package com.attendance.service.security;

import com.attendance.common.constants.SystemConstants;
import com.attendance.model.entity.Course;
import com.attendance.model.entity.User;
import com.attendance.repository.course.CourseRepository;
import com.attendance.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 课程安全服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseSecurityServiceImpl implements CourseSecurityService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    /**
     * 检查当前用户是否为课程创建者
     * 
     * @param courseId 课程ID
     * @return 是否为课程创建者
     */
    @Override
    public boolean isCourseCreator(String courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        // 查询课程
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        Course course = courseOpt.get();
        
        // 验证课程类型
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            return false;
        }
        
        // 查询当前用户
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 检查是否为创建者
        return course.getCreatorId().equals(user.getId());
    }
    
    /**
     * 检查当前用户是否为签到任务创建者
     * 
     * @param checkinId 签到任务ID
     * @return 是否为签到任务创建者
     */
    @Override
    public boolean isCheckinCreator(String checkinId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        // 查询签到任务
        Optional<Course> checkinOpt = courseRepository.findById(checkinId);
        if (checkinOpt.isEmpty()) {
            return false;
        }
        
        Course checkin = checkinOpt.get();
        
        // 验证任务类型
        if (!SystemConstants.CourseType.CHECKIN.equals(checkin.getType())) {
            return false;
        }
        
        // 查询当前用户
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 检查是否为创建者
        if (checkin.getCreatorId().equals(user.getId())) {
            return true;
        }
        
        // 如果不是直接创建者，检查是否为父课程的创建者
        if (checkin.getParentCourseId() != null) {
            Optional<Course> parentOpt = courseRepository.findById(checkin.getParentCourseId());
            if (parentOpt.isPresent() && parentOpt.get().getCreatorId().equals(user.getId())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查当前用户是否为课程成员
     * 
     * @param courseId 课程ID
     * @return 是否为课程成员
     */
    @Override
    public boolean isCourseMember(String courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        // 查询课程
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        // 查询当前用户
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        Course course = courseOpt.get();
        
        // 创建者默认为成员
        if (course.getCreatorId().equals(user.getId())) {
            return true;
        }
        
        // TODO: 实际项目中应查询课程-用户关联表确认成员身份
        // 此处为临时实现，后续需完善
        return false;
    }
    
    /**
     * 检查用户是否可以加入课程
     * 
     * @param courseId 课程ID
     * @return 是否可以加入课程
     */
    @Override
    public boolean canJoinCourse(String courseId) {
        // 查询课程
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        
        Course course = courseOpt.get();
        
        // 验证课程类型
        if (!SystemConstants.CourseType.COURSE.equals(course.getType())) {
            return false;
        }
        
        // 验证课程状态
        return SystemConstants.CourseStatus.ACTIVE.equals(course.getStatus());
    }
} 