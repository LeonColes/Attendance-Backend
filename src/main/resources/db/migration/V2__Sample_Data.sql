-- 添加示例教师用户
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('teacher-uuid-1', 'teacher1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '李老师', 'teacher1@example.com', '13800000001', 'TEACHER', TRUE, NOW(), NOW());

-- 添加示例学生用户
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('student-uuid-1', 'student1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '张同学', 'student1@example.com', '13900000001', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-2', 'student2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '王同学', 'student2@example.com', '13900000002', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-3', 'student3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '刘同学', 'student3@example.com', '13900000003', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-4', 'student4', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '赵同学', 'student4@example.com', '13900000004', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-5', 'student5', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZ.kY4jKY0hxAEEIFU6aG16bvt1m', '陈同学', 'student5@example.com', '13900000005', 'STUDENT', TRUE, NOW(), NOW());

-- 添加示例课程
INSERT INTO courses (id, name, description, creator_id, code, type, status, start_date, end_date, created_at, updated_at)
VALUES 
('course-uuid-1', 'Java程序设计', '本课程介绍Java编程基础与应用开发', 'teacher-uuid-1', 'ABC123', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW()),
('course-uuid-2', '数据结构与算法', '本课程介绍常见数据结构与算法设计', 'teacher-uuid-1', 'DEF456', 'COURSE', 'ACTIVE', '2023-09-01', '2024-01-15', NOW(), NOW());

-- 添加示例签到任务
INSERT INTO courses (id, name, description, creator_id, code, type, status, parent_course_id, checkin_start_time, checkin_end_time, checkin_type, verify_params, created_at, updated_at)
VALUES 
('checkin-uuid-1', '第1周Java课程签到', '第1周Java课程签到任务', 'teacher-uuid-1', 'QR1001', 'CHECKIN', 'ENDED', 'course-uuid-1', '2023-09-05 09:00:00', '2023-09-05 09:15:00', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('checkin-uuid-2', '第2周Java课程签到', '第2周Java课程签到任务', 'teacher-uuid-1', 'QR1002', 'CHECKIN', 'ENDED', 'course-uuid-1', '2023-09-12 09:00:00', '2023-09-12 09:15:00', 'QR_CODE', '{"code":"qrcode-token-2"}', NOW(), NOW()),
('checkin-uuid-3', '第1周数据结构签到', '第1周数据结构课程签到任务', 'teacher-uuid-1', 'QR2001', 'CHECKIN', 'ENDED', 'course-uuid-2', '2023-09-06 14:00:00', '2023-09-06 14:15:00', 'QR_CODE', '{"code":"qrcode-token-3"}', NOW(), NOW()),
('checkin-uuid-4', '第2周数据结构签到', '第2周数据结构课程签到任务', 'teacher-uuid-1', 'QR2002', 'CHECKIN', 'ACTIVE', 'course-uuid-2', NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'QR_CODE', '{"code":"qrcode-token-4"}', NOW(), NOW());

-- 添加课程-用户关联
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
-- Java程序设计课程
('cu-uuid-1', 'course-uuid-1', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-2', 'course-uuid-1', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-3', 'course-uuid-1', 'student-uuid-2', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-4', 'course-uuid-1', 'student-uuid-3', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
-- 数据结构课程
('cu-uuid-5', 'course-uuid-2', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-6', 'course-uuid-2', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-7', 'course-uuid-2', 'student-uuid-4', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-8', 'course-uuid-2', 'student-uuid-5', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW());

-- 添加签到记录
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
-- 第1周Java课程签到记录
('record-uuid-1', 'student-uuid-1', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:01:30', '{"latitude":39.908823,"longitude":116.397470}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-2', 'student-uuid-2', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', '2023-09-05 09:02:45', '{"latitude":39.908825,"longitude":116.397475}', '{"type":"iOS","model":"iPhone 13"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
('record-uuid-3', 'student-uuid-3', 'checkin-uuid-1', 'course-uuid-1', 'LATE', '2023-09-05 09:12:05', '{"latitude":39.908830,"longitude":116.397480}', '{"type":"Android","model":"Galaxy S22"}', 'QR_CODE', '{"code":"qrcode-token-1"}', NOW(), NOW()),
-- 第1周数据结构课程签到记录
('record-uuid-4', 'student-uuid-1', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', '2023-09-06 14:03:12', '{"latitude":39.908835,"longitude":116.397485}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"qrcode-token-3"}', NOW(), NOW()),
('record-uuid-5', 'student-uuid-4', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', '2023-09-06 14:05:40', '{"latitude":39.908840,"longitude":116.397490}', '{"type":"iOS","model":"iPhone 14"}', 'QR_CODE', '{"code":"qrcode-token-3"}', NOW(), NOW()),
('record-uuid-6', 'student-uuid-5', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', '2023-09-06 14:04:22', '{"latitude":39.908845,"longitude":116.397495}', '{"type":"Android","model":"Galaxy S22"}', 'QR_CODE', '{"code":"qrcode-token-3"}', NOW(), NOW());