package com.brushing.framework.front;

import com.brushing.common.utils.DateUtils;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.brushing.common.core.redis.RedisCache;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class FrontJwtUtil {
    @Value("${token.secret}")
    private String SECRET_KEY;

    private static final long EXPIRATION_TIME = 24*3600*1000*3; // 3天

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private IOrderMemberUserService userService;

    private static final Logger logger = LoggerFactory.getLogger(FrontJwtUtil.class);

    public String generateToken(String username) {
        try {
            // 获取用户现有的所有 token
            Set<String> existingTokens = redisCache.getCacheSet("user:tokens:" + username);
            if (existingTokens != null && !existingTokens.isEmpty()) {
                // 删除所有旧的 token:valid:<token> 记录
                for (String oldToken : existingTokens) {
                    redisCache.deleteObject("token:valid:" + oldToken);
                    logger.info("Removed old token for user: {}, token: {}", username, oldToken);
                }
                // 清空 user:tokens:<username> 集合
                redisCache.deleteObject("user:tokens:" + username);
            }
        } catch (Exception e) {
            logger.error("Failed to clear old tokens for user: {}, error: {}", username, e.getMessage());
        }

        // 生成新 token
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(DateUtils.getNowDate())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();

        // 将新 token 存入 Redis
        try {
            redisCache.setCacheObject("token:valid:" + token, username, (int) (EXPIRATION_TIME / 1000), TimeUnit.SECONDS);
            redisCache.setCacheSet("user:tokens:" + username, Collections.singleton(token));
            logger.info("Generated new token for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to store new token for user: {}, error: {}", username, e.getMessage());
            throw new RuntimeException("Failed to store token in Redis", e);
        }
        return token;
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            // 检查 token 是否有效（存在于 Redis）
            if (!redisCache.hasKey("token:valid:" + token)) {
                logger.warn("Token not found in valid tokens: {}", token);
                return false;
            }

            // 验证 token 签名和过期时间
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();

            // 检查用户是否有效
            String username = claims.getSubject();
            OrderMemberUser user = userService.findByUsername(username);
            if (user == null || !user.getAccountStatus().equals("0")) {
                logger.warn("Invalid user: {} for token: {}", username, token);
                // 移除无效 token
                try {
                    redisCache.deleteObject("token:valid:" + token);
                    redisCache.deleteObject("user:tokens:" + username); // 改为删除整个集合
                } catch (Exception e) {
                    logger.error("Failed to remove invalid token for user: {}, token: {}, error: {}", username, token, e.getMessage());
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationTimeFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration()
                    .getTime();
        } catch (Exception e) {
            logger.error("Failed to extract expiration time from token: {}", e.getMessage());
            return 0;
        }
    }

    public void invalidateToken(String token, String username) {
        try {
            // 移除 token:valid:<token>
            if (redisCache.hasKey("token:valid:" + token)) {
                redisCache.deleteObject("token:valid:" + token);
                logger.info("Removed token:valid:{}", token);
            } else {
                logger.warn("Token not found in Redis: token:valid:{}", token);
            }

            // 从 user:tokens:<username> 中移除 token
            if (redisCache.hasKey("user:tokens:" + username)) {
                redisCache.deleteCacheSetValue("user:tokens:" + username, token);
                logger.info("Removed token from user:tokens:{}", username);
            } else {
                logger.warn("Token set not found in Redis: user:tokens:{}", username);
            }
        } catch (Exception e) {
            logger.error("Failed to invalidate token for user: {}, token: {}, error: {}", username, token, e.getMessage());
            throw new RuntimeException("Failed to invalidate token in Redis: " + e.getMessage(), e);
        }
    }
}