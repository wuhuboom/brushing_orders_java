package com.order.framework.front;

import com.order.common.core.redis.RedisCache;
import com.order.common.utils.DateUtils;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT and server-side session storage for the public member API.
 *
 * <p>New tokens are stored by JWT id (jti), never by their full bearer value.
 * Legacy token keys remain readable for one token lifetime during rollout.</p>
 */
@Component
public class FrontJwtUtil {
    private static final Logger log = LoggerFactory.getLogger(FrontJwtUtil.class);
    private static final String TOKEN_KEY_PREFIX = "front:token:";
    private static final String USER_TOKEN_KEY_PREFIX = "front:user_tokens:";
    private static final String LEGACY_TOKEN_KEY_PREFIX = "token:valid:";
    private static final String LEGACY_USER_TOKEN_KEY_PREFIX = "user:tokens:";

    @Value("${token.secret}")
    private String secretKey;

    @Value("${front-token.expire-seconds:259200}")
    private int expirationSeconds;

    private final RedisCache redisCache;
    private final OrderUserMapper userMapper;

    public FrontJwtUtil(RedisCache redisCache, OrderUserMapper userMapper) {
        this.redisCache = redisCache;
        this.userMapper = userMapper;
    }

    public String generateToken(OrderUser user) {
        if (user == null || user.getId() == null || user.getUsername() == null) {
            throw new IllegalArgumentException("User identity is required");
        }

        invalidateAllTokens(user.getId(), user.getUsername());

        String jti = UUID.randomUUID().toString();
        Date issuedAt = DateUtils.getNowDate();
        Date expiration = new Date(issuedAt.getTime() + expirationSeconds * 1000L);
        String token = Jwts.builder()
                .setId(jti)
                .setSubject(user.getUsername())
                .claim("uid", user.getId())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();

        String tokenKey = TOKEN_KEY_PREFIX + jti;
        String userTokenKey = USER_TOKEN_KEY_PREFIX + user.getId();
        try {
            redisCache.setCacheObject(
                    tokenKey,
                    user.getId() + ":" + user.getUsername(),
                    expirationSeconds,
                    TimeUnit.SECONDS);
            redisCache.setCacheSet(userTokenKey, Collections.singleton(jti));
            redisCache.expire(userTokenKey, expirationSeconds, TimeUnit.SECONDS);
            log.info("event=front_token_issued userId={}", user.getId());
            return token;
        } catch (RuntimeException ex) {
            redisCache.deleteObject(tokenKey);
            redisCache.deleteCacheSetValue(userTokenKey, jti);
            log.error("event=front_token_store_failed userId={}", user.getId(), ex);
            throw ex;
        }
    }

    /**
     * Parse, validate and resolve a token exactly once.
     */
    public FrontPrincipal authenticate(String token) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            String jti = claims.getId();
            Number uidClaim = claims.get("uid", Number.class);

            if (jti != null && uidClaim != null) {
                Long userId = uidClaim.longValue();
                String storedIdentity = redisCache.getCacheObject(TOKEN_KEY_PREFIX + jti);
                if (!identityMatches(storedIdentity, userId, username)) {
                    log.warn("event=front_token_rejected reason=not_in_session_store");
                    return null;
                }

                OrderUser user = userMapper.selectAuthUserById(userId);
                if (!isActive(user) || !username.equals(user.getUsername())) {
                    invalidateAllTokens(userId, username);
                    log.warn("event=front_token_rejected reason=inactive_account userId={}", userId);
                    return null;
                }
                return new FrontPrincipal(userId, username, jti, false);
            }

            // Legacy compatibility: old keys contain the complete token.
            if (!Boolean.TRUE.equals(redisCache.hasKey(LEGACY_TOKEN_KEY_PREFIX + token))) {
                log.warn("event=front_token_rejected reason=legacy_not_in_session_store");
                return null;
            }
            OrderUser user = userMapper.selectAuthUserByName(username);
            if (!isActive(user)) {
                if (user != null) {
                    invalidateAllTokens(user.getId(), username);
                }
                return null;
            }
            return new FrontPrincipal(user.getId(), username, null, true);
        } catch (Exception ex) {
            log.warn("event=front_token_rejected reason=invalid_or_expired");
            return null;
        }
    }

    public boolean validateToken(String token) {
        return authenticate(token) != null;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public long getExpirationTimeFromToken(String token) {
        try {
            return parseClaims(token).getExpiration().getTime();
        } catch (Exception ex) {
            return 0L;
        }
    }

    public void invalidateTokenIfPresent(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = parseClaims(token);
            String jti = claims.getId();
            Number uidClaim = claims.get("uid", Number.class);
            if (jti != null && uidClaim != null) {
                Long userId = uidClaim.longValue();
                redisCache.deleteObject(TOKEN_KEY_PREFIX + jti);
                redisCache.deleteCacheSetValue(USER_TOKEN_KEY_PREFIX + userId, jti);
                return;
            }

            String username = claims.getSubject();
            redisCache.deleteObject(LEGACY_TOKEN_KEY_PREFIX + token);
            redisCache.deleteCacheSetValue(LEGACY_USER_TOKEN_KEY_PREFIX + username, token);
        } catch (Exception ex) {
            // Logout is intentionally idempotent for missing, malformed and expired tokens.
            log.debug("event=front_logout_token_ignored reason=invalid_or_expired");
        }
    }

    public void invalidateToken(String token, String username) {
        invalidateTokenIfPresent(token);
    }

    public void invalidateAllTokens(Long userId, String username) {
        if (userId != null) {
            String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
            Set<String> tokenIds = redisCache.getCacheSet(userTokenKey);
            if (tokenIds != null) {
                for (String tokenId : tokenIds) {
                    redisCache.deleteObject(TOKEN_KEY_PREFIX + tokenId);
                }
            }
            redisCache.deleteObject(userTokenKey);
        }

        if (username != null) {
            String legacyUserKey = LEGACY_USER_TOKEN_KEY_PREFIX + username;
            Set<String> legacyTokens = redisCache.getCacheSet(legacyUserKey);
            if (legacyTokens != null) {
                for (String legacyToken : legacyTokens) {
                    redisCache.deleteObject(LEGACY_TOKEN_KEY_PREFIX + legacyToken);
                }
            }
            redisCache.deleteObject(legacyUserKey);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean identityMatches(String storedIdentity, Long userId, String username) {
        return storedIdentity != null
                && storedIdentity.equals(userId + ":" + username);
    }

    private boolean isActive(OrderUser user) {
        return user != null && !"1".equals(user.getAccountStatus());
    }

    public record FrontPrincipal(Long userId, String username, String jti, boolean legacy) {
    }
}
