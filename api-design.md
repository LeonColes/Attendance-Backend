# 智能考勤系统 API 设计文档

本文档描述智能考勤系统的RESTful API接口设计，包含接口定义和curl调用示例。
所有接口使用JSON格式进行数据交换，JWT令牌用于认证授权。

## 目录

- [认证接口](#认证接口)
- [用户接口](#用户接口)
- [任务接口](#任务接口)
- [签到记录接口](#签到记录接口)
- [统计接口](#统计接口)

## 基础信息

- 基础URL: `http://localhost:8080/api`
- 认证方式: Bearer Token (JWT)
- 内容类型: `application/json`

所有请求都需要添加标准头：
```
Content-Type: application/json
```

认证请求需要添加令牌头：
```
Authorization: Bearer {token}
```

## 认证接口

认证接口用于用户登录、注册和令牌管理。

| 接口        | 方法 | URL              | 描述       | 需要认证 |
|------------|------|------------------|-----------|---------|
| 用户登录     | POST | /auth/login      | 用户登录并获取令牌 | 否 |
| 用户注册     | POST | /auth/register   | 注册新用户   | 否 |
| 刷新令牌     | POST | /auth/refresh    | 刷新访问令牌 | 是 |
| 用户注销     | POST | /auth/logout     | 使当前令牌失效 | 是 |

### 登录接口

**请求示例**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher1",
    "password": "password123"
  }'
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "{{token}}",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "teacher1",
    "fullName": "张老师",
    "role": "TEACHER"
  }
}
```

### 注册接口

**请求示例**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "password123",
    "fullName": "李同学",
    "email": "student1@example.com",
    "role": "STUDENT"
  }'
```

**成功响应**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 3,
    "username": "student1",
    "fullName": "李同学",
    "email": "student1@example.com",
    "role": "STUDENT"
  }
}
```

## 用户接口

用户接口用于管理用户信息和权限。

| 接口         | 方法   | URL                | 描述         | 需要认证 |
|-------------|-------|-------------------|-------------|---------|
| 获取当前用户   | GET   | /users/me         | 获取当前用户信息 | 是 |
| 获取用户信息   | GET   | /users/{id}       | 获取指定用户信息 | 是 |
| 获取用户列表   | GET   | /users            | 获取用户列表(分页) | 是(ADMIN/TEACHER) |
| 更新用户信息   | PUT   | /users/{id}       | 更新用户信息    | 是 |
| 重置用户密码   | POST  | /users/{id}/reset-password | 重置用户密码 | 是(ADMIN) |
| 修改用户状态   | PATCH | /users/{id}/status | 启用/禁用用户   | 是(ADMIN) |

### 获取当前用户信息

**请求示例**

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "teacher1",
    "fullName": "张老师",
    "email": "teacher@example.com",
    "role": "TEACHER",
    "enabled": true,
    "createdAt": "2023-07-01T10:30:00",
    "updatedAt": "2023-07-01T10:30:00"
  }
}
```

### 获取用户列表

**请求示例**

```bash
curl -X GET "http://localhost:8080/api/users?page=0&size=10" \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "teacher1",
        "fullName": "张老师",
        "email": "teacher@example.com",
        "role": "TEACHER",
        "enabled": true
      },
      {
        "id": 2,
        "username": "student1",
        "fullName": "李同学",
        "email": "student1@example.com",
        "role": "STUDENT",
        "enabled": true
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

## 任务接口

任务接口用于管理签到任务。

| 接口         | 方法   | URL                   | 描述          | 需要认证 |
|-------------|-------|----------------------|--------------|---------|
| 创建任务      | POST  | /tasks               | 创建新签到任务   | 是(TEACHER/ADMIN) |
| 获取任务详情   | GET   | /tasks/{id}          | 获取任务详情    | 是 |
| 获取任务列表   | GET   | /tasks               | 获取任务列表(分页) | 是 |
| 更新任务      | PUT   | /tasks/{id}          | 更新任务信息     | 是(创建者/ADMIN) |
| 取消任务      | DELETE| /tasks/{id}          | 取消/删除任务    | 是(创建者/ADMIN) |
| 获取用户任务   | GET   | /tasks/creator/{id}  | 获取用户创建的任务 | 是 |

### 创建任务

**请求示例**

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{token}}" \
  -d '{
    "title": "周一早课签到",
    "description": "数据结构课堂签到",
    "startTime": "2023-07-03T08:00:00",
    "endTime": "2023-07-03T09:30:00",
    "checkInType": "QR_CODE",
    "verifyParams": "{\"validRadius\": 100}"
  }'
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "周一早课签到",
    "description": "数据结构课堂签到",
    "creatorId": 1,
    "creatorUsername": "teacher1",
    "startTime": "2023-07-03T08:00:00",
    "endTime": "2023-07-03T09:30:00",
    "status": "CREATED",
    "checkInType": "QR_CODE",
    "verifyParams": "{\"validRadius\": 100}",
    "createdAt": "2023-07-02T15:30:00",
    "updatedAt": "2023-07-02T15:30:00"
  }
}
```

### 获取任务列表

**请求示例**

```bash
curl -X GET "http://localhost:8080/api/tasks?page=0&size=10&status=ACTIVE" \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "周一早课签到",
        "description": "数据结构课堂签到",
        "creatorId": 1,
        "creatorUsername": "teacher1",
        "startTime": "2023-07-03T08:00:00",
        "endTime": "2023-07-03T09:30:00",
        "status": "ACTIVE",
        "checkInType": "QR_CODE"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

## 签到记录接口

签到记录接口用于管理用户签到信息。

| 接口          | 方法   | URL                      | 描述            | 需要认证 |
|--------------|-------|--------------------------|----------------|---------|
| 提交签到       | POST  | /records                | 提交签到信息      | 是 |
| 获取签到记录    | GET   | /records/{id}           | 获取签到记录详情   | 是 |
| 获取任务签到记录 | GET   | /records/task/{taskId}  | 获取任务的签到记录  | 是(创建者/ADMIN) |
| 获取用户签到记录 | GET   | /records/user/{userId}  | 获取用户的签到记录  | 是 |
| 批量签到操作    | POST  | /records/batch          | 批量签到操作      | 是(TEACHER/ADMIN) |
| 更新签到状态    | PATCH | /records/{id}/status    | 更新签到状态      | 是(TEACHER/ADMIN) |

### 提交签到

**请求示例 (二维码签到)**

```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{token}}" \
  -d '{
    "taskId": 1,
    "verifyMethod": "QR_CODE",
    "location": "{\"latitude\": 39.9087243, \"longitude\": 116.3952859}",
    "device": "iPhone 13, iOS 15.4",
    "verifyData": "{\"code\": \"a1b2c3d4\"}"
  }'
```

**成功响应**

```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "id": 1,
    "userId": 2,
    "username": "student1",
    "userFullName": "李同学",
    "taskId": 1,
    "taskTitle": "周一早课签到",
    "status": "NORMAL",
    "checkInTime": "2023-07-03T08:15:30",
    "location": "{\"latitude\": 39.9087243, \"longitude\": 116.3952859}",
    "device": "iPhone 13, iOS 15.4",
    "verifyMethod": "QR_CODE"
  }
}
```

### 获取任务签到记录

**请求示例**

```bash
curl -X GET "http://localhost:8080/api/records/task/1?page=0&size=20" \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 2,
        "username": "student1",
        "userFullName": "李同学",
        "taskId": 1,
        "taskTitle": "周一早课签到",
        "status": "NORMAL",
        "checkInTime": "2023-07-03T08:15:30"
      },
      {
        "id": 2,
        "userId": 3,
        "username": "student2",
        "userFullName": "王同学",
        "taskId": 1,
        "taskTitle": "周一早课签到",
        "status": "LATE",
        "checkInTime": "2023-07-03T08:45:20"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

## 统计接口

统计接口用于获取考勤统计数据。

| 接口            | 方法 | URL                          | 描述            | 需要认证 |
|----------------|------|------------------------------|----------------|---------|
| 任务签到统计     | GET  | /statistics/tasks/{id}      | 任务签到统计数据  | 是(创建者/ADMIN) |
| 用户签到统计     | GET  | /statistics/users/{id}      | 用户签到统计数据  | 是 |
| 时间段签到统计   | GET  | /statistics/period          | 时间段内签到统计  | 是(TEACHER/ADMIN) |
| 导出签到数据     | POST | /statistics/export          | 导出签到数据     | 是(TEACHER/ADMIN) |

### 任务签到统计

**请求示例**

```bash
curl -X GET http://localhost:8080/api/statistics/tasks/1 \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 1,
    "taskTitle": "周一早课签到",
    "totalStudents": 30,
    "attendCount": 25,
    "lateCount": 3,
    "absentCount": 2,
    "attendRate": 83.33,
    "lateRate": 10.00,
    "absentRate": 6.67,
    "statusStats": [
      {
        "status": "NORMAL",
        "count": 25,
        "percentage": 83.33
      },
      {
        "status": "LATE",
        "count": 3,
        "percentage": 10.00
      },
      {
        "status": "ABSENT",
        "count": 2,
        "percentage": 6.67
      }
    ]
  }
}
```

### 用户签到统计

**请求示例**

```bash
curl -X GET "http://localhost:8080/api/statistics/users/2?startDate=2023-07-01&endDate=2023-07-31" \
  -H "Authorization: Bearer {{token}}"
```

**成功响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 2,
    "username": "student1",
    "fullName": "李同学",
    "totalTasks": 20,
    "attendCount": 18,
    "lateCount": 1,
    "absentCount": 1,
    "attendRate": 90.00,
    "lateRate": 5.00,
    "absentRate": 5.00,
    "statusStats": [
      {
        "status": "NORMAL",
        "count": 18,
        "percentage": 90.00
      },
      {
        "status": "LATE",
        "count": 1,
        "percentage": 5.00
      },
      {
        "status": "ABSENT",
        "count": 1,
        "percentage": 5.00
      }
    ]
  }
}
```

## API使用流程

本节描述不同角色使用API的典型流程，以便开发人员理解API调用的顺序和上下文。

### 教师创建并管理签到流程

1. **用户登录**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "teacher1",
       "password": "password123"
     }'
   ```

2. **创建签到任务**
   ```bash
   curl -X POST http://localhost:8080/api/tasks \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "title": "周一数据结构课签到",
       "description": "第七周周一上午数据结构课堂签到",
       "startTime": "2023-07-03T08:00:00",
       "endTime": "2023-07-03T09:30:00",
       "checkInType": "QR_CODE",
       "verifyParams": "{\"validRadius\": 100}"
     }'
   ```

3. **获取签到任务列表**
   ```bash
   curl -X GET "http://localhost:8080/api/tasks?page=0&size=10&status=ACTIVE" \
     -H "Authorization: Bearer {token}"
   ```

4. **查看任务的签到记录**
   ```bash
   curl -X GET "http://localhost:8080/api/records/task/1?page=0&size=20" \
     -H "Authorization: Bearer {token}"
   ```

5. **查看签到统计**
   ```bash
   curl -X GET http://localhost:8080/api/statistics/tasks/1 \
     -H "Authorization: Bearer {token}"
   ```

6. **导出签到数据**
   ```bash
   curl -X POST http://localhost:8080/api/statistics/export \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "taskId": 1,
       "format": "EXCEL"
     }'
   ```

7. **结束签到任务**
   ```bash
   curl -X PUT http://localhost:8080/api/tasks/1 \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "status": "ENDED"
     }'
   ```

### 学生签到流程

1. **用户登录**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "student1",
       "password": "password123"
     }'
   ```

2. **查看活跃的签到任务**
   ```bash
   curl -X GET "http://localhost:8080/api/tasks?page=0&size=10&status=ACTIVE" \
     -H "Authorization: Bearer {token}"
   ```

3. **提交签到（二维码方式）**
   ```bash
   curl -X POST http://localhost:8080/api/records \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "taskId": 1,
       "verifyMethod": "QR_CODE",
       "location": "{\"latitude\": 39.9087243, \"longitude\": 116.3952859}",
       "device": "iPhone 13, iOS 15.4",
       "verifyData": "{\"code\": \"a1b2c3d4\"}"
     }'
   ```

4. **查看个人签到记录**
   ```bash
   curl -X GET "http://localhost:8080/api/records/user/me?page=0&size=10" \
     -H "Authorization: Bearer {token}"
   ```

5. **查看个人签到统计**
   ```bash
   curl -X GET "http://localhost:8080/api/statistics/users/me?startDate=2023-07-01&endDate=2023-07-31" \
     -H "Authorization: Bearer {token}"
   ```

### 管理员管理用户流程

1. **管理员登录**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "admin",
       "password": "admin123"
     }'
   ```

2. **获取用户列表**
   ```bash
   curl -X GET "http://localhost:8080/api/users?page=0&size=10" \
     -H "Authorization: Bearer {token}"
   ```

3. **创建新教师账号**
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "username": "teacher2",
       "password": "password123",
       "fullName": "王老师",
       "email": "teacher2@example.com",
       "role": "TEACHER"
     }'
   ```

4. **修改用户状态（禁用用户）**
   ```bash
   curl -X PATCH http://localhost:8080/api/users/3/status \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "enabled": false
     }'
   ```

5. **重置用户密码**
   ```bash
   curl -X POST http://localhost:8080/api/users/3/reset-password \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer {token}" \
     -d '{
       "newPassword": "newpassword123"
     }'
   ```

### 接口调用注意事项

1. **认证要求**：
   - 除了登录和注册接口外，所有接口都需要在请求头中携带JWT令牌
   - 令牌格式：`Authorization: Bearer {token}`

2. **权限控制**：
   - 用户只能访问自己有权限的资源
   - 教师可以创建和管理自己创建的任务
   - 学生只能查看自己的签到记录和统计
   - 管理员可以访问和管理所有资源

3. **数据格式**：
   - 所有请求和响应数据均使用JSON格式
   - 日期时间格式遵循ISO 8601标准：`YYYY-MM-DDThh:mm:ss`

4. **错误处理**：
   - 成功响应状态码为200，data字段包含结果数据
   - 失败响应包含错误代码和错误消息
   - 认证失败返回401，权限不足返回403

5. **分页查询**：
   - 列表查询支持分页，默认每页大小为10
   - 使用page参数指定页码（从0开始），使用size参数指定每页大小
   - 响应中包含分页信息和总记录数

## 系统功能流程与接口调用逻辑

本节描述系统主要功能流程和接口调用逻辑，帮助开发人员理解各个接口之间的关系和调用顺序。

### 系统功能流程图

```
+----------------+    +-----------------+    +----------------+
|                |    |                 |    |                |
|   用户认证      |--->|   任务管理      |--->|   签到管理     |
|                |    |                 |    |                |
+----------------+    +-----------------+    +----------------+
        |                     |                      |
        v                     v                      v
+----------------+    +-----------------+    +----------------+
|                |    |                 |    |                |
|   用户管理      |    |   数据统计      |<---|   记录查询     |
|                |    |                 |    |                |
+----------------+    +-----------------+    +----------------+
```

### 完整功能场景：课堂签到流程

下面以一个完整的课堂签到流程为例，展示各接口的调用逻辑：

#### 1. 前期准备

1. **管理员创建用户账号**
   - 管理员登录系统
   - 使用 `POST /auth/register` 创建教师和学生账号
   - 分配适当的角色权限

#### 2. 签到任务创建与管理

1. **教师创建签到任务**
   - 教师登录系统 (`POST /auth/login`)
   - 创建新的签到任务 (`POST /tasks`)
   - 设置任务标题、时间范围、签到类型等参数

2. **任务状态管理**
   - 系统根据任务开始时间自动激活任务
   - 教师可手动更新任务状态 (`PUT /tasks/{id}`)
   - 任务结束后更新状态为"已结束"

#### 3. 学生签到流程

1. **查看可签到任务**
   - 学生登录系统 (`POST /auth/login`)
   - 获取当前可签到任务列表 (`GET /tasks?status=ACTIVE`)

2. **执行签到操作**
   - 根据任务指定的签到方式准备签到数据
   - 提交签到信息 (`POST /records`)
   - 获取签到结果

3. **查看签到历史**
   - 查看个人所有签到记录 (`GET /records/user/me`)
   - 查看个人签到统计数据 (`GET /statistics/users/me`)

#### 4. 统计与分析

1. **教师查看签到情况**
   - 查看任务的所有签到记录 (`GET /records/task/{taskId}`)
   - 获取签到统计数据 (`GET /statistics/tasks/{id}`)

2. **数据导出**
   - 导出签到数据为Excel格式 (`POST /statistics/export`)
   - 下载统计报表

### 接口调用逻辑详解

#### 认证与授权流程

1. **用户认证流程**：
   - 用户首先通过 `/auth/login` 获取JWT令牌
   - 所有后续请求在header中携带令牌
   - 令牌包含用户ID、用户名和角色信息
   - 系统根据令牌中的角色信息进行权限验证

2. **权限控制原则**：
   - ADMIN角色可访问所有接口
   - TEACHER角色可管理自己创建的任务和查看签到记录
   - STUDENT角色只能提交签到和查看自己的记录
   - 资源拥有者可以操作自己的资源（如教师管理自己创建的任务）

#### 任务生命周期管理

1. **任务状态流转**：
   ```
   创建(CREATED) --> 活跃(ACTIVE) --> 结束(ENDED)
                 \                /
                  --> 取消(CANCELED)
   ```

2. **状态变更规则**：
   - 新建任务状态为"已创建"(CREATED)
   - 当前时间达到任务开始时间时，状态自动变为"活跃"(ACTIVE)
   - 任务可以手动结束或到达结束时间自动结束，状态变为"已结束"(ENDED)
   - 任务可以在活跃前取消，状态变为"已取消"(CANCELED)
   - 已结束或已取消的任务状态不能再变更

#### 签到验证逻辑

1. **签到方式与验证**：
   - **二维码签到**：验证提交的二维码内容是否匹配
   - **位置签到**：验证签到位置是否在指定范围内
   - **WiFi签到**：验证连接的WiFi是否是指定网络
   - **手动签到**：教师手动确认签到

2. **签到状态判断**：
   - 正常签到时间内提交：状态为"正常"(NORMAL)
   - 迟到后提交：状态为"迟到"(LATE)
   - 未签到：系统自动标记为"缺席"(ABSENT)
   - 请假已批准：状态为"请假"(LEAVE)

#### 数据统计与分析

1. **统计维度**：
   - 按任务统计：特定任务的签到情况
   - 按用户统计：特定用户的签到历史
   - 按时间段统计：特定时间范围内的签到数据

2. **统计指标**：
   - 出勤率：正常签到人数/总人数
   - 迟到率：迟到人数/总人数
   - 缺席率：缺席人数/总人数
   - 各状态分布：各状态记录的数量和百分比

### 特殊场景处理

1. **网络异常处理**：
   - 客户端应实现签到数据本地缓存
   - 网络恢复后自动重新提交
   - 后端根据任务时间判断签到有效性

2. **批量操作**：
   - 教师可以使用 `POST /records/batch` 进行批量签到操作
   - 适用于手动点名或特殊情况下的批量签到

3. **数据修正**：
   - 教师可以使用 `PATCH /records/{id}/status` 修改签到状态
   - 用于处理特殊情况下的签到异常 