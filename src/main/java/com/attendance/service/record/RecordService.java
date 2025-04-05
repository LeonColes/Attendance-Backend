package com.attendance.service.record;

import com.attendance.model.dto.record.RecordDTO;

import java.util.List;

/**
 * 签到记录服务接口
 */
public interface RecordService {
    
    /**
     * 获取签到记录
     * 
     * @param id 记录ID
     * @return 签到记录
     */
    RecordDTO getRecord(String id);
    
    /**
     * 获取签到任务的所有签到记录
     * 
     * @param courseId 签到任务ID
     * @return 签到记录列表
     */
    List<RecordDTO> getRecordsByCourse(String courseId);
    
    /**
     * 获取用户的所有签到记录
     * 
     * @param userId 用户ID
     * @return 签到记录列表
     */
    List<RecordDTO> getRecordsByUser(String userId);
    
    /**
     * 获取课程的所有签到记录
     * 
     * @param parentCourseId 父课程ID
     * @return 签到记录列表
     */
    List<RecordDTO> getRecordsByParentCourse(String parentCourseId);
    
    /**
     * 提交签到
     * 
     * @param courseId 签到任务ID
     * @param verifyMethod 验证方式
     * @param location 位置信息
     * @param device 设备信息
     * @param verifyData 验证数据
     * @return 签到记录
     */
    RecordDTO submitCheckIn(String courseId, String verifyMethod, String location, String device, String verifyData);
} 