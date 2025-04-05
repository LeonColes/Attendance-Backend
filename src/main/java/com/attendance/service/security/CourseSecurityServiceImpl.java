package com.attendance.service.security;

import com.attendance.model.entity.Course;
import com.attendance.model.entity.CourseUser;
import com.attendance.repository.course.CourseRepository;
import com.attendance.repository.course.CourseUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 课程安全服务实现类
 * 处理与课程相关的权限检查
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseSecurityServiceImpl implements CourseSecurityService {

    private final CourseRepository courseRepository;
    private final CourseUserRepository courseUserRepository;
    /**
     * 检查当前用户是否为课程创建者
     *
     * @param courseId 课程ID
     * @return 是否为课程创建者
     */
    @Override
    public boolean isCourseCreator(String courseId) {
        if (courseId == null || courseId.isEmpty()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String username = authentication.getName();
        log.debug("检查用户 [{}] 是否为课程 [{}] 的创建者", username, courseId);

        // 获取课程信息
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (!courseOpt.isPresent()) {
            log.debug("课程 [{}] 不存在", courseId);
            return false;
        }

        // 如果是创建者，直接返回true
        Optional<CourseUser> courseUserOpt = courseUserRepository.findByCourseIdAndUsername(courseId, username);
        if (courseUserOpt.isPresent() && courseUserOpt.get().isCreator()) {
            log.debug("用户 [{}] 是课程 [{}] 的创建者", username, courseId);
            return true;
        }

        log.debug("用户 [{}] 不是课程 [{}] 的创建者", username, courseId);
        return false;
    }

    /**
     * 检查当前用户是否为签到任务创建者
     *
     * @param checkinId 签到任务ID
     * @return 是否为签到任务创建者
     */
    @Override
    public boolean isCheckinCreator(String checkinId) {
        if (checkinId == null || checkinId.isEmpty()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String username = authentication.getName();
        log.debug("检查用户 [{}] 是否为签到任务 [{}] 的创建者", username, checkinId);

        // 获取签到任务信息
        Optional<Course> checkinOpt = courseRepository.findById(checkinId);
        if (!checkinOpt.isPresent()) {
            log.debug("签到任务 [{}] 不存在", checkinId);
            return false;
        }

        Course checkin = checkinOpt.get();
        
        // 如果是签到任务创建者，直接返回true
        Optional<CourseUser> courseUserOpt = courseUserRepository.findByCourseIdAndUsername(checkin.getParentCourseId(), username);
        if (courseUserOpt.isPresent() && courseUserOpt.get().isCreator()) {
            log.debug("用户 [{}] 是签到任务 [{}] 的创建者", username, checkinId);
            return true;
        }

        log.debug("用户 [{}] 不是签到任务 [{}] 的创建者", username, checkinId);
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
        if (courseId == null || courseId.isEmpty()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String username = authentication.getName();
        log.debug("检查用户 [{}] 是否为课程 [{}] 的成员", username, courseId);

        // 检查用户是否为课程成员
        boolean isMember = courseUserRepository.existsByCourseIdAndUsernameAndActiveTrue(courseId, username);
        
        if (isMember) {
            log.debug("用户 [{}] 是课程 [{}] 的成员", username, courseId);
        } else {
            log.debug("用户 [{}] 不是课程 [{}] 的成员", username, courseId);
        }
        
        return isMember;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String username = authentication.getName();
        
        // 获取课程信息，检查课程状态
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (!courseOpt.isPresent()) {
            log.debug("课程 [{}] 不存在", courseId);
            return false;
        }

        Course course = courseOpt.get();
        
        // 如果课程不处于活跃状态，不能加入
        if (!"ACTIVE".equals(course.getStatus())) {
            log.debug("课程 [{}] 不处于活跃状态，用户 [{}] 不能加入", courseId, username);
            return false;
        }
        
        // 检查用户是否已经是课程成员
        boolean alreadyMember = courseUserRepository.existsByCourseIdAndUsernameAndActiveTrue(courseId, username);
        if (alreadyMember) {
            log.debug("用户 [{}] 已经是课程 [{}] 的成员", username, courseId);
            return false;
        }
        
        log.debug("用户 [{}] 可以加入课程 [{}]", username, courseId);
        return true;
    }
}