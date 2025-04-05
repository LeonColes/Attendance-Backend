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
    FOREIGN KEY (creator_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 添加课程表自引用外键
ALTER TABLE courses ADD CONSTRAINT fk_courses_parent 
FOREIGN KEY (parent_course_id) REFERENCES courses(id);

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
CREATE TABLE IF NOT EXISTS records (
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (parent_course_id) REFERENCES courses(id),
    UNIQUE KEY unique_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据 --

-- 创建默认管理员账户
INSERT INTO users (id, username, password, full_name, email, role, enabled, created_at, updated_at)
VALUES ('admin-uuid', 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', 'System Administrator', 'admin@example.com', 'ADMIN', TRUE, NOW(), NOW());

-- 教师用户 (3名)
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('teacher-uuid-1', 'teacher1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '李老师', 'teacher1@example.com', '13800000001', 'TEACHER', TRUE, NOW(), NOW()),
('teacher-uuid-2', 'teacher2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '王老师', 'teacher2@example.com', '13800000002', 'TEACHER', TRUE, NOW(), NOW()),
('teacher-uuid-3', 'teacher3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '张老师', 'teacher3@example.com', '13800000003', 'TEACHER', TRUE, NOW(), NOW());

-- 学生用户 (10名)
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('student-uuid-1', 'student1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '张同学', 'student1@example.com', '13900000001', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-2', 'student2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '王同学', 'student2@example.com', '13900000002', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-3', 'student3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '刘同学', 'student3@example.com', '13900000003', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-4', 'student4', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '赵同学', 'student4@example.com', '13900000004', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-5', 'student5', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '陈同学', 'student5@example.com', '13900000005', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-6', 'student6', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '杨同学', 'student6@example.com', '13900000006', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-7', 'student7', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '黄同学', 'student7@example.com', '13900000007', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-8', 'student8', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '周同学', 'student8@example.com', '13900000008', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-9', 'student9', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '吴同学', 'student9@example.com', '13900000009', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-10', 'student10', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '郑同学', 'student10@example.com', '13900000010', 'STUDENT', TRUE, NOW(), NOW());

-- 课程 (李老师的课程)
INSERT INTO courses (id, name, description, creator_id, code, type, status, start_date, end_date, created_at, updated_at)
VALUES 
('course-uuid-1', 'Java程序设计', '本课程介绍Java编程基础与应用开发', 'teacher-uuid-1', 'JAVA123', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW()),
('course-uuid-2', '数据结构与算法', '本课程介绍常见数据结构与算法设计', 'teacher-uuid-1', 'DSA456', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW()),
('course-uuid-3', '软件工程', '软件开发生命周期与项目管理', 'teacher-uuid-1', 'SE789', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW());

-- 课程 (王老师的课程)
INSERT INTO courses (id, name, description, creator_id, code, type, status, start_date, end_date, created_at, updated_at)
VALUES 
('course-uuid-4', '计算机网络', '计算机网络原理与协议分析', 'teacher-uuid-2', 'NET123', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW()),
('course-uuid-5', '操作系统', '操作系统原理与实现', 'teacher-uuid-2', 'OS456', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW());

-- 课程 (张老师的课程)
INSERT INTO courses (id, name, description, creator_id, code, type, status, start_date, end_date, created_at, updated_at)
VALUES 
('course-uuid-6', '数据库系统', '数据库设计与实现', 'teacher-uuid-3', 'DB123', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW()),
('course-uuid-7', '人工智能导论', '人工智能基础理论与应用', 'teacher-uuid-3', 'AI456', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW());

-- 签到任务 (Java程序设计)
INSERT INTO courses (id, name, description, creator_id, code, type, status, parent_course_id, checkin_start_time, checkin_end_time, checkin_type, verify_params, created_at, updated_at)
VALUES 
('checkin-uuid-1', '第1周Java课程签到', '第1周Java课程签到任务', 'teacher-uuid-1', 'J1001', 'CHECKIN', 'ENDED', 'course-uuid-1', '2023-09-05 09:00:00', '2023-09-05 09:15:00', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('checkin-uuid-2', '第2周Java课程签到', '第2周Java课程签到任务', 'teacher-uuid-1', 'J1002', 'CHECKIN', 'ENDED', 'course-uuid-1', '2023-09-12 09:00:00', '2023-09-12 09:15:00', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW()),
('checkin-uuid-3', '第3周Java课程签到', '第3周Java课程签到任务', 'teacher-uuid-1', 'J1003', 'CHECKIN', 'ACTIVE', 'course-uuid-1', NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'QR_CODE', '{"code":"qrcode-token-3"}', NOW(), NOW());

-- 签到任务 (数据结构与算法)
INSERT INTO courses (id, name, description, creator_id, code, type, status, parent_course_id, checkin_start_time, checkin_end_time, checkin_type, verify_params, created_at, updated_at)
VALUES 
('checkin-uuid-4', '第1周数据结构签到', '第1周数据结构课程签到任务', 'teacher-uuid-1', 'D1001', 'CHECKIN', 'ENDED', 'course-uuid-2', '2023-09-06 14:00:00', '2023-09-06 14:15:00', 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW()),
('checkin-uuid-5', '第2周数据结构签到', '第2周数据结构课程签到任务', 'teacher-uuid-1', 'D1002', 'CHECKIN', 'ACTIVE', 'course-uuid-2', NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'QR_CODE', '{"code":"qrcode-token-5"}', NOW(), NOW());

-- 签到任务 (计算机网络)
INSERT INTO courses (id, name, description, creator_id, code, type, status, parent_course_id, checkin_start_time, checkin_end_time, checkin_type, verify_params, created_at, updated_at)
VALUES 
('checkin-uuid-6', '第1周网络课程签到', '第1周网络课程签到任务', 'teacher-uuid-2', 'N1001', 'CHECKIN', 'ENDED', 'course-uuid-4', '2023-09-07 10:00:00', '2023-09-07 10:15:00', 'LOCATION', '{"latitude":39.908823,"longitude":116.397470,"radius":100}', NOW(), NOW()),
('checkin-uuid-7', '第2周网络课程签到', '第2周网络课程签到任务', 'teacher-uuid-2', 'N1002', 'CHECKIN', 'ACTIVE', 'course-uuid-4', NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'LOCATION', '{"latitude":39.908823,"longitude":116.397470,"radius":100}', NOW(), NOW());

-- 课程-用户关联 (Java程序设计)
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
('cu-uuid-1', 'course-uuid-1', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-2', 'course-uuid-1', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-3', 'course-uuid-1', 'student-uuid-2', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-4', 'course-uuid-1', 'student-uuid-3', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-5', 'course-uuid-1', 'student-uuid-4', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-6', 'course-uuid-1', 'student-uuid-5', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW());

-- 课程-用户关联 (数据结构与算法)
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
('cu-uuid-7', 'course-uuid-2', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-8', 'course-uuid-2', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-9', 'course-uuid-2', 'student-uuid-2', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-10', 'course-uuid-2', 'student-uuid-6', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-11', 'course-uuid-2', 'student-uuid-7', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW());

-- 课程-用户关联 (软件工程)
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
('cu-uuid-12', 'course-uuid-3', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-13', 'course-uuid-3', 'student-uuid-3', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-14', 'course-uuid-3', 'student-uuid-4', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-15', 'course-uuid-3', 'student-uuid-8', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-16', 'course-uuid-3', 'student-uuid-9', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW());

-- 课程-用户关联 (计算机网络)
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
('cu-uuid-17', 'course-uuid-4', 'teacher-uuid-2', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-18', 'course-uuid-4', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-19', 'course-uuid-4', 'student-uuid-5', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-20', 'course-uuid-4', 'student-uuid-6', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-21', 'course-uuid-4', 'student-uuid-10', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW());

-- 签到记录 (Java第1周)
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
('record-uuid-1', 'student-uuid-1', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:01:30', '{"latitude":39.908823,"longitude":116.397470}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-2', 'student-uuid-2', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:02:45', '{"latitude":39.908825,"longitude":116.397475}', '{"type":"iOS","model":"iPhone 13"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-3', 'student-uuid-3', 'checkin-uuid-1', 'course-uuid-1', 'LATE', '2023-09-05 09:12:05', '{"latitude":39.908830,"longitude":116.397480}', '{"type":"Android","model":"Galaxy S22"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-4', 'student-uuid-4', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:03:15', '{"latitude":39.908832,"longitude":116.397478}', '{"type":"iOS","model":"iPhone 12"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-5', 'student-uuid-5', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:05:22', '{"latitude":39.908828,"longitude":116.397472}', '{"type":"Android","model":"Xiaomi 12"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW());

-- 签到记录 (Java第2周)
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
('record-uuid-6', 'student-uuid-1', 'checkin-uuid-2', 'course-uuid-1', 'NORMAL', '2023-09-12 09:02:10', '{"latitude":39.908823,"longitude":116.397470}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW()),
('record-uuid-7', 'student-uuid-2', 'checkin-uuid-2', 'course-uuid-1', 'NORMAL', '2023-09-12 09:03:25', '{"latitude":39.908825,"longitude":116.397475}', '{"type":"iOS","model":"iPhone 13"}', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW()),
('record-uuid-8', 'student-uuid-3', 'checkin-uuid-2', 'course-uuid-1', 'ABSENT', NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
('record-uuid-9', 'student-uuid-4', 'checkin-uuid-2', 'course-uuid-1', 'LATE', '2023-09-12 09:13:05', '{"latitude":39.908832,"longitude":116.397478}', '{"type":"iOS","model":"iPhone 12"}', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW()),
('record-uuid-10', 'student-uuid-5', 'checkin-uuid-2', 'course-uuid-1', 'NORMAL', '2023-09-12 09:04:32', '{"latitude":39.908828,"longitude":116.397472}', '{"type":"Android","model":"Xiaomi 12"}', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW());

-- 签到记录 (数据结构第1周)
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
('record-uuid-11', 'student-uuid-1', 'checkin-uuid-4', 'course-uuid-2', 'NORMAL', '2023-09-06 14:03:12', '{"latitude":39.908835,"longitude":116.397485}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW()),
('record-uuid-12', 'student-uuid-2', 'checkin-uuid-4', 'course-uuid-2', 'NORMAL', '2023-09-06 14:02:48', '{"latitude":39.908837,"longitude":116.397488}', '{"type":"iOS","model":"iPhone 13"}', 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW()),
('record-uuid-13', 'student-uuid-6', 'checkin-uuid-4', 'course-uuid-2', 'LATE', '2023-09-06 14:12:30', '{"latitude":39.908840,"longitude":116.397490}', '{"type":"Android","model":"OnePlus 9"}', 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW()),
('record-uuid-14', 'student-uuid-7', 'checkin-uuid-4', 'course-uuid-2', 'NORMAL', '2023-09-06 14:05:15', '{"latitude":39.908842,"longitude":116.397492}', '{"type":"iOS","model":"iPhone 14"}', 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW());

-- 签到记录 (计算机网络第1周)
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
('record-uuid-15', 'student-uuid-1', 'checkin-uuid-6', 'course-uuid-4', 'NORMAL', '2023-09-07 10:02:18', '{"latitude":39.908823,"longitude":116.397470}', '{"type":"Android","model":"Pixel 6"}', 'LOCATION', '{"latitude":39.908823,"longitude":116.397470,"distance":15}', NOW(), NOW()),
('record-uuid-16', 'student-uuid-5', 'checkin-uuid-6', 'course-uuid-4', 'NORMAL', '2023-09-07 10:03:45', '{"latitude":39.908825,"longitude":116.397475}', '{"type":"Android","model":"Xiaomi 12"}', 'LOCATION', '{"latitude":39.908825,"longitude":116.397475,"distance":12}', NOW(), NOW()),
('record-uuid-17', 'student-uuid-6', 'checkin-uuid-6', 'course-uuid-4', 'LATE', '2023-09-07 10:12:30', '{"latitude":39.908830,"longitude":116.397480}', '{"type":"Android","model":"OnePlus 9"}', 'LOCATION', '{"latitude":39.908830,"longitude":116.397480,"distance":18}', NOW(), NOW()),
('record-uuid-18', 'student-uuid-10', 'checkin-uuid-6', 'course-uuid-4', 'NORMAL', '2023-09-07 10:05:22', '{"latitude":39.908835,"longitude":116.397485}', '{"type":"iOS","model":"iPhone SE"}', 'LOCATION', '{"latitude":39.908835,"longitude":116.397485,"distance":22}', NOW(), NOW()); 