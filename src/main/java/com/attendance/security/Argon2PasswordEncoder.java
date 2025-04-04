package com.attendance.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

/**
 * Argon2密码编码器
 * 使用Argon2id算法进行密码加密，提供更高的安全性
 */
public class Argon2PasswordEncoder implements PasswordEncoder {

    private final Argon2 argon2;
    private final int iterations;
    private final int memory;
    private final int parallelism;
    private static final String PREFIX = "{argon2id}";
    
    /**
     * 构造函数
     */
    public Argon2PasswordEncoder() {
        // 使用Argon2id (混合模式，抵抗侧信道攻击和GPU暴力破解)
        this.argon2 = Argon2Factory.create(Argon2Types.ARGON2id);
        
        // 默认参数设置
        this.iterations = 2;        // 迭代次数
        this.memory = 65536;        // 内存占用 (64MB)
        this.parallelism = 1;       // 并行度
    }
    
    /**
     * 构造函数
     * 
     * @param iterations 迭代次数
     * @param memory 内存占用 (KB)
     * @param parallelism 并行度
     */
    public Argon2PasswordEncoder(int iterations, int memory, int parallelism) {
        this.argon2 = Argon2Factory.create(Argon2Types.ARGON2id);
        this.iterations = iterations;
        this.memory = memory;
        this.parallelism = parallelism;
    }
    
    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码哈希
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // 将密码转换为字符数组
        char[] passwordChars = rawPassword.toString().toCharArray();
        
        try {
            // 执行哈希操作，并添加前缀
            String hash = argon2.hash(iterations, memory, parallelism, passwordChars);
            return PREFIX + hash;
        } finally {
            // 安全清除密码字符数组
            argon2.wipeArray(passwordChars);
        }
    }
    
    /**
     * 验证密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码哈希
     * @return 密码是否匹配
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        // 检查是否包含前缀
        String actualEncodedPassword = encodedPassword;
        if (encodedPassword.startsWith(PREFIX)) {
            actualEncodedPassword = encodedPassword.substring(PREFIX.length());
        }
        
        // 将密码转换为字符数组
        char[] passwordChars = rawPassword.toString().toCharArray();
        
        try {
            // 执行验证操作
            return argon2.verify(actualEncodedPassword, passwordChars);
        } finally {
            // 安全清除密码字符数组
            argon2.wipeArray(passwordChars);
        }
    }
}
