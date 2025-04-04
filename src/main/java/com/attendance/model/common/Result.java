package com.attendance.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口响应结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    /**
     * 成功响应（不带数据）
     */
    public static <T> Result<T> success() {
        return new Result<T>(200, "操作成功", null);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<T>(code, message, null);
    }
    
    /**
     * 服务器内部错误
     */
    public static <T> Result<T> serverError() {
        return new Result<T>(500, "服务器内部错误", null);
    }
    
    /**
     * 参数错误
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<T>(400, message, null);
    }
    
    /**
     * 未授权错误
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<T>(401, message, null);
    }
    
    /**
     * 权限不足错误
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<T>(403, message, null);
    }
    
    /**
     * 资源不存在错误
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<T>(404, message, null);
    }
} 