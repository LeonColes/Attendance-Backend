package com.attendance.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 位置工具类
 */
@Slf4j
public class LocationUtil {
    
    /**
     * 地球半径（米）
     */
    private static final double EARTH_RADIUS = 6371000;
    
    /**
     * 计算两个坐标点之间的距离（米）
     * 
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @return 两点之间的距离（米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 转换为弧度
        double radLat1 = Math.toRadians(lat1);
        double radLng1 = Math.toRadians(lng1);
        double radLat2 = Math.toRadians(lat2);
        double radLng2 = Math.toRadians(lng2);
        
        // 计算差值
        double deltaLat = radLat1 - radLat2;
        double deltaLng = radLng1 - radLng2;
        
        // Haversine公式计算球面距离
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(radLat1) * Math.cos(radLat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        
        log.debug("计算距离: ({}, {}) 到 ({}, {}) = {}米", lat1, lng1, lat2, lng2, distance);
        
        return distance;
    }
    
    /**
     * 从坐标字符串解析纬度（格式: "纬度,经度"）
     * 
     * @param locationStr 坐标字符串，格式为"纬度,经度"
     * @return 纬度
     * @throws IllegalArgumentException 如果坐标格式不正确
     */
    public static double parseLatitude(String locationStr) {
        if (locationStr == null || locationStr.isEmpty() || !locationStr.contains(",")) {
            throw new IllegalArgumentException("无效的位置坐标格式: " + locationStr);
        }
        
        String[] parts = locationStr.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的位置坐标格式，应为'纬度,经度': " + locationStr);
        }
        
        try {
            return Double.parseDouble(parts[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的纬度值: " + parts[0]);
        }
    }
    
    /**
     * 从坐标字符串解析经度（格式: "纬度,经度"）
     * 
     * @param locationStr 坐标字符串，格式为"纬度,经度"
     * @return 经度
     * @throws IllegalArgumentException 如果坐标格式不正确
     */
    public static double parseLongitude(String locationStr) {
        if (locationStr == null || locationStr.isEmpty() || !locationStr.contains(",")) {
            throw new IllegalArgumentException("无效的位置坐标格式: " + locationStr);
        }
        
        String[] parts = locationStr.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的位置坐标格式，应为'纬度,经度': " + locationStr);
        }
        
        try {
            return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的经度值: " + parts[1]);
        }
    }
} 