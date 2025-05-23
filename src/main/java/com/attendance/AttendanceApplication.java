package com.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 考勤管理系统应用启动类
 * 
 * @author attendance
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class AttendanceApplication {

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(AttendanceApplication.class, args);
    }
}