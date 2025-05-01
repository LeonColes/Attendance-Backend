package com.attendance.common.config.web;

import com.attendance.common.util.DateTimeUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Jackson配置类
 * 用于配置JSON序列化和反序列化
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置ObjectMapper
     * 增加对LocalDateTime和LocalDate的自定义序列化和反序列化支持
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 添加LocalDateTime的序列化和反序列化器
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        
        // 添加LocalDate的序列化和反序列化器
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer());
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        
        objectMapper.registerModule(javaTimeModule);
        
        // 配置其他Jackson特性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        return objectMapper;
    }

    /**
     * LocalDateTime序列化器
     * 将LocalDateTime序列化为yyyy-MM-dd HH:mm:ss格式
     */
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(DateTimeUtil.formatDateTime(value));
            }
        }
    }

    /**
     * LocalDateTime反序列化器
     * 支持多种格式的反序列化，包括ISO格式(带T)和标准格式
     */
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateTimeStr = p.getValueAsString();
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return null;
            }
            
            return DateTimeUtil.parseDateTime(dateTimeStr);
        }
    }

    /**
     * LocalDate序列化器
     * 将LocalDate序列化为yyyy-MM-dd格式
     */
    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(DateTimeUtil.formatDate(value));
            }
        }
    }

    /**
     * LocalDate反序列化器
     * 支持yyyy-MM-dd格式的反序列化
     */
    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getValueAsString();
            return DateTimeUtil.parseDate(dateStr);
        }
    }
} 