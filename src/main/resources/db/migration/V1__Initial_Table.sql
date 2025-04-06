-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY COMMENT '用户ID，使用UUID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    full_name VARCHAR(100) COMMENT '用户全名',
    email VARCHAR(100) COMMENT '电子邮箱',
    phone VARCHAR(20) COMMENT '电话号码',
    role VARCHAR(20) NOT NULL COMMENT '用户角色（ADMIN/TEACHER/STUDENT）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '账户是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建课程表
CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) PRIMARY KEY COMMENT '课程/签到任务ID，使用UUID',
    name VARCHAR(100) NOT NULL COMMENT '课程/签到任务名称',
    description VARCHAR(500) COMMENT '课程/签到任务描述',
    creator_id VARCHAR(36) NOT NULL COMMENT '创建者ID',
    code VARCHAR(10) NOT NULL UNIQUE COMMENT '课程邀请码',
    type VARCHAR(20) NOT NULL COMMENT '类型（COURSE/CHECKIN）',
    status VARCHAR(20) NOT NULL COMMENT '状态（CREATED/ACTIVE/ENDED/ARCHIVED）',
    start_date DATE COMMENT '课程开始日期',
    end_date DATE COMMENT '课程结束日期',
    checkin_start_time TIMESTAMP NULL COMMENT '签到开始时间',
    checkin_end_time TIMESTAMP NULL COMMENT '签到结束时间',
    checkin_type VARCHAR(20) NULL COMMENT '签到类型（QR_CODE/LOCATION/WIFI/MANUAL）',
    verify_params TEXT NULL COMMENT '验证参数（JSON格式）',
    parent_course_id VARCHAR(36) NULL COMMENT '父课程ID（仅当type=CHECKIN时有值）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (creator_id) REFERENCES users(id),
    FOREIGN KEY (parent_course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建课程-用户关联表
CREATE TABLE IF NOT EXISTS course_users (
    id VARCHAR(36) PRIMARY KEY COMMENT '关联ID，使用UUID',
    course_id VARCHAR(36) NOT NULL COMMENT '课程ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '用户在课程中的角色（CREATOR/TEACHER/STUDENT）',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    join_method VARCHAR(20) COMMENT '加入方式（CREATED/INVITED/CODE/QR_CODE）',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否活跃',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_course_user (course_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建签到记录表
CREATE TABLE IF NOT EXISTS course_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID，使用UUID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    course_id VARCHAR(36) NOT NULL COMMENT '签到任务ID',
    parent_course_id VARCHAR(36) COMMENT '父课程ID',
    status VARCHAR(20) NOT NULL COMMENT '签到状态（NORMAL/LATE/ABSENT）',
    check_in_time TIMESTAMP COMMENT '签到时间',
    location VARCHAR(255) COMMENT '位置信息（JSON格式）',
    device VARCHAR(255) COMMENT '设备信息（JSON格式）',
    verify_method VARCHAR(20) COMMENT '验证方式（QR_CODE/LOCATION/WIFI/MANUAL）',
    verify_data TEXT COMMENT '验证数据（JSON格式）',
    remark VARCHAR(500) COMMENT '备注信息',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有效',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (parent_course_id) REFERENCES courses(id),
    UNIQUE KEY unique_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 基础测试数据
INSERT INTO users (id, username, password, full_name, email, role, enabled, created_at, updated_at) 
VALUES ('admin-uuid', 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', 'System Administrator', 'admin@example.com', 'ADMIN', TRUE, NOW(), NOW());