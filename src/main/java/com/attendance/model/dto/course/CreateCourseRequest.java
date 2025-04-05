package com.attendance.model.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建课程/签到任务请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {
    
    /**
     * 课程/任务名称
     */
    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 100, message = "名称长度必须在2-100个字符之间")
    private String name;
    
    /**
     * 课程/任务描述
     */
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
    /**
     * 类型 (COURSE-普通课程, CHECKIN-签到任务)
     */
    @NotBlank(message = "类型不能为空")
    private String type;
    
    // === 以下字段仅在type=COURSE时使用 ===
    
    /**
     * 课程开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 课程结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    // === 以下字段仅在type=CHECKIN时使用 ===
    
    /**
     * 签到开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkinStartTime;
    
    /**
     * 签到结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkinEndTime;
    
    /**
     * 签到类型 (QR_CODE, LOCATION, WIFI, MANUAL)
     */
    private String checkinType;
    
    /**
     * 验证参数 (JSON格式)
     * 对于位置签到: {"latitude": 纬度, "longitude": 经度, "radius": 半径(米)}
     * 对于WiFi签到: {"ssid": "WiFi名称", "bssid": "MAC地址"}
     * 对于二维码签到: 服务端生成
     */
    private String verifyParams;
    
    /**
     * 所属课程ID (仅当type=CHECKIN时必填)
     */
    private String parentCourseId;
} 