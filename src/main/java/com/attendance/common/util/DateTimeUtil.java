package com.attendance.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * 日期时间工具类
 */
public class DateTimeUtil {
    
    /**
     * 标准日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String STANDARD_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * ISO日期时间格式：yyyy-MM-dd'T'HH:mm:ss
     */
    public static final String ISO_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    
    /**
     * 标准日期格式：yyyy-MM-dd
     */
    public static final String STANDARD_DATE_PATTERN = "yyyy-MM-dd";
    
    /**
     * 标准日期时间格式化器
     */
    public static final DateTimeFormatter STANDARD_DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern(STANDARD_DATETIME_PATTERN);
    
    /**
     * ISO格式时间格式化器
     */
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern(ISO_DATETIME_PATTERN);
    
    /**
     * 标准日期格式化器
     */
    public static final DateTimeFormatter STANDARD_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern(STANDARD_DATE_PATTERN);
    
    /**
     * 灵活的日期时间格式化器，支持带T的ISO格式和标准格式
     */
    public static final DateTimeFormatter FLEXIBLE_DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd[ ]['T']HH:mm:ss");
    
    /**
     * 灵活的日期时间格式化器，支持小时位缺少前导零的情况
     * 例如: 2025-05-02 0:00:00
     */
    public static final DateTimeFormatter LENIENT_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd[ ]['T']")
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendPattern(":mm:ss")
            .toFormatter();
    
    /**
     * 格式化LocalDateTime为标准格式字符串
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return STANDARD_DATETIME_FORMATTER.format(dateTime);
    }
    
    /**
     * 格式化LocalDate为标准格式字符串
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return STANDARD_DATE_FORMATTER.format(date);
    }
    
    /**
     * 解析日期时间字符串为LocalDateTime
     * 支持ISO格式和标准格式，以及非标准格式如小时位缺少前导零
     *
     * @param dateTimeStr 日期时间字符串
     * @return 解析后的LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeStr, FLEXIBLE_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // 尝试使用更宽松的格式解析器，处理小时位缺少前导零的情况
                // 例如: 2025-05-02 0:00:00
                return LocalDateTime.parse(dateTimeStr, LENIENT_DATETIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                // 忽略此异常，继续尝试其他格式
            }
            
            try {
                // 尝试手动格式化处理并解析
                if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{1}:\\d{2}:\\d{2}")) {
                    // 处理小时位缺少前导零的情况
                    String[] parts = dateTimeStr.split(" ");
                    String datePart = parts[0];
                    String[] timeParts = parts[1].split(":");
                    String formattedTime = String.format("%02d:%s:%s", 
                                         Integer.parseInt(timeParts[0]), 
                                         timeParts[1], 
                                         timeParts[2]);
                    String formattedDateTime = datePart + " " + formattedTime;
                    
                    return LocalDateTime.parse(formattedDateTime, STANDARD_DATETIME_FORMATTER);
                }
            } catch (Exception ignored) {
                // 忽略此异常，继续尝试其他格式
            }
            
            try {
                // 尝试手动去除T字符
                if (dateTimeStr.contains("T")) {
                    String converted = dateTimeStr.replace("T", " ");
                    return LocalDateTime.parse(converted, STANDARD_DATETIME_FORMATTER);
                }
            } catch (DateTimeParseException ignored) {
                // 忽略此异常，继续尝试其他格式
            }
            
            try {
                // 尝试ISO格式
                return LocalDateTime.parse(dateTimeStr, ISO_DATETIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                // 忽略此异常，继续尝试其他格式
            }
            
            try {
                // 尝试标准格式
                return LocalDateTime.parse(dateTimeStr, STANDARD_DATETIME_FORMATTER);
            } catch (DateTimeParseException ignoredAgain) {
                throw new IllegalArgumentException("无法解析日期时间字符串: " + dateTimeStr, e);
            }
        }
    }
    
    /**
     * 解析日期字符串为LocalDate
     *
     * @param dateStr 日期字符串
     * @return 解析后的LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, STANDARD_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无法解析日期字符串: " + dateStr, e);
        }
    }
    
    /**
     * 转换ISO格式的日期时间字符串为标准格式
     * 例如: 2023-01-01T12:30:45 -> 2023-01-01 12:30:45
     *
     * @param isoDateTimeStr ISO格式的日期时间字符串
     * @return 标准格式的日期时间字符串
     */
    public static String convertIsoToStandard(String isoDateTimeStr) {
        if (isoDateTimeStr == null || isoDateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        if (isoDateTimeStr.contains("T")) {
            return isoDateTimeStr.replace("T", " ");
        }
        
        return isoDateTimeStr;
    }
    
    /**
     * 将Date转换为LocalDateTime
     *
     * @param date Date对象
     * @return LocalDateTime对象
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * 将LocalDateTime转换为Date
     *
     * @param localDateTime LocalDateTime对象
     * @return Date对象
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * 将LocalDate转换为Date
     *
     * @param localDate LocalDate对象
     * @return Date对象
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * 格式化Date为标准格式字符串
     *
     * @param date Date对象
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return formatDateTime(toLocalDateTime(date));
    }
} 