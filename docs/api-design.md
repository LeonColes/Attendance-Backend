# 考勤系统API设计文档

## 基础规范

### 基础URL
所有API均以 `/api` 为前缀。

### 请求方法
- **GET**: 获取资源
- **POST**: 创建资源、执行操作或更新资源

### 响应格式
所有API响应均使用JSON格式，包含以下字段：
```json
{
  "code": 200,           // 状态码：200成功，非200表示错误
  "message": "操作成功",   // 操作结果描述
  "data": {}             // 响应数据，可能是对象或数组
}
```

### 分页格式
分页查询返回格式：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [],        // 当前页数据
    "totalElements": 100, // 总记录数
    "totalPages": 10,     // 总页数
    "size": 10,           // 每页大小
    "number": 0,          // 当前页码(从0开始)
    "first": true,        // 是否为第一页
    "last": false,        // 是否为最后一页
    "empty": false        // 是否为空结果
  }
}
```

### 分页参数
所有列表查询支持以下分页参数：
- `page`: 页码(从0开始)，默认0
- `size`: 每页大小，默认10，最大100

### 错误码
- 200: 成功
- 400: 请求参数错误
- 401: 未认证
- 403: 权限不足
- 404: 资源不存在
- 409: 资源冲突
- 500: 服务器内部错误

## 认证API

### 用户登录
- **URL**: `/api/auth/login`
- **方法**: POST
- **描述**: 登录系统并获取认证令牌
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "accessToken": "JWT令牌",
      "tokenType": "Bearer",
      "userId": "用户ID",
      "username": "用户名",
      "fullName": "用户全名",
      "role": "用户角色"
    }
  }
  ```

### 用户注册
- **URL**: `/api/auth/register`
- **方法**: POST
- **描述**: 注册新用户
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码",
    "fullName": "用户全名",
    "email": "邮箱",
    "phone": "手机号",
    "role": "STUDENT或TEACHER"
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "id": "用户ID",
      "username": "用户名",
      "fullName": "用户全名",
      "email": "邮箱",
      "role": "角色",
      "enabled": true,
      "createdAt": "创建时间",
      "updatedAt": "更新时间"
    }
  }
  ```

## 用户API

### 获取当前用户信息
- **URL**: `/api/users/current`
- **方法**: GET
- **描述**: 获取当前登录用户的信息
- **响应**:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": "用户ID",
      "username": "用户名",
      "fullName": "用户全名",
      "email": "邮箱",
      "phone": "手机号",
      "role": "角色",
      "enabled": true,
      "createdAt": "创建时间",
      "updatedAt": "更新时间"
    }
  }
  ```

### 获取用户列表
- **URL**: `/api/users/list`
- **方法**: GET
- **描述**: 获取用户列表
- **权限**: 仅管理员和教师
- **查询参数**:
  - `role`(可选): 用户角色，如STUDENT、TEACHER
  - `keyword`(可选): 搜索关键词，匹配用户名、全名和邮箱
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式

### 获取用户详情
- **URL**: `/api/users/detail`
- **方法**: GET
- **描述**: 获取用户详情
- **查询参数**:
  - `id`: 用户ID
- **权限**: 仅允许管理员或用户本人访问
- **响应**:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": "用户ID",
      "username": "用户名",
      "fullName": "用户全名",
      "email": "邮箱",
      "phone": "手机号",
      "role": "角色",
      "enabled": true,
      "createdAt": "创建时间",
      "updatedAt": "更新时间"
    }
  }
  ```

### 更新用户信息
- **URL**: `/api/users/update`
- **方法**: POST
- **描述**: 更新用户信息
- **权限**: 仅允许管理员或用户本人修改
- **请求体**:
  ```json
  {
    "id": "用户ID",
    "fullName": "更新的全名",
    "email": "更新的邮箱",
    "phone": "更新的手机号"
  }
  ```
- **响应**: 更新后的用户信息

## 课程API

### 创建课程
- **URL**: `/api/courses/create`
- **方法**: POST
- **描述**: 创建新课程
- **权限**: 教师和管理员
- **请求体**:
  ```json
  {
    "name": "课程名称",
    "description": "课程描述",
    "startDate": "开始日期",
    "endDate": "结束日期"
  }
  ```
- **响应**: 创建的课程信息

### 获取课程列表
- **URL**: `/api/courses/list`
- **方法**: GET
- **描述**: 获取课程列表
- **查询参数**:
  - `status`(可选): 课程状态，如ACTIVE、ENDED
  - `keyword`(可选): 搜索关键词，匹配课程名称和描述
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式的课程列表

### 获取我的课程
- **URL**: `/api/courses/my-list`
- **方法**: GET
- **描述**: 获取当前用户参与的所有课程
- **查询参数**:
  - `status`(可选): 课程状态
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式的课程列表

### 获取课程详情
- **URL**: `/api/courses/detail`
- **方法**: GET
- **描述**: 获取课程详情
- **查询参数**:
  - `id`: 课程ID
- **权限**: 课程成员才可访问
- **响应**: 课程详情信息

### 更新课程
- **URL**: `/api/courses/update`
- **方法**: POST
- **描述**: 更新课程信息
- **权限**: 仅课程创建者或管理员
- **请求体**:
  ```json
  {
    "id": "课程ID",
    "name": "更新的名称",
    "description": "更新的描述",
    "startDate": "更新的开始日期",
    "endDate": "更新的结束日期",
    "status": "更新的状态"
  }
  ```
- **响应**: 更新后的课程信息

### 删除课程
- **URL**: `/api/courses/remove`
- **方法**: POST
- **描述**: 逻辑删除课程
- **权限**: 仅课程创建者或管理员
- **请求体**:
  ```json
  {
    "id": "课程ID",
    "deleteReason": "删除原因(可选)"
  }
  ```
- **响应**: 成功消息

## 课程成员API

### 获取课程成员列表
- **URL**: `/api/courses/members/list`
- **方法**: GET
- **描述**: 获取课程成员列表
- **权限**: 课程成员才可访问
- **查询参数**:
  - `courseId`: 课程ID
  - `role`(可选): 成员角色
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式的成员列表

### 添加课程成员
- **URL**: `/api/courses/members/add`
- **方法**: POST
- **描述**: 添加课程成员
- **权限**: 仅课程创建者或助教
- **请求体**:
  ```json
  {
    "courseId": "课程ID",
    "userIds": ["用户ID1", "用户ID2", ...],
    "role": "成员角色(如STUDENT)"
  }
  ```
- **响应**: 添加成功消息和添加的成员列表

### 移除课程成员
- **URL**: `/api/courses/members/remove`
- **方法**: POST
- **描述**: 移除课程成员
- **权限**: 仅课程创建者或助教
- **请求体**:
  ```json
  {
    "courseId": "课程ID",
    "userId": "用户ID",
    "removeReason": "移除原因(可选)"
  }
  ```
- **响应**: 成功消息

### 加入课程
- **URL**: `/api/courses/members/join`
- **方法**: POST
- **描述**: 通过邀请码加入课程
- **请求体**:
  ```json
  {
    "code": "课程邀请码",
    "joinMethod": "加入方式(CODE或QR_CODE)"
  }
  ```
- **响应**: 成功消息和课程信息

## 签到任务API

### 创建签到任务
- **URL**: `/api/courses/tasks/create`
- **方法**: POST
- **描述**: 创建课程签到任务
- **权限**: 仅课程创建者或助教
- **请求体**:
  ```json
  {
    "parentCourseId": "所属课程ID",
    "name": "签到任务名称",
    "description": "签到任务描述",
    "startTime": "签到开始时间",
    "endTime": "签到结束时间",
    "checkinType": "签到类型(QR_CODE/LOCATION/WIFI/MANUAL)",
    "verifyParams": {
      // 根据签到类型不同而不同
      // QR_CODE: {}
      // LOCATION: {"latitude": 纬度, "longitude": 经度, "radius": 半径}
      // WIFI: {"ssid": "WiFi名称", "bssid": "MAC地址"}
    }
  }
  ```
- **响应**: 创建的签到任务信息

### 获取签到任务列表
- **URL**: `/api/courses/tasks/list`
- **方法**: GET
- **描述**: 获取课程的签到任务列表
- **权限**: 课程成员才可访问
- **查询参数**:
  - `courseId`: 课程ID
  - `status`(可选): 任务状态
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式的签到任务列表

### 更新签到任务
- **URL**: `/api/courses/tasks/update`
- **方法**: POST
- **描述**: 更新签到任务
- **权限**: 仅任务创建者或课程管理员
- **请求体**:
  ```json
  {
    "id": "签到任务ID",
    "name": "更新的名称",
    "description": "更新的描述",
    "startTime": "更新的开始时间",
    "endTime": "更新的结束时间",
    "status": "更新的状态"
  }
  ```
- **响应**: 更新后的签到任务信息

### 删除签到任务
- **URL**: `/api/courses/tasks/remove`
- **方法**: POST
- **描述**: 逻辑删除签到任务
- **权限**: 仅任务创建者或课程管理员
- **请求体**:
  ```json
  {
    "id": "签到任务ID",
    "deleteReason": "删除原因(可选)"
  }
  ```
- **响应**: 成功消息

## 签到记录API

### 提交签到
- **URL**: `/api/attendance/records/create`
- **方法**: POST
- **描述**: 学生提交签到
- **请求体**:
  ```json
  {
    "courseId": "签到任务ID",
    "location": {
      "latitude": 纬度,
      "longitude": 经度
    },
    "device": "设备信息",
    "verifyMethod": "签到方式(QR_CODE/LOCATION/WIFI)",
    "verifyData": "签到验证数据"
  }
  ```
- **响应**: 签到结果

### 获取签到记录列表
- **URL**: `/api/attendance/records/list`
- **方法**: GET
- **描述**: 获取签到记录
- **权限**: 教师可查看所有记录，学生只能查看自己的记录
- **查询参数**:
  - `courseId`(可选): 签到任务ID
  - `parentCourseId`(可选): 所属课程ID
  - `userId`(可选): 用户ID
  - `status`(可选): 签到状态
  - `startDate`(可选): 签到日期起始
  - `endDate`(可选): 签到日期结束
  - `page`: 页码
  - `size`: 每页大小
- **响应**: 分页格式的签到记录列表

### 手动签到
- **URL**: `/api/attendance/records/manual-create`
- **方法**: POST
- **描述**: 教师为学生手动添加签到记录
- **权限**: 仅课程创建者或助教
- **请求体**:
  ```json
  {
    "courseId": "签到任务ID",
    "userId": "学生用户ID",
    "status": "签到状态(NORMAL/LATE/LEAVE)"
  }
  ```
- **响应**: 创建的签到记录

### 更新签到记录
- **URL**: `/api/attendance/records/update`
- **方法**: POST
- **描述**: 更新签到记录（如修改状态）
- **权限**: 仅课程创建者或助教
- **请求体**:
  ```json
  {
    "id": "签到记录ID",
    "status": "更新的状态",
    "comment": "备注信息(可选)"
  }
  ```
- **响应**: 更新后的签到记录

## 统计API

### 获取课程签到统计
- **URL**: `/api/statistics/course`
- **方法**: GET
- **描述**: 获取课程签到统计信息
- **权限**: 仅课程创建者或助教
- **查询参数**:
  - `courseId`: 课程ID
- **响应**:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "courseId": "课程ID",
      "courseName": "课程名称",
      "totalStudents": 30,
      "totalTasks": 10,
      "attendanceRate": 0.95,
      "statusDistribution": {
        "NORMAL": 280,
        "LATE": 12,
        "ABSENT": 8
      },
      "taskStatistics": [
        {
          "taskId": "任务ID",
          "taskName": "任务名称",
          "date": "日期",
          "normalCount": 28,
          "lateCount": 1,
          "absentCount": 1,
          "attendanceRate": 0.97
        }
      ]
    }
  }
  ```

### 获取学生签到统计
- **URL**: `/api/statistics/student`
- **方法**: GET
- **描述**: 获取学生签到统计信息
- **查询参数**:
  - `courseId`: 课程ID
  - `userId`: 学生ID (如不提供，则获取当前用户统计)
- **响应**:
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "userId": "学生ID",
      "userName": "学生姓名",
      "courseId": "课程ID",
      "courseName": "课程名称",
      "totalTasks": 10,
      "attendanceRate": 0.9,
      "normalCount": 8,
      "lateCount": 1,
      "absentCount": 1,
      "records": [
        {
          "taskId": "任务ID",
          "taskName": "任务名称",
          "date": "日期",
          "status": "签到状态",
          "checkInTime": "签到时间"
        }
      ]
    }
  }
  ```

## 流程图

### 用户认证流程
```mermaid
sequenceDiagram
    participant 客户端
    participant 认证服务
    participant JWT服务
    participant 用户服务
    
    客户端->>认证服务: 登录请求(用户名/密码)
    认证服务->>用户服务: 验证用户凭证
    用户服务-->>认证服务: 验证结果
    认证服务->>JWT服务: 生成JWT令牌
    JWT服务-->>认证服务: 返回令牌
    认证服务-->>客户端: 返回认证结果和令牌
    
    客户端->>认证服务: 请求受保护资源(带令牌)
    认证服务->>JWT服务: 验证令牌
    JWT服务-->>认证服务: 验证结果
    认证服务-->>客户端: 返回请求的资源
```

### 签到流程
```mermaid
sequenceDiagram
    participant 学生客户端
    participant 教师客户端
    participant 签到服务
    participant 课程服务
    
    教师客户端->>课程服务: 创建签到任务
    课程服务-->>教师客户端: 返回签到任务信息(含二维码或位置)
    
    教师客户端->>学生客户端: 分享签到方式(展示二维码等)
    学生客户端->>签到服务: 提交签到(扫码/位置/WiFi)
    签到服务->>课程服务: 验证签到有效性
    课程服务-->>签到服务: 验证结果
    签到服务-->>学生客户端: 返回签到结果
    
    教师客户端->>签到服务: 获取签到统计
    签到服务-->>教师客户端: 返回签到统计结果
```

### 课程管理流程
```mermaid
sequenceDiagram
    participant 教师客户端
    participant 学生客户端
    participant 课程服务
    participant 用户服务
    
    教师客户端->>课程服务: 创建课程
    课程服务-->>教师客户端: 返回课程信息(含邀请码)
    
    教师客户端->>学生客户端: 分享课程邀请码
    学生客户端->>课程服务: 加入课程(使用邀请码)
    课程服务->>用户服务: 验证用户
    用户服务-->>课程服务: 用户信息
    课程服务-->>学生客户端: 加入结果
    
    教师客户端->>课程服务: 创建签到任务
    课程服务-->>教师客户端: 返回任务信息
    
    学生客户端->>课程服务: 提交签到
    课程服务-->>学生客户端: 签到结果
``` 