package com.attendance.common.config.security;

import com.attendance.common.util.DateTimeUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌提供器
 * 负责生成和解析JWT令牌
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecretString;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    private SecretKey secretKey;
    
    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * 从令牌中获取声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 从令牌中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查令牌是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        LocalDateTime expirationDateTime = DateTimeUtil.toLocalDateTime(expiration);
        return expirationDateTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * 生成令牌
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> claims = new HashMap<>();
        
        // 可以添加其他声明，如角色信息
        if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
            claims.put("roles", authentication.getAuthorities().toString());
        }
        
        return doGenerateToken(claims, username);
    }
    
    /**
     * 生成令牌
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, username);
    }
    
    /**
     * 生成令牌的核心方法
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plus(Duration.ofMillis(jwtExpiration));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(DateTimeUtil.toDate(now))
                .setExpiration(DateTimeUtil.toDate(expiryDate))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 获取签名密钥
     * 如果配置的密钥不够安全，则动态生成安全密钥
     */
    private Key getSigningKey() {
        if (secretKey == null) {
            try {
                // 尝试使用配置的密钥
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecretString);
                
                // 检查密钥长度
                if (keyBytes.length * 8 >= 512) {
                    secretKey = Keys.hmacShaKeyFor(keyBytes);
                    log.info("使用配置的JWT密钥");
                } else {
                    // 密钥长度不足，生成新的安全密钥
                    secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
                    log.warn("配置的JWT密钥长度不足（需要至少512位），已自动生成安全密钥");
                }
            } catch (Exception e) {
                // 如果解码失败或其他错误，生成新的安全密钥
                secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
                log.error("JWT密钥处理错误，已自动生成安全密钥", e);
            }
        }
        
        return secretKey;
    }
    
    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}