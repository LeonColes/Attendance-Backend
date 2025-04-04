package com.attendance.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson配置类
 * 设置全局日期时间格式化选项
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义Jackson配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // 配置日期时间格式
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            builder.serializers(new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            builder.serializers(new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 配置反序列化器
            builder.deserializers(new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            builder.deserializers(new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        };
    }
} 