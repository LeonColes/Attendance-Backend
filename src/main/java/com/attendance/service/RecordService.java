package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.model.dto.RecordDTO;
import com.attendance.model.entity.Record;
import com.attendance.model.entity.Task;
import com.attendance.model.entity.User;
import com.attendance.repository.RecordRepository;
import com.attendance.repository.TaskRepository;
import com.attendance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 考勤记录服务
 */
@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);
    
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    
    public RecordService(RecordRepository recordRepository, 
                          UserRepository userRepository, 
                          TaskRepository taskRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }
    
    /**
     * 创建考勤记录
     * 
     * @param recordDTO 考勤记录DTO
     * @return 创建的考勤记录
     */
    @Transactional
    public RecordDTO createRecord(RecordDTO recordDTO) {
        log.info("创建考勤记录，用户ID: {}, 任务ID: {}", recordDTO.getUserId(), recordDTO.getTaskId());
        
        // 获取用户
        User user = userRepository.findById(recordDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + recordDTO.getUserId()));
        
        // 获取任务
        Task task = taskRepository.findById(recordDTO.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + recordDTO.getTaskId()));
        
        // 检查任务是否处于活跃状态
        if (task.getStatus() != Task.TaskStatus.ACTIVE) {
            throw new BusinessException("只能签到活跃状态的任务");
        }
        
        // 检查是否已经有考勤记录
        if (recordRepository.findByUserAndTask(user, task).isPresent()) {
            throw new BusinessException("已经存在该用户的考勤记录");
        }
        
        // 创建考勤记录
        Record record = new Record();
        record.setUser(user);
        record.setTask(task);
        record.setCheckInTime(LocalDateTime.now());
        record.setCheckInLocation(recordDTO.getCheckInLocation());
        record.setCheckInType(task.getCheckInType());
        record.setStatus(determineStatus(task));
        record.setIpAddress(recordDTO.getIpAddress());
        record.setDeviceInfo(recordDTO.getDeviceInfo());
        record.setRemark(recordDTO.getRemark());
        
        // 保存考勤记录
        Record savedRecord = recordRepository.save(record);
        log.info("考勤记录创建成功，ID: {}", savedRecord.getId());
        
        return RecordDTO.fromEntity(savedRecord);
    }
    
    /**
     * 更新考勤记录（签退）
     * 
     * @param id 考勤记录ID
     * @param recordDTO 考勤记录DTO
     * @return 更新后的考勤记录
     */
    @Transactional
    public RecordDTO updateRecord(String id, RecordDTO recordDTO) {
        log.info("更新考勤记录，ID: {}", id);
        
        // 查找考勤记录
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤记录不存在，ID: " + id));
        
        // 更新签退信息
        record.setCheckOutTime(LocalDateTime.now());
        record.setCheckOutLocation(recordDTO.getCheckOutLocation());
        record.setRemark(recordDTO.getRemark());
        
        // 更新状态
        record.setStatus(determineCheckoutStatus(record));
        
        // 保存更新
        Record updatedRecord = recordRepository.save(record);
        log.info("考勤记录更新成功，ID: {}", updatedRecord.getId());
        
        return RecordDTO.fromEntity(updatedRecord);
    }
    
    /**
     * 获取考勤记录详情
     * 
     * @param id 考勤记录ID
     * @return 考勤记录详情
     */
    public RecordDTO getRecordById(String id) {
        log.info("获取考勤记录详情，ID: {}", id);
        
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤记录不存在，ID: " + id));
        
        return RecordDTO.fromEntity(record);
    }
    
    /**
     * 获取用户的考勤记录
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RecordDTO> getRecordsByUserId(String userId, Pageable pageable) {
        log.info("获取用户的考勤记录，用户ID: {}", userId);
        
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("用户不存在，ID: " + userId);
        }
        
        return recordRepository.findByUserId(userId, pageable)
                .map(RecordDTO::fromEntity);
    }
    
    /**
     * 获取任务的考勤记录
     * 
     * @param taskId 任务ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RecordDTO> getRecordsByTaskId(String taskId, Pageable pageable) {
        log.info("获取任务的考勤记录，任务ID: {}", taskId);
        
        // 检查任务是否存在
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("考勤任务不存在，ID: " + taskId);
        }
        
        return recordRepository.findByTaskId(taskId, pageable)
                .map(RecordDTO::fromEntity);
    }
    
    /**
     * 根据状态获取考勤记录
     * 
     * @param status 考勤状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<RecordDTO> getRecordsByStatus(Record.RecordStatus status, Pageable pageable) {
        log.info("获取特定状态的考勤记录，状态: {}", status);
        
        return recordRepository.findByStatus(status, pageable)
                .map(RecordDTO::fromEntity);
    }
    
    /**
     * 获取任务的考勤统计
     * 包括已签到、迟到、请假和未签到人数
     * 
     * @param taskId 任务ID
     * @return 考勤统计结果
     */
    public Map<String, Long> getTaskStatistics(String taskId) {
        log.info("获取任务的考勤统计，任务ID: {}", taskId);
        
        // 检查任务是否存在
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("考勤任务不存在，ID: " + taskId);
        }
        
        // 获取统计数据
        List<Object[]> statistics = recordRepository.countByTaskIdGroupByStatus(taskId);
        
        // 转换为Map<String, Long>
        Map<String, Long> result = new HashMap<>();
        statistics.forEach(stat -> {
            Record.RecordStatus status = (Record.RecordStatus) stat[0];
            Long count = (Long) stat[1];
            result.put(status.name(), count);
        });
        
        // 获取未签到学生人数
        long absentCount = recordRepository.countAbsentStudentsForTask(taskId);
        result.put("ABSENT", absentCount);
        
        return result;
    }
    
    /**
     * 获取时间范围内的用户考勤记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 考勤记录列表
     */
    public List<RecordDTO> getUserRecordsInTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("获取时间范围内的用户考勤记录，用户ID: {}, 开始时间: {}, 结束时间: {}", userId, startTime, endTime);
        
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("用户不存在，ID: " + userId);
        }
        
        // 验证时间范围
        if (endTime.isBefore(startTime)) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        
        return recordRepository.findByUserIdAndCheckInTimeBetween(userId, startTime, endTime)
                .stream()
                .map(RecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 批准异常考勤记录
     * 
     * @param id 考勤记录ID
     * @param remark 备注
     * @return 更新后的考勤记录
     */
    @Transactional
    public RecordDTO approveRecord(String id, String remark) {
        log.info("批准异常考勤记录，ID: {}", id);
        
        // 查找考勤记录
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤记录不存在，ID: " + id));
        
        // 更新状态和备注
        record.setStatus(Record.RecordStatus.APPROVED);
        record.setRemark(remark);
        
        // 保存更新
        Record updatedRecord = recordRepository.save(record);
        log.info("考勤记录已批准，ID: {}", updatedRecord.getId());
        
        return RecordDTO.fromEntity(updatedRecord);
    }
    
    /**
     * 拒绝异常考勤记录
     * 
     * @param id 考勤记录ID
     * @param remark 备注
     * @return 更新后的考勤记录
     */
    @Transactional
    public RecordDTO rejectRecord(String id, String remark) {
        log.info("拒绝异常考勤记录，ID: {}", id);
        
        // 查找考勤记录
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤记录不存在，ID: " + id));
        
        // 更新状态和备注
        record.setStatus(Record.RecordStatus.REJECTED);
        record.setRemark(remark);
        
        // 保存更新
        Record updatedRecord = recordRepository.save(record);
        log.info("考勤记录已拒绝，ID: {}", updatedRecord.getId());
        
        return RecordDTO.fromEntity(updatedRecord);
    }
    
    /**
     * 设置考勤状态为请假
     * 
     * @param id 考勤记录ID
     * @param remark 备注
     * @return 更新后的考勤记录
     */
    @Transactional
    public RecordDTO setLeaveStatus(String id, String remark) {
        log.info("设置考勤状态为请假，ID: {}", id);
        
        // 查找考勤记录
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考勤记录不存在，ID: " + id));
        
        // 更新状态和备注
        record.setStatus(Record.RecordStatus.LEAVE);
        record.setRemark(remark);
        
        // 保存更新
        Record updatedRecord = recordRepository.save(record);
        log.info("考勤记录已设置为请假，ID: {}", updatedRecord.getId());
        
        return RecordDTO.fromEntity(updatedRecord);
    }
    
    /**
     * 根据任务确定签到状态
     */
    private Record.RecordStatus determineStatus(Task task) {
        LocalDateTime now = LocalDateTime.now();
        
        // 如果在任务开始时间之前
        if (now.isBefore(task.getStartTime())) {
            return Record.RecordStatus.NORMAL;
        }
        
        // 如果在任务开始时间后30分钟之内（可配置）
        if (now.isBefore(task.getStartTime().plusMinutes(30))) {
            return Record.RecordStatus.NORMAL;
        }
        
        // 否则视为迟到
        return Record.RecordStatus.LATE;
    }
    
    /**
     * 根据任务确定签退状态
     */
    private Record.RecordStatus determineCheckoutStatus(Record record) {
        LocalDateTime now = LocalDateTime.now();
        Task task = record.getTask();
        
        // 保持原有状态，除非是早退
        if (now.isBefore(task.getEndTime())) {
            return Record.RecordStatus.EARLY_LEAVE;
        }
        
        // 如果是迟到，保持迟到状态
        if (record.getStatus() == Record.RecordStatus.LATE) {
            return Record.RecordStatus.LATE;
        }
        
        return Record.RecordStatus.NORMAL;
    }
    
    /**
     * 获取用户的所有考勤记录（不分页）
     * 按签到时间倒序排列，最新记录在前
     * 
     * @param userId 用户ID
     * @return 考勤记录列表
     */
    public List<RecordDTO> getAllRecordsByUserId(String userId) {
        log.info("获取用户的所有考勤记录，用户ID: {}", userId);
        
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("用户不存在，ID: " + userId);
        }
        
        return recordRepository.findByUserIdOrderByCheckInTimeDesc(userId)
                .stream()
                .map(RecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务的所有考勤记录（不分页）
     * 按签到时间倒序排列，最新记录在前
     * 
     * @param taskId 任务ID
     * @return 考勤记录列表
     */
    public List<RecordDTO> getAllRecordsByTaskId(String taskId) {
        log.info("获取任务的所有考勤记录，任务ID: {}", taskId);
        
        // 检查任务是否存在
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("考勤任务不存在，ID: " + taskId);
        }
        
        return recordRepository.findByTaskIdOrderByCheckInTimeDesc(taskId)
                .stream()
                .map(RecordDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建补签记录（迟到签到申请）
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param remark 补签说明
     * @return 创建的补签记录
     */
    @Transactional
    public RecordDTO createLateSignRecord(String taskId, String userId, String remark) {
        log.info("创建补签申请，用户ID: {}, 任务ID: {}", userId, taskId);
        
        // 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + userId));
        
        // 获取任务
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + taskId));
        
        // 检查任务是否已结束
        if (task.getStatus() == Task.TaskStatus.CREATED) {
            throw new BusinessException("任务尚未开始，不能申请补签");
        }
        
        // 检查是否已经有考勤记录
        if (recordRepository.findByUserAndTask(user, task).isPresent()) {
            throw new BusinessException("已经存在该用户的考勤记录，不能重复补签");
        }
        
        // 创建考勤记录（补签状态）
        Record record = new Record();
        record.setUser(user);
        record.setTask(task);
        record.setCheckInTime(LocalDateTime.now());
        record.setStatus(Record.RecordStatus.PENDING);
        record.setRemark(remark);
        
        // 保存考勤记录
        Record savedRecord = recordRepository.save(record);
        log.info("补签申请创建成功，ID: {}", savedRecord.getId());
        
        return RecordDTO.fromEntity(savedRecord);
    }
    
    /**
     * 检查用户是否已经签到过某个任务
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 是否已签到
     */
    public boolean hasUserSignedIn(String userId, String taskId) {
        log.info("检查用户是否已签到，用户ID: {}, 任务ID: {}", userId, taskId);
        
        // 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + userId));
        
        // 获取任务
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("考勤任务不存在，ID: " + taskId));
        
        // 检查是否已有签到记录
        return recordRepository.findByUserAndTask(user, task).isPresent();
    }
    
    /**
     * 获取用户在特定任务的签到记录
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 用户在该任务的签到记录列表
     */
    public List<RecordDTO> getAllRecordsByUserIdAndTaskId(String userId, String taskId) {
        log.info("获取用户在特定任务的签到记录，用户ID: {}, 任务ID: {}", userId, taskId);
        
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("用户不存在，ID: " + userId);
        }
        
        // 检查任务是否存在
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("考勤任务不存在，ID: " + taskId);
        }
        
        // 查找用户在特定任务的签到记录
        Optional<Record> recordOpt = recordRepository.findByUserIdAndTaskId(userId, taskId);
        
        // 转换为列表形式返回
        return recordOpt.map(record -> List.of(RecordDTO.fromEntity(record)))
                        .orElse(List.of());
    }
}