package com.order.common.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.apache.commons.codec.binary.Base32;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

/**
 * Google Authenticator helper: 生成 secret、otpAuth URL、二维码 Base64、校验 TOTP
 */
public class GoogleAuthUtils {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base32 base32 = new Base32();

    // 生成长度为 16 的 Base32 secret（无 '=' 填充）
    public static String generateSecret() {
        byte[] bytes = new byte[10]; // 10 bytes -> 16 Base32 chars
        secureRandom.nextBytes(bytes);
        String secret = base32.encodeToString(bytes).replace("=", "");
        return secret;
    }

    // 生成 otpauth URL，供二维码扫描
    // account: 用户账号（例如 email 或 username）
    // issuer: 项目/公司名称显示在 Google Authenticator 上
    public static String getOtpAuthUrl(String secret, String account, String issuer) {
        String encodedIssuer = urlEncode(issuer);
        String encodedAccount = urlEncode(account);
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            encodedIssuer, encodedAccount, secret, encodedIssuer);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // 生成二维码的 Base64 PNG 字符串
    public static String generateQRCodeBase64(String otpAuthUrl, int width, int height) throws Exception {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUrl, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException we) {
            throw new Exception("Failed to generate QR code", we);
        }
    }

    // 计算 TOTP 并校验，允许一定窗口（默认 ±1 步）
    public static boolean validateTotp(String base32Secret, String code, int window) {
        if (base32Secret == null || code == null) return false;
        String normalized = code.trim();
        long timeIndex = Instant.now().getEpochSecond() / 30;
        for (int i = -window; i <= window; i++) {
            long idx = timeIndex + i;
            String candidate = generateTotp(base32Secret, idx);
            if (normalized.equals(candidate)) return true;
        }
        return false;
    }

    private static String generateTotp(String base32Secret, long timeIndex) {
        try {
            byte[] key = base32.decode(base32Secret);
            byte[] data = new byte[8];
            long value = timeIndex;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            javax.crypto.spec.SecretKeySpec signKey = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

