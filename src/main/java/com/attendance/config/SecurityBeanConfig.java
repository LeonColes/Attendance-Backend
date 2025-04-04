package com.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.attendance.security.Argon2PasswordEncoder;

/**
 * 应用安全配置
 * 配置密码编码器和其他安全相关组件
 */
@Configuration
public class SecurityBeanConfig {

    /**
     * 配置密码编码器
     * 确保系统中所有需要密码验证的地方使用同一个编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 创建新的Argon2PasswordEncoder实例，不再依赖自动注册的组件
        return new Argon2PasswordEncoder();
    }
}