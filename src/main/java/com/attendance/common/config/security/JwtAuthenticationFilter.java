package com.attendance.common.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 拦截请求，验证JWT令牌，设置认证信息
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    
    // 不需要验证的路径
    private final List<String> PUBLIC_PATHS = Arrays.asList(
        "/auth/**",
        "/public/**",
        "/test/public",
        "/test/login",
        "/h2-console/**",
        "/v3/api-docs/**", 
        "/swagger-ui/**", 
        "/swagger-ui.html"
    );
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    /**
     * 是否应该过滤器处理该请求
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 如果是公开路径，则跳过JWT验证
        boolean shouldSkip = PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        log.info("请求路径: {}, 是否跳过验证: {}", path, shouldSkip);
        return shouldSkip;
    }
    
    /**
     * 过滤请求
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getServletPath();
        log.info("过滤请求: {}, 方法: {}", path, request.getMethod());
        
        try {
            // 从请求头中获取JWT令牌
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                log.info("JWT令牌存在: {}", jwt.substring(0, Math.min(10, jwt.length())) + "...");
            } else {
                log.info("请求中没有JWT令牌");
            }
            
            // 如果令牌存在且有效，设置认证信息
            if (StringUtils.hasText(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.info("认证成功: {}, 角色: {}", username, userDetails.getAuthorities());
                    } else {
                        log.warn("JWT令牌验证失败");
                    }
                }
            }
        } catch (Exception ex) {
            log.error("无法设置用户认证: {}", ex.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中获取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}