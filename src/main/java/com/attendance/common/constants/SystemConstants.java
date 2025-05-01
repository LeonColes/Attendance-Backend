package com.attendance.common.constants;

/**
 * 系统常量定义
 */
public final class SystemConstants {
    
    /**
     * 用户角色
     */
    public static final class UserRole {
        /**
         * 学生角色
         */
        public static final String STUDENT = "STUDENT";
        
        /**
         * 教师角色
         */
        public static final String TEACHER = "TEACHER";
        
        /**
         * 管理员角色
         */
        public static final String ADMIN = "ADMIN";
        
        // 私有构造函数，防止实例化
        private UserRole() {}

        /**
         * 判断角色是否有效
         * @param role 角色值
         * @return 是否有效
         */
        public static boolean isValidRole(String role) {
            return ADMIN.equals(role) || 
                   TEACHER.equals(role) || 
                   STUDENT.equals(role);
        }
    }
    
    /**
     * 任务状态
     */
    public static final class TaskStatus {
        /**
         * 已创建
         */
        public static final String CREATED = "CREATED";
        
        /**
         * 进行中
         */
        public static final String ACTIVE = "ACTIVE";
        
        /**
         * 已完成
         */
        public static final String COMPLETED = "COMPLETED";
        
        /**
         * 已结束
         */
        public static final String ENDED = "ENDED";
        
        /**
         * 已取消
         */
        public static final String CANCELED = "CANCELED";
        
        /**
         * 已删除（逻辑删除）
         */
        public static final String DELETED = "DELETED";
        
        // 私有构造函数，防止实例化
        private TaskStatus() {}

        /**
         * 判断状态是否有效
         * @param status 状态值
         * @return 是否有效
         */
        public static boolean isValidStatus(String status) {
            return CREATED.equals(status) || 
                   ACTIVE.equals(status) || 
                   ENDED.equals(status) || 
                   CANCELED.equals(status) ||
                   COMPLETED.equals(status) ||
                   DELETED.equals(status);
        }
    }
    
    /**
     * 签到类型
     */
    public static final class CheckInType {
        /**
         * 二维码签到
         */
        public static final String QR_CODE = "QR_CODE";
        
        /**
         * 位置签到
         */
        public static final String LOCATION = "LOCATION";
        
        /**
         * WiFi签到
         */
        public static final String WIFI = "WIFI";
        
        /**
         * 手动签到
         */
        public static final String MANUAL = "MANUAL";
        
        // 私有构造函数，防止实例化
        private CheckInType() {}

        /**
         * 判断类型是否有效
         * @param type 类型值
         * @return 是否有效
         */
        public static boolean isValidType(String type) {
            return QR_CODE.equals(type) || 
                   LOCATION.equals(type) || 
                   WIFI.equals(type) || 
                   MANUAL.equals(type);
        }
    }
    
    /**
     * 签到状态
     */
    public static final class RecordStatus {
        /**
         * 正常
         */
        public static final String NORMAL = "NORMAL";
        
        /**
         * 迟到
         */
        public static final String LATE = "LATE";
        
        /**
         * 缺席
         */
        public static final String ABSENT = "ABSENT";
        
        /**
         * 请假
         */
        public static final String LEAVE = "LEAVE";
        
        // 私有构造函数，防止实例化
        private RecordStatus() {}

        /**
         * 判断状态是否有效
         * @param status 状态值
         * @return 是否有效
         */
        public static boolean isValidStatus(String status) {
            return NORMAL.equals(status) || 
                   LATE.equals(status) || 
                   ABSENT.equals(status) || 
                   LEAVE.equals(status);
        }
    }
    
    /**
     * 安全相关常量
     */
    public static final class Security {
        /**
         * JWT令牌前缀
         */
        public static final String TOKEN_PREFIX = "Bearer ";
        
        /**
         * 授权头
         */
        public static final String HEADER_STRING = "Authorization";
        
        /**
         * 私有构造函数，防止实例化
         */
        private Security() {}
    }
    
    /**
     * 课程状态常量
     */
    public static final class CourseStatus {
        /**
         * 已创建
         */
        public static final String CREATED = "CREATED";
        
        /**
         * 活跃中
         */
        public static final String ACTIVE = "ACTIVE";
        
        /**
         * 已结束
         */
        public static final String FINISHED = "FINISHED";
        
        /**
         * 已归档
         */
        public static final String ARCHIVED = "ARCHIVED";
        
        /**
         * 已删除（逻辑删除）
         */
        public static final String DELETED = "DELETED";
        
        /**
         * 判断是否是有效状态
         */
        public static boolean isValidStatus(String status) {
            return CREATED.equals(status) || ACTIVE.equals(status) || 
                   FINISHED.equals(status) || ARCHIVED.equals(status) ||
                   DELETED.equals(status);
        }
    }
    
    /**
     * 课程类型常量
     */
    public static final class CourseType {
        /**
         * 普通课程
         */
        public static final String COURSE = "COURSE";
        
        /**
         * 签到任务
         */
        public static final String CHECKIN = "CHECKIN";
        
        /**
         * 判断是否是有效类型
         */
        public static boolean isValidType(String type) {
            return COURSE.equals(type) || CHECKIN.equals(type);
        }
    }
    
    /**
     * 课程用户角色常量
     */
    public static final class CourseUserRole {
        /**
         * 创建者
         */
        public static final String CREATOR = "CREATOR";
        
        /**
         * 助教
         */
        public static final String ASSISTANT = "ASSISTANT";
        
        /**
         * 教师
         */
        public static final String TEACHER = "TEACHER";
        
        /**
         * 学生
         */
        public static final String STUDENT = "STUDENT";
        
        /**
         * 判断是否是有效角色
         */
        public static boolean isValidRole(String role) {
            return CREATOR.equals(role) || ASSISTANT.equals(role) || 
                   TEACHER.equals(role) || STUDENT.equals(role);
        }
    }
    
    /**
     * UUID策略常量
     * 定义不同类型实体ID使用的UUID格式
     */
    public static final class UuidStrategy {
        /**
         * 用户ID格式 - 使用UUID v4 (随机UUID)
         */
        public static final String USER_ID = "uuid2";
        
        /**
         * 课程ID格式 - 使用UUID v4 (随机UUID)
         */
        public static final String COURSE_ID = "uuid2";
        
        /**
         * 签到记录ID格式 - 使用UUID v4 (随机UUID)
         */
        public static final String RECORD_ID = "uuid2";
        
        /**
         * 课程用户关系ID格式 - 使用UUID v4 (随机UUID)
         */
        public static final String COURSE_USER_ID = "uuid2";
        
        /**
         * 时间戳UUID格式 - 使用UUID v1 (基于时间的UUID)
         * 适用于需要按时间排序的实体
         */
        public static final String TIMESTAMP_BASED = "uuid1";
        
        /**
         * 自定义前缀格式 - 使用自定义实现
         * 可生成包含特定前缀的UUID，便于区分不同业务实体
         */
        public static final String PREFIXED = "custom-prefixed";
    }
    
    /**
     * 加入课程的方式
     */
    public static final class JoinMethod {
        /**
         * 创建者（自动加入）
         */
        public static final String CREATED = "CREATED";
        
        /**
         * 通过邀请码加入
         */
        public static final String CODE = "CODE";
        
        /**
         * 通过二维码加入
         */
        public static final String QR_CODE = "QR_CODE";
        
        /**
         * 被管理员添加
         */
        public static final String ADDED = "ADDED";
        
        /**
         * 判断是否是有效加入方式
         */
        public static boolean isValidMethod(String method) {
            return CREATED.equals(method) || CODE.equals(method) || 
                   QR_CODE.equals(method) || ADDED.equals(method);
        }
    }
    
    /**
     * 防止实例化
     */
    private SystemConstants() {}
} 