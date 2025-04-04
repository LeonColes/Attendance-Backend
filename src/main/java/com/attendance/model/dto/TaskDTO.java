package com.attendance.model.dto;

import com.attendance.model.entity.Task;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考勤任务数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    
    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 任务状态
     */
    private Task.TaskStatus status;
    
    /**
     * 位置要求
     */
    private String locationRequirement;
    
    /**
     * 签到类型
     */
    @NotNull(message = "签到类型不能为空")
    private Task.CheckInType checkInType;
    
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
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 创建者姓名
     */
    private String creatorName;
    
    /**
     * 实体转DTO
     */
    public static TaskDTO fromEntity(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .status(task.getStatus())
                .locationRequirement(task.getLocationRequirement())
                .checkInType(task.getCheckInType())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .creatorId(task.getCreator().getId())
                .creatorName(task.getCreator().getFullName())
                .build();
    }
    
    /**
     * DTO转实体
     */
    public Task toEntity() {
        return Task.builder()
                .title(this.title)
                .description(this.description)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .status(this.status != null ? this.status : Task.TaskStatus.CREATED)
                .locationRequirement(this.locationRequirement)
                .checkInType(this.checkInType)
                .build();
    }
} 