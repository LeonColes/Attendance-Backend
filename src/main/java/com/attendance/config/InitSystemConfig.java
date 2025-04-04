package com.attendance.config;

import com.attendance.model.entity.User;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 系统初始化配置
 * 用于在系统启动时执行一些初始化操作
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitSystemConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${admin.username:admin}")
    private String adminUsername;
    
    @Value("${admin.password:123456}")
    private String adminPassword;
    
    @Value("${admin.fullName:系统管理员}")
    private String adminFullName;
    
    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    /**
     * 初始化系统管理员账户
     * 如果系统中没有系统管理员，则创建一个默认的系统管理员账户
     */
    @Bean
    public CommandLineRunner initSystemAdmin() {
        return args -> {
            // 检查是否存在系统管理员
            boolean hasSystemAdmin = userRepository.findAll().stream()
                    .anyMatch(user -> user.getRole() == User.Role.SYSTEM_ADMIN);
            
            if (!hasSystemAdmin) {
                log.info("系统初始化: 未检测到系统管理员，创建默认系统管理员账户");
                
                User admin = User.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .fullName(adminFullName)
                        .email(adminEmail)
                        .role(User.Role.SYSTEM_ADMIN)
                        .enabled(true)
                        .build();
                
                userRepository.save(admin);
                log.info("系统初始化: 默认系统管理员创建成功，用户名: {}", adminUsername);
            } else {
                log.info("系统初始化: 系统管理员已存在，跳过创建");
            }
        };
    }
}