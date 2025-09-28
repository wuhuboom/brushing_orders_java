package com.brushing.framework.web.service;

import com.brushing.system.mapper.SysUserMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthenticatorService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Autowired
    private SysUserMapper userMapper;

    // 生成TOTP密钥并返回二维码信息
    public String startSetup(Long userId, String accountName, String issuer) {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey(); // Base32 密钥

        // 将密钥存储到数据库
        userMapper.updateTotpSecret(userId, secret);

        // 生成二维码链接（otpauth URI）
        String label = accountName + "@" + issuer;
        String otpauthUri = String.format("otpauth://totp/%s?secret=%s&issuer=%s&digits=6&period=30&algorithm=SHA1",
                label, secret, issuer);

        return otpauthUri; // 返回二维码链接
    }

    // 验证用户输入的验证码
    public boolean verify(Long userId, int code) {
        String secret = userMapper.selectTotpSecretById(userId);
        if (secret == null) return false;
        return gAuth.authorize(secret, code);
    }

    // 启用 Google Authenticator
    public boolean enable(Long userId, int code) {
        if (!verify(userId, code)) {
            return false; // 验证码错误
        }
        // 更新用户状态，表示启用了 Google 验证器
        userMapper.enableTotp(userId, "0");
        return true;
    }

}
