package com.attendance.model.dto;

import com.attendance.model.entity.Record;
import com.attendance.model.entity.Task;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考勤记录数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDTO {
    
    /**
     * 记录ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private String userId;
    
    /**
     * 用户名称
     */
    private String userName;
    
    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private String taskId;
    
    /**
     * 任务标题
     */
    private String taskTitle;
    
    /**
     * 签到时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;
    
    /**
     * 签到位置
     */
    private String checkInLocation;
    
    /**
     * 签退时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;
    
    /**
     * 签退位置
     */
    private String checkOutLocation;
    
    /**
     * 签到类型
     */
    private Task.CheckInType checkInType;
    
    /**
     * 签到状态
     */
    private Record.RecordStatus status;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 设备信息
     */
    private String deviceInfo;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 实体转DTO
     */
    public static RecordDTO fromEntity(Record record) {
        if (record == null) {
            return null;
        }
        
        RecordDTO dto = new RecordDTO();
        dto.setId(record.getId());
        
        if (record.getUser() != null) {
            dto.setUserId(record.getUser().getId());
            dto.setUserName(record.getUser().getFullName());
        }
        
        if (record.getTask() != null) {
            dto.setTaskId(record.getTask().getId());
            dto.setTaskTitle(record.getTask().getTitle());
        }
        
        dto.setCheckInTime(record.getCheckInTime());
        dto.setCheckInLocation(record.getCheckInLocation());
        dto.setCheckOutTime(record.getCheckOutTime());
        dto.setCheckOutLocation(record.getCheckOutLocation());
        dto.setCheckInType(record.getCheckInType());
        dto.setStatus(record.getStatus());
        dto.setIpAddress(record.getIpAddress());
        dto.setDeviceInfo(record.getDeviceInfo());
        dto.setRemark(record.getRemark());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        
        return dto;
    }
} 