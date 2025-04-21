package com.attendance.model.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建签到任务请求DTO
 */
@Data
public class CreateAttendanceRequest {
    
    /**
     * 所属课程ID
     */
    @NotBlank(message = "课程ID不能为空")
    private String courseId;
    
    /**
     * 签到任务标题
     */
    @NotBlank(message = "签到任务标题不能为空")
    private String title;
    
    /**
     * 签到任务描述
     */
    private String description;
    
    /**
     * 签到开始时间
     */
    @NotNull(message = "签到开始时间不能为空")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 签到结束时间
     */
    @NotNull(message = "签到结束时间不能为空")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 签到类型: QR_CODE, LOCATION, WIFI, MANUAL
     */
    @NotBlank(message = "签到类型不能为空")
    private String checkInType;
    
    /**
     * 签到验证参数, JSON格式
     * 根据签到类型不同, 内容有所区别:
     * QR_CODE: {}
     * LOCATION: {"latitude": 纬度, "longitude": 经度, "radius": 半径}
     * WIFI: {"ssid": "WiFi名称", "bssid": "MAC地址"}
     */
    private String verifyParams;
}