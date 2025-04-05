package com.attendance.repository.user;

import com.attendance.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据仓库接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 通过用户名查找用户
     * 
     * @param username 用户名
     * @return 用户对象（可选）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 通过邮箱查找用户
     * 
     * @param email 邮箱
     * @return 用户对象（可选）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 判断邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);
} 