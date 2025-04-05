# 智能考勤系统 API 设计文档

## 目录

1. [基本信息](#基本信息)
2. [认证 APIs](#认证-apis)
3. [用户 APIs](#用户-apis)
4. [课程与签到管理 APIs](#课程与签到管理-apis)
5. [签到记录 APIs](#签到记录-apis)
6. [统计 APIs](#统计-apis)
7. [业务流程图](#业务流程图)

## 基本信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token JWT
- **数据格式**: JSON
- **请求头要求**:
  - Content-Type: application/json
  - Authorization: Bearer {token} (认证接口除外)

## 认证 APIs

| 方法   | URL                | 描述              | 认证要求           |
|------|------------------|-----------------|-----------------|
| POST | /auth/register   | 用户注册            | 无               |
| POST | /auth/login      | 用户登录            | 无               |
| POST | /auth/refresh    | 刷新访问令牌          | 需要刷新令牌          |
| POST | /auth/logout     | 登出并使当前令牌失效      | 需要认证            |

### 用户注册

**请求**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "Test@123",
    "fullName": "学生一",
    "email": "student1@example.com",
    "role": "STUDENT"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "用户注册成功",
  "data": {
    "id": 1,
    "username": "student1",
    "fullName": "学生一",
    "email": "student1@example.com",
    "role": "STUDENT",
    "enabled": true,
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  }
}
```

### 用户登录

**请求**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "Test@123"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "student1",
    "fullName": "学生一",
    "role": "STUDENT"
  }
}
```

## 用户 APIs

| 方法   | URL                | 描述            | 认证要求           |
|------|------------------|---------------|-----------------|
| GET  | /users/me        | 获取当前用户信息      | 需要认证            |
| GET  | /users/{id}      | 获取指定用户信息      | 需要认证, ADMIN或本人  |
| GET  | /users           | 获取用户列表        | 需要认证, ADMIN/TEACHER |
| PUT  | /users/{id}      | 更新用户信息        | 需要认证, ADMIN或本人  |
| PATCH| /users/{id}/password | 修改用户密码    | 需要认证, ADMIN或本人  |

### 获取当前用户信息

**请求**:
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJ..."
```

**成功响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "student1",
    "fullName": "学生一",
    "email": "student1@example.com",
    "role": "STUDENT",
    "enabled": true,
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  }
}
```

## 课程与签到管理 APIs

| 方法   | URL                         | 描述              | 认证要求           |
|------|----------------------------|-----------------|-----------------|
| POST | /courses                   | 创建课程或签到任务      | 需要认证，TEACHER/ADMIN |
| GET  | /courses                   | 获取课程列表         | 需要认证            |
| GET  | /courses/{id}              | 获取课程或签到任务详情   | 需要认证，课程成员       |
| PUT  | /courses/{id}              | 更新课程或签到任务信息   | 需要认证，课程创建者/ADMIN |
| POST | /courses/{id}/members      | 添加课程成员         | 需要认证，课程创建者/ADMIN |
| GET  | /courses/{id}/members      | 获取课程成员列表       | 需要认证，课程成员       |
| DELETE | /courses/{id}/members/{userId} | 移除课程成员   | 需要认证，课程创建者/ADMIN |
| POST | /courses/join/{code}       | 通过邀请码加入课程      | 需要认证            |
| GET  | /courses/my                | 获取我的课程列表       | 需要认证            |
| GET  | /courses/{courseId}/checkins | 获取课程下的签到任务列表 | 需要认证，课程成员       |
| POST | /courses/checkin/{checkinId} | 提交签到          | 需要认证，课程成员       |
| GET  | /courses/checkin/{checkinId}/code | 获取签到二维码  | 需要认证，任务创建者/ADMIN |
| POST | /courses/checkin/{checkinId}/end | 手动结束签到任务  | 需要认证，任务创建者/ADMIN |

### 创建课程

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "name": "Java程序设计",
    "description": "本课程介绍Java编程基础与应用开发",
    "type": "COURSE",
    "startDate": "2025-09-01",
    "endDate": "2026-01-15"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "课程创建成功",
  "data": {
    "id": "abc123",
    "name": "Java程序设计",
    "description": "本课程介绍Java编程基础与应用开发",
    "creatorId": 2,
    "creatorName": "张老师",
    "code": "XYZ789",
    "type": "COURSE",
    "startDate": "2025-09-01",
    "endDate": "2026-01-15",
    "status": "ACTIVE",
    "memberCount": 1,
    "createdAt": "2025-04-05T14:30:00",
    "updatedAt": "2025-04-05T14:30:00"
  }
}
```

### 创建签到任务

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "name": "周三课程签到",
    "description": "第5周周三课程签到",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "checkinStartTime": "2025-04-10T15:00:00",
    "checkinEndTime": "2025-04-10T15:15:00",
    "checkinType": "QR_CODE"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到任务创建成功",
  "data": {
    "id": "def456",
    "name": "周三课程签到",
    "description": "第5周周三课程签到",
    "creatorId": 2,
    "creatorUsername": "teacher1",
    "creatorFullName": "张老师",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "parentCourseName": "Java程序设计",
    "checkinStartTime": "2025-04-10T15:00:00",
    "checkinEndTime": "2025-04-10T15:15:00",
    "checkinType": "QR_CODE",
    "verifyParams": "{\"code\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\"}",
    "status": "CREATED",
    "createdAt": "2025-04-05T14:30:00",
    "updatedAt": "2025-04-05T14:30:00"
  }
}
```

### 获取课程的签到任务列表

**请求**:
```bash
curl -X GET http://localhost:8080/api/courses/abc123/checkins \
  -H "Authorization: Bearer eyJhbGciOiJ..."
```

**成功响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": "def456",
      "name": "周三课程签到",
      "description": "第5周周三课程签到",
      "creatorId": 2,
      "creatorUsername": "teacher1",
      "type": "CHECKIN",
      "parentCourseId": "abc123",
      "parentCourseName": "Java程序设计",
      "checkinStartTime": "2025-04-10T15:00:00",
      "checkinEndTime": "2025-04-10T15:15:00",
      "checkinType": "QR_CODE",
      "status": "ACTIVE",
      "createdAt": "2025-04-05T14:30:00"
    },
    // 更多签到任务...
  ]
}
```

### 获取签到二维码

**请求**:
```bash
curl -X GET http://localhost:8080/api/courses/checkin/def456/code \
  -H "Authorization: Bearer eyJhbGciOiJ..."
```

**成功响应**:
```json
{
  "code": 200,
  "message": "生成签到码成功",
  "data": {
    "checkinId": "def456",
    "code": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "timestamp": 1712356200000
  }
}
```

### 提交签到

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses/checkin/def456 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "verifyData": "{\"code\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\"}",
    "location": "113.9432,22.5194",
    "device": "iPhone 15 Pro"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "checkinId": "def456",
    "success": true,
    "timestamp": 1712356290000
  }
}
```

### 位置签到创建

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "name": "位置签到示例",
    "description": "基于位置的签到任务",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "checkinStartTime": "2025-04-12T10:00:00",
    "checkinEndTime": "2025-04-12T10:30:00",
    "checkinType": "LOCATION",
    "verifyParams": "{\"latitude\":22.5194,\"longitude\":113.9432,\"radius\":100}"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到任务创建成功",
  "data": {
    "id": "ghi789",
    "name": "位置签到示例",
    "description": "基于位置的签到任务",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "parentCourseName": "Java程序设计",
    "checkinStartTime": "2025-04-12T10:00:00",
    "checkinEndTime": "2025-04-12T10:30:00",
    "checkinType": "LOCATION",
    "verifyParams": "{\"latitude\":22.5194,\"longitude\":113.9432,\"radius\":100}",
    "status": "CREATED",
    "createdAt": "2025-04-05T15:20:00",
    "updatedAt": "2025-04-05T15:20:00"
  }
}
```

### 位置签到提交

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses/checkin/ghi789 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "verifyData": "{\"latitude\":22.5190,\"longitude\":113.9428}",
    "location": "113.9428,22.5190",
    "device": "iPhone 15 Pro"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "checkinId": "ghi789",
    "success": true,
    "timestamp": 1712403610000
  }
}
```

### WiFi签到创建

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "name": "WiFi签到示例",
    "description": "基于WiFi连接的签到任务",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "checkinStartTime": "2025-04-15T14:00:00",
    "checkinEndTime": "2025-04-15T14:30:00",
    "checkinType": "WIFI",
    "verifyParams": "{\"ssid\":\"University_WiFi\",\"bssid\":\"00:11:22:33:44:55\"}"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到任务创建成功",
  "data": {
    "id": "jkl012",
    "name": "WiFi签到示例",
    "description": "基于WiFi连接的签到任务",
    "type": "CHECKIN",
    "parentCourseId": "abc123",
    "parentCourseName": "Java程序设计",
    "checkinStartTime": "2025-04-15T14:00:00",
    "checkinEndTime": "2025-04-15T14:30:00",
    "checkinType": "WIFI",
    "verifyParams": "{\"ssid\":\"University_WiFi\",\"bssid\":\"00:11:22:33:44:55\"}",
    "status": "CREATED",
    "createdAt": "2025-04-05T16:10:00",
    "updatedAt": "2025-04-05T16:10:00"
  }
}
```

### WiFi签到提交

**请求**:
```bash
curl -X POST http://localhost:8080/api/courses/checkin/jkl012 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "verifyData": "{\"ssid\":\"University_WiFi\",\"bssid\":\"00:11:22:33:44:55\"}",
    "location": "113.9440,22.5188",
    "device": "iPhone 15 Pro"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "checkinId": "jkl012",
    "success": true,
    "timestamp": 1713262810000
  }
}
```

## 签到记录 APIs

| 方法   | URL                         | 描述              | 认证要求           |
|------|----------------------------|-----------------|-----------------|
| GET  | /records/{id}              | 获取签到记录详情        | 需要认证，ADMIN或记录关联用户 |
| GET  | /records/task/{taskId}     | 获取任务所有签到记录      | 需要认证，ADMIN或任务创建者 |
| GET  | /records/user/{userId}     | 获取用户所有签到记录      | 需要认证，ADMIN/TEACHER或本人 |
| GET  | /records/course/{courseId} | 获取课程所有签到记录      | 需要认证，课程创建者/ADMIN |
| POST | /records/check-in/qrcode   | 提交二维码签到         | 需要认证，课程成员 |

### 提交二维码签到

**请求**:
```bash
curl -X POST http://localhost:8080/api/records/check-in/qrcode \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJ..." \
  -d '{
    "taskId": "12a45b67-89c0-12d3-e456-78fg901hij2k",
    "code": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "location": "113.9432,22.5194",
    "device": "iPhone 15 Pro"
  }'
```

**成功响应**:
```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "id": "23b56c78-90d1-23e4-f567-89gh012jkl3m",
    "userId": 3,
    "username": "student1",
    "userFullName": "李学生",
    "taskId": "12a45b67-89c0-12d3-e456-78fg901hij2k",
    "taskTitle": "程序设计课程签到",
    "courseId": "abc123",
    "courseName": "Java程序设计",
    "status": "NORMAL",
    "checkInTime": "2025-04-10T15:01:30",
    "location": "113.9432,22.5194",
    "device": "iPhone 15 Pro",
    "verifyMethod": "QR_CODE",
    "createdAt": "2025-04-10T15:01:30",
    "updatedAt": "2025-04-10T15:01:30"
  }
}
```

### 获取课程所有签到记录

**请求**:
```bash
curl -X GET http://localhost:8080/api/records/course/abc123 \
  -H "Authorization: Bearer eyJhbGciOiJ..."
```

**成功响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": "23b56c78-90d1-23e4-f567-89gh012jkl3m",
      "userId": 3,
      "username": "student1",
      "userFullName": "李学生",
      "taskId": "12a45b67-89c0-12d3-e456-78fg901hij2k",
      "taskTitle": "程序设计课程签到",
      "status": "NORMAL",
      "checkInTime": "2025-04-10T15:01:30"
    },
    // 更多记录...
  ]
}
```

## 统计 APIs

| 方法   | URL                                  | 描述              | 认证要求           |
|------|--------------------------------------|-----------------|-----------------|
| GET  | /stats/task/{taskId}                 | 获取任务签到统计        | 需要认证，ADMIN或任务创建者 |
| GET  | /stats/course/{courseId}             | 获取课程签到统计        | 需要认证，课程创建者/ADMIN |
| GET  | /stats/user/{userId}/course/{courseId} | 获取用户在课程中的签到统计  | 需要认证，ADMIN/TEACHER或本人 |

### 获取课程签到统计

**请求**:
```bash
curl -X GET http://localhost:8080/api/stats/course/abc123 \
  -H "Authorization: Bearer eyJhbGciOiJ..."
```

**成功响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "courseId": "abc123",
    "courseName": "Java程序设计",
    "totalTasks": 15,
    "totalStudents": 45,
    "averageAttendance": 92.5,
    "taskStats": [
      {
        "taskId": "12a45b67-89c0-12d3-e456-78fg901hij2k",
        "taskTitle": "第5周周三课程签到",
        "totalRecords": 42,
        "normalCount": 38,
        "lateCount": 4,
        "absentCount": 3,
        "attendanceRate": 93.33
      },
      // 更多任务统计...
    ],
    "studentStats": [
      {
        "userId": 3,
        "username": "student1",
        "fullName": "李学生",
        "totalTasks": 15,
        "attendedCount": 14,
        "lateCount": 1,
        "absentCount": 0,
        "attendanceRate": 93.33
      },
      // 更多学生统计...
    ]
  }
}
```

## 业务流程图

### 课程与签到流程

```
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│    老师创建课程   │────▶│  学生加入课程   │────▶│   课程群组形成   │
└───────┬───────┘     └───────────────┘     └───────┬───────┘
        │                                           │
        │                                           ▼
        │                                  ┌───────────────┐
        └─────────────────────────────────▶│  老师创建签到任务  │
                                          └───────┬───────┘
                                                  │
                                                  ▼
        ┌───────────────┐                 ┌───────────────┐
        │  学生查看课程任务  │◀───────────────│  任务推送给课程成员 │
        └───────┬───────┘                 └───────────────┘
                │
                ▼
        ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
        │  学生扫码进行签到  │────▶│  系统验证签到信息  │────▶│  记录签到结果   │
        └───────────────┘     └───────────────┘     └───────┬───────┘
                                                            │
                                                            ▼
        ┌───────────────┐                           ┌───────────────┐
        │  老师查看签到统计  │◀──────────────────────────│  生成课程统计数据  │
        └───────────────┘                           └───────────────┘
```

### 二维码签到详细流程

```
┌───────────────┐     ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  老师创建签到任务  │────▶│ 系统生成唯一签到码 │────▶│ 前端渲染二维码   │────▶│   展示给学生     │
└───────────────┘     └───────────────┘     └───────────────┘     └───────┬───────┘
                                                                          │
                                                                          ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  记录签到并返回结果 │◀────│  系统验证签到码   │◀────│ 发送签到信息到服务器 │◀────│  学生扫描二维码   │
└───────┬───────┘     └───────────────┘     └───────────────┘     └───────────────┘
        │
        ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  实时更新签到状态  │────▶│  汇总课程签到数据  │────▶│  生成签到报表    │────▶│  老师查看统计结果  │
└───────────────┘     └───────────────┘     └───────────────┘     └───────────────┘
``` 