-- 添加示例教师用户
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('teacher-uuid-1', 'teacher1', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '教师一', 'teacher1@example.com', '13800000001', 'TEACHER', TRUE, NOW(), NOW());

-- 添加示例学生用户
INSERT INTO users (id, username, password, full_name, email, phone, role, enabled, created_at, updated_at)
VALUES 
('student-uuid-1', 'student1', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '学生一', 'student1@example.com', '13900000001', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-2', 'student2', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '学生二', 'student2@example.com', '13900000002', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-3', 'student3', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '学生三', 'student3@example.com', '13900000003', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-4', 'student4', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '学生四', 'student4@example.com', '13900000004', 'STUDENT', TRUE, NOW(), NOW()),
('student-uuid-5', 'student5', '$argon2id$v=19$m=65536,t=3,p=4$QUJDREVGMTIzNDU2$2hYE9vMqZfGCunYODIGPx2ias9D4fYWvzsN0YSG9a78', '学生五', 'student5@example.com', '13900000005', 'STUDENT', TRUE, NOW(), NOW());

-- 添加示例课程
INSERT INTO courses (id, name, description, creator_id, code, type, status, start_date, end_date, created_at, updated_at)
VALUES 
('course-uuid-1', '数据结构与算法', '计算机科学的基础课程，讲解常用的数据结构和算法设计', 'teacher-uuid-1', 'ALGO12345', 'COURSE', 'ACTIVE', '2025-03-01', '2025-07-01', NOW(), NOW()),
('course-uuid-2', '软件工程实践', '软件开发生命周期、项目管理、测试和部署', 'teacher-uuid-1', 'SENG67890', 'COURSE', 'ACTIVE', '2025-03-01', '2025-07-01', NOW(), NOW());

-- 添加示例签到任务
INSERT INTO courses (id, name, description, creator_id, code, type, status, checkin_start_time, checkin_end_time, checkin_type, verify_params, parent_course_id, created_at, updated_at)
VALUES 
('checkin-uuid-1', '第1周签到', '数据结构第1周课堂签到', 'teacher-uuid-1', 'CK0000001', 'CHECKIN', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'QR_CODE', '{"code":"{{token}}"}', 'course-uuid-1', NOW(), NOW()),
('checkin-uuid-2', '第2周签到', '数据结构第2周课堂签到', 'teacher-uuid-1', 'CK0000002', 'CHECKIN', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'LOCATION', '{"latitude":39.908823,"longitude":116.397470,"radius":200}', 'course-uuid-1', NOW(), NOW()),
('checkin-uuid-3', '第1周签到', '软件工程第1周课堂签到', 'teacher-uuid-1', 'CK0000003', 'CHECKIN', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'QR_CODE', '{"code":"{{token}}"}', 'course-uuid-2', NOW(), NOW());

-- 添加课程-用户关系（学生选课）
INSERT INTO course_users (id, course_id, user_id, role, joined_at, join_method, active, created_at, updated_at)
VALUES 
('cu-uuid-1', 'course-uuid-1', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-2', 'course-uuid-1', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-3', 'course-uuid-1', 'student-uuid-2', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-4', 'course-uuid-1', 'student-uuid-3', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-5', 'course-uuid-2', 'teacher-uuid-1', 'CREATOR', NOW(), 'CREATED', TRUE, NOW(), NOW()),
('cu-uuid-6', 'course-uuid-2', 'student-uuid-1', 'STUDENT', NOW(), 'CODE', TRUE, NOW(), NOW()),
('cu-uuid-7', 'course-uuid-2', 'student-uuid-4', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW()),
('cu-uuid-8', 'course-uuid-2', 'student-uuid-5', 'STUDENT', NOW(), 'QR_CODE', TRUE, NOW(), NOW());

-- 添加签到记录
INSERT INTO records (id, user_id, course_id, parent_course_id, status, check_in_time, location, device, verify_method, verify_data, created_at, updated_at)
VALUES 
('record-uuid-1', 'student-uuid-1', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', NOW(), '{"latitude":39.908823,"longitude":116.397470}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"{{token}}"}', NOW(), NOW()),
('record-uuid-2', 'student-uuid-2', 'checkin-uuid-1', 'course-uuid-1', 'NORMAL', NOW(), '{"latitude":39.908825,"longitude":116.397475}', '{"type":"iOS","model":"iPhone 13"}', 'QR_CODE', '{"code":"{{token}}"}', NOW(), NOW()),
('record-uuid-3', 'student-uuid-1', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', NOW(), '{"latitude":39.908830,"longitude":116.397480}', '{"type":"Android","model":"Pixel 6"}', 'QR_CODE', '{"code":"{{token}}"}', NOW(), NOW()),
('record-uuid-4', 'student-uuid-4', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', NOW(), '{"latitude":39.908835,"longitude":116.397485}', '{"type":"iOS","model":"iPhone 14"}', 'QR_CODE', '{"code":"{{token}}"}', NOW(), NOW()),
('record-uuid-5', 'student-uuid-5', 'checkin-uuid-3', 'course-uuid-2', 'NORMAL', NOW(), '{"latitude":39.908840,"longitude":116.397490}', '{"type":"Android","model":"Galaxy S22"}', 'QR_CODE', '{"code":"{{token}}"}', NOW(), NOW()); 