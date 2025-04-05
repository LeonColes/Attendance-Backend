package com.attendance.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统一分页请求DTO
 */
@Data
public class PageRequestDTO {

    /**
     * 当前页码（从0开始）
     */
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Integer size = 10;

    /**
     * 排序字段列表
     */
    private List<SortField> sort = new ArrayList<>();

    /**
     * 过滤条件
     */
    private Map<String, Object> filters;

    /**
     * 排序字段
     */
    @Data
    public static class SortField {
        /**
         * 字段名
         */
        private String field;

        /**
         * 排序方向（ASC/DESC）
         */
        private String direction = "DESC";
    }
} 