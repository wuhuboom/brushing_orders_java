package com.order.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 简单的 AES/CBC/PKCS5 加密工具
 * - 密钥来源优先级：环境变量 GOOGLE_AUTH_AES_KEY -> 系统属性 google.auth.aes.key -> 内置默认（仅开发使用，生产请设置环境变量）
 * - 加密结果格式：Base64( iv(16) || cipherBytes )
 */
public class CryptoUtils {
    private static final String ENV_KEY = "GOOGLE_AUTH_AES_KEY";
    private static final String SYS_PROP = "google.auth.aes.key";
    private static final String DEFAULT_KEY = "change_me_release"; // 开发 fallback，生产请覆盖
    private static final SecureRandom RANDOM = new SecureRandom();

    private static byte[] getAesKeyBytes() {
        try {
            String key = System.getenv(ENV_KEY);
            if (key == null || key.isEmpty()) {
                key = System.getProperty(SYS_PROP);
            }
            if (key == null || key.isEmpty()) {
                key = DEFAULT_KEY;
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(key.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 16); // 128-bit key
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive AES key", e);
        }
    }

    public static String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] keyBytes = getAesKeyBytes();
            byte[] iv = new byte[16];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherB64) {
        if (cipherB64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherB64);
            if (all.length < 16) return null;
            byte[] iv = Arrays.copyOfRange(all, 0, 16);
            byte[] cipherBytes = Arrays.copyOfRange(all, 16, all.length);
            byte[] keyBytes = getAesKeyBytes();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }
}

