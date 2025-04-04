package com.attendance.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.ServletContextRequestLoggingFilter;

/**
 * 应用初始化配置类
 */
@Configuration
public class InitializationConfig {

    /**
     * 配置请求日志过滤器
     * 用于记录HTTP请求的详细信息
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(10000);
        loggingFilter.setIncludeHeaders(true);
        loggingFilter.setBeforeMessagePrefix("Request received => ");
        loggingFilter.setAfterMessagePrefix("Request processed => ");
        return loggingFilter;
    }
    
    /**
     * 配置Servlet上下文请求日志过滤器
     * 用于记录更详细的请求信息，包括请求路径和参数
     */
    @Bean
    public ServletContextRequestLoggingFilter servletContextRequestLoggingFilter() {
        ServletContextRequestLoggingFilter filter = new ServletContextRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeClientInfo(true);
        filter.setIncludeHeaders(true);
        return filter;
    }
    
    /**
     * 确保日志过滤器有较高的优先级
     */
    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> loggingFilterRegistration(CommonsRequestLoggingFilter filter) {
        FilterRegistrationBean<CommonsRequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
