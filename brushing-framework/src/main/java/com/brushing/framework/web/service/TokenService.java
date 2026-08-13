package com.brushing.framework.web.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.brushing.common.constant.CacheConstants;
import com.brushing.common.constant.Constants;
import com.brushing.common.core.domain.model.LoginUser;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.utils.ServletUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.ip.AddressUtils;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.common.utils.uuid.IdUtils;
import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * token验证处理
 * 
 * @author brushing
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    /**
     * 原子切换用户当前会话，并返回之前的 token UUID。
     */
    private static final DefaultRedisScript<String> SWITCH_LOGIN_SESSION_SCRIPT = new DefaultRedisScript<>(
            "local oldToken = redis.call('get', KEYS[1]); "
                    + "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]); "
                    + "return oldToken;",
            String.class);

    /**
     * 仅当映射仍指向指定 token 时才删除，避免旧设备退出时误删新设备会话。
     */
    private static final DefaultRedisScript<Long> DELETE_LOGIN_SESSION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]); "
                    + "end; return 0;",
            Long.class);

    /**
     * 仅刷新当前会话映射的有效期，旧设备不能延长自己的会话。
     */
    private static final DefaultRedisScript<Long> REFRESH_LOGIN_SESSION_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('pexpire', KEYS[1], ARGV[2]); "
                    + "end; return 0;",
            Long.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取用户身份信息
     * 
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                if (StringUtils.isNotNull(user) && !isCurrentSession(user))
                {
                    // 已被后登录设备替换的旧会话立即失效，同时清理残留缓存。
                    redisCache.deleteObject(userKey);
                    return null;
                }
                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            LoginUser loginUser = redisCache.getCacheObject(userKey);
            redisCache.deleteObject(userKey);
            if (StringUtils.isNotNull(loginUser) && StringUtils.isNotNull(loginUser.getUserId()))
            {
                deleteCurrentSession(loginUser.getUserId(), token);
            }
        }
    }

    /**
     * 创建令牌
     * 
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        String jwt = createToken(claims);

        // 先准备新会话，再原子替换当前会话映射；映射切换后旧 token 即刻失效。
        storeLoginUser(loginUser);
        String oldToken;
        try
        {
            oldToken = switchCurrentSession(loginUser.getUserId(), token);
        }
        catch (RuntimeException e)
        {
            redisCache.deleteObject(getTokenKey(token));
            throw e;
        }

        if (StringUtils.isNotEmpty(oldToken) && !token.equals(oldToken))
        {
            try
            {
                redisCache.deleteObject(getTokenKey(oldToken));
                log.info("用户'{}'在新设备登录，旧会话已失效", loginUser.getUsername());
            }
            catch (Exception e)
            {
                // 当前会话映射已经切换，旧 token 即使暂未清理也无法通过鉴权。
                log.warn("清理用户'{}'的旧会话缓存失败: {}", loginUser.getUsername(), e.getMessage());
            }
        }
        return jwt;
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     * 
     * @param loginUser 登录信息
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     * 
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        if (!isCurrentSession(loginUser))
        {
            return;
        }
        storeLoginUser(loginUser);
        refreshCurrentSession(loginUser.getUserId(), loginUser.getToken());
    }

    private void storeLoginUser(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 校验 token 是否仍是该用户的当前会话。
     */
    private boolean isCurrentSession(LoginUser loginUser)
    {
        if (StringUtils.isNull(loginUser) || StringUtils.isNull(loginUser.getUserId())
                || StringUtils.isEmpty(loginUser.getToken()))
        {
            return false;
        }

        String sessionKey = getUserSessionKey(loginUser.getUserId());
        String currentToken = stringRedisTemplate.opsForValue().get(sessionKey);
        return loginUser.getToken().equals(currentToken);
    }

    private String switchCurrentSession(Long userId, String token)
    {
        return stringRedisTemplate.execute(
                SWITCH_LOGIN_SESSION_SCRIPT,
                Collections.singletonList(getUserSessionKey(userId)),
                token,
                String.valueOf(TimeUnit.MINUTES.toMillis(expireTime)));
    }

    private void deleteCurrentSession(Long userId, String token)
    {
        stringRedisTemplate.execute(
                DELETE_LOGIN_SESSION_SCRIPT,
                Collections.singletonList(getUserSessionKey(userId)),
                token);
    }

    private void refreshCurrentSession(Long userId, String token)
    {
        stringRedisTemplate.execute(
                REFRESH_LOGIN_SESSION_SCRIPT,
                Collections.singletonList(getUserSessionKey(userId)),
                token,
                String.valueOf(TimeUnit.MINUTES.toMillis(expireTime)));
    }

    /**
     * 设置用户代理信息
     * 
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser)
    {
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(userAgent.getBrowser().getName());
        loginUser.setOs(userAgent.getOperatingSystem().getName());
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    private String getUserSessionKey(Long userId)
    {
        return CacheConstants.LOGIN_USER_SESSION_KEY + userId;
    }
}
