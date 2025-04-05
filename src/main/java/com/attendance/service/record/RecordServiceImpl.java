package com.attendance.service.record;

import com.attendance.common.constants.SystemConstants;
import com.attendance.common.exception.BusinessException;
import com.attendance.model.dto.record.RecordDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 签到记录服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    
    /**
     * 获取签到记录
     * 
     * @param id 记录ID
     * @return 签到记录
     */
    @Override
    public RecordDTO getRecord(String id) {
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("签到记录不存在"));
        
        return convertToDTO(record);
    }
    
    /**
     * 获取签到任务的所有签到记录
     * 
     * @param courseId 签到任务ID
     * @return 签到记录列表
     */
    @Override
    public List<RecordDTO> getRecordsByCourse(String courseId) {
        // 验证签到任务是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        if (!SystemConstants.CourseType.CHECKIN.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        List<Record> records = recordRepository.findByCourseId(courseId);
        
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户的所有签到记录
     * 
     * @param userId 用户ID
     * @return 签到记录列表
     */
    @Override
    public List<RecordDTO> getRecordsByUser(String userId) {
        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        List<Record> records = recordRepository.findByUserId(userId);
        
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取课程的所有签到记录
     * 
     * @param parentCourseId 父课程ID
     * @return 签到记录列表
     */
    @Override
    public List<RecordDTO> getRecordsByParentCourse(String parentCourseId) {
        // 验证课程是否存在
        Course parentCourse = courseRepository.findById(parentCourseId)
                .orElseThrow(() -> new BusinessException("课程不存在"));
        
        if (!SystemConstants.CourseType.COURSE.equals(parentCourse.getType())) {
            throw new BusinessException("指定ID不是有效的课程");
        }
        
        List<Record> records = recordRepository.findByParentCourseId(parentCourseId);
        
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 提交签到
     * 
     * @param courseId 签到任务ID
     * @param verifyMethod 验证方式
     * @param location 位置信息
     * @param device 设备信息
     * @param verifyData 验证数据
     * @return 签到记录
     */
    @Override
    @Transactional
    public RecordDTO submitCheckIn(String courseId, String verifyMethod, String location, String device, String verifyData) {
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证签到任务是否存在且活跃
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("签到任务不存在"));
        
        if (!SystemConstants.CourseType.CHECKIN.equals(course.getType())) {
            throw new BusinessException("指定ID不是有效的签到任务");
        }
        
        if (!SystemConstants.TaskStatus.ACTIVE.equals(course.getStatus())) {
            throw new BusinessException("签到任务未开始或已结束");
        }
        
        // 检查是否已经签到
        if (recordRepository.findByUserIdAndCourseId(user.getId(), courseId).isPresent()) {
            throw new BusinessException("您已经签到过该任务");
        }
        
        // 验证签到数据（简化版，实际应根据不同验证方式进行详细验证）
        String checkInStatus = validateCheckIn(course, verifyMethod, verifyData, location);
        
        // 创建签到记录
        Record record = new Record();
        record.setUserId(user.getId());
        record.setCourseId(courseId);
        record.setParentCourseId(course.getParentCourseId());
        record.setStatus(checkInStatus);
        record.setCheckInTime(LocalDateTime.now());
        record.setLocation(location);
        record.setDevice(device);
        record.setVerifyMethod(verifyMethod);
        record.setVerifyData(verifyData);
        
        // 保存记录
        Record savedRecord = recordRepository.save(record);
        
        return convertToDTO(savedRecord);
    }
    
    /**
     * 验证签到数据
     * 
     * @param course 签到任务对象
     * @param verifyMethod 验证方式
     * @param verifyData 验证数据
     * @param location 位置信息
     * @return 签到状态
     */
    private String validateCheckIn(Course course, String verifyMethod, String verifyData, String location) {
        // 检查是否迟到
        LocalDateTime now = LocalDateTime.now();
        
        // 任务已结束，不能签到
        if (course.getCheckinEndTime() != null && now.isAfter(course.getCheckinEndTime())) {
            throw new BusinessException("签到任务已结束，无法签到");
        }
        
        // 任务未开始，不能签到
        if (course.getCheckinStartTime() != null && now.isBefore(course.getCheckinStartTime())) {
            throw new BusinessException("签到任务尚未开始，请稍后再试");
        }
        
        // 判断签到状态：正常或迟到
        if (course.getCheckinStartTime() != null && course.getCheckinEndTime() != null) {
            // 计算任务总时长
            long totalDuration = java.time.Duration.between(
                course.getCheckinStartTime(), 
                course.getCheckinEndTime()
            ).toMinutes();
            
            // 计算已经过去的时长
            long elapsedDuration = java.time.Duration.between(
                course.getCheckinStartTime(), 
                now
            ).toMinutes();
            
            // 如果已经过去了签到时间的80%，标记为迟到
            if (elapsedDuration > totalDuration * 0.8) {
                return SystemConstants.RecordStatus.LATE;
            }
        }
        
        return SystemConstants.RecordStatus.NORMAL;
        
        // 注：实际实现中应根据不同的签到类型(二维码、位置、WiFi等)进行详细验证
    }
    
    /**
     * 转换实体到DTO
     * 
     * @param record 签到记录实体
     * @return 签到记录DTO
     */
    private RecordDTO convertToDTO(Record record) {
        // 查询关联用户信息
        User user = userRepository.findById(record.getUserId())
                .orElse(null);
        
        // 查询关联签到任务信息
        Course course = courseRepository.findById(record.getCourseId())
                .orElse(null);
        
        // 查询关联父课程信息
        Course parentCourse = null;
        if (record.getParentCourseId() != null) {
            parentCourse = courseRepository.findById(record.getParentCourseId())
                    .orElse(null);
        }
        
        RecordDTO dto = RecordDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .courseId(record.getCourseId())
                .parentCourseId(record.getParentCourseId())
                .status(record.getStatus())
                .checkInTime(record.getCheckInTime())
                .location(record.getLocation())
                .device(record.getDevice())
                .verifyMethod(record.getVerifyMethod())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
        
        // 填充关联信息
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserFullName(user.getFullName());
        }
        
        if (course != null) {
            dto.setCourseName(course.getName());
        }
        
        if (parentCourse != null) {
            dto.setParentCourseName(parentCourse.getName());
        }
        
        return dto;
    }
} 