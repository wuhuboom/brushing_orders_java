package com.order.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.GoodsWithdrawalAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AccountDataCipher {
    private static final Logger log = LoggerFactory.getLogger(AccountDataCipher.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;

    public enum Mode {
        LEGACY_READ,
        DUAL_WRITE,
        ENCRYPTED_ONLY
    }

    private final Mode mode;
    private final String activeKeyId;
    private final Map<String, SecretKeySpec> keys;
    private final ObjectMapper objectMapper;

    @Autowired
    public AccountDataCipher(
            @Value("${account-security.mode:LEGACY_READ}") String mode,
            @Value("${account-security.active-key-id:v1}") String activeKeyId,
            @Value("${account-security.keys:}") String configuredKeys,
            ObjectMapper objectMapper,
            Environment environment) {
        this(mode, activeKeyId, configuredKeys, objectMapper, isProduction(environment));
    }

    AccountDataCipher(
            String mode,
            String activeKeyId,
            String configuredKeys,
            ObjectMapper objectMapper) {
        this(mode, activeKeyId, configuredKeys, objectMapper, false);
    }

    AccountDataCipher(
            String mode,
            String activeKeyId,
            String configuredKeys,
            ObjectMapper objectMapper,
            boolean production) {
        this.mode = Mode.valueOf(mode.trim().toUpperCase());
        this.activeKeyId = activeKeyId;
        this.keys = parseKeys(configuredKeys);
        this.objectMapper = objectMapper;
        if (production && this.mode == Mode.LEGACY_READ) {
            throw new IllegalStateException(
                    "Production requires DUAL_WRITE or ENCRYPTED_ONLY account encryption mode");
        }
        if (this.mode != Mode.LEGACY_READ && !this.keys.containsKey(activeKeyId)) {
            throw new IllegalStateException("ACCOUNT_DATA_ENCRYPTION_KEYS must contain the active AES-256 key");
        }
        if (this.mode == Mode.LEGACY_READ) {
            log.warn("event=account_encryption_legacy_read mode=LEGACY_READ");
        }
    }

    public Mode mode() {
        return mode;
    }

    public void protect(GoodsWithdrawalAccount account) {
        account.setBankAccountMask(maskEnding(account.getBankAccount(), 4));
        account.setWalletAddressMask(maskWallet(account.getWalletAddress()));
        account.setAccountHolderMask(maskName(account.getAccountHolder()));
        account.setAccountNameMask(maskName(account.getAccountName()));
        if (mode == Mode.LEGACY_READ) {
            return;
        }
        Long userId = account.getUserId();
        account.setBankAccountEncrypted(encrypt(account.getBankAccount(), aad(userId, "bankAccount")));
        account.setAccountHolderEncrypted(encrypt(account.getAccountHolder(), aad(userId, "accountHolder")));
        account.setAccountNameEncrypted(encrypt(account.getAccountName(), aad(userId, "accountName")));
        account.setWalletAddressEncrypted(encrypt(account.getWalletAddress(), aad(userId, "walletAddress")));
        if (mode == Mode.ENCRYPTED_ONLY) {
            account.setBankAccount(null);
            account.setAccountHolder(null);
            account.setAccountName(null);
            account.setWalletAddress(null);
        }
    }

    public GoodsWithdrawalAccount reveal(GoodsWithdrawalAccount account) {
        if (account == null) {
            return null;
        }
        Long userId = account.getUserId();
        account.setBankAccount(decryptOrLegacy(account.getBankAccountEncrypted(), account.getBankAccount(), aad(userId, "bankAccount")));
        account.setAccountHolder(decryptOrLegacy(account.getAccountHolderEncrypted(), account.getAccountHolder(), aad(userId, "accountHolder")));
        account.setAccountName(decryptOrLegacy(account.getAccountNameEncrypted(), account.getAccountName(), aad(userId, "accountName")));
        account.setWalletAddress(decryptOrLegacy(account.getWalletAddressEncrypted(), account.getWalletAddress(), aad(userId, "walletAddress")));
        return account;
    }

    public String encryptSnapshot(GoodsWithdrawalAccount account) {
        if (mode == Mode.LEGACY_READ) {
            return null;
        }
        GoodsWithdrawalAccount revealed = reveal(account);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("type", revealed.getType());
        snapshot.put("withdrawalTypeId", revealed.getWithdrawalTypeId());
        snapshot.put("withdrawalType", revealed.getWithdrawalType());
        snapshot.put("bankName", revealed.getBankName());
        snapshot.put("depositType", revealed.getDepositType());
        snapshot.put("branchCode", revealed.getBranchCode());
        snapshot.put("branchName", revealed.getBranchName());
        snapshot.put("bankAccount", revealed.getBankAccount());
        snapshot.put("accountHolder", revealed.getAccountHolder());
        snapshot.put("accountName", revealed.getAccountName());
        snapshot.put("walletName", revealed.getWalletName());
        snapshot.put("walletAddress", revealed.getWalletAddress());
        try {
            return encrypt(objectMapper.writeValueAsString(snapshot), aad(account.getUserId(), "withdrawalSnapshot"));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize withdrawal account snapshot", ex);
        }
    }

    public Map<String, Object> decryptSnapshot(Long userId, String encryptedSnapshot) {
        if (encryptedSnapshot == null || encryptedSnapshot.isBlank()) {
            return Map.of();
        }
        String json = decryptOrLegacy(
                encryptedSnapshot, null, aad(userId, "withdrawalSnapshot"));
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize withdrawal account snapshot", ex);
        }
    }

    public String displayMask(GoodsWithdrawalAccount account) {
        if (account == null) {
            return null;
        }
        if ("1".equals(account.getType())) {
            String mask = firstNonBlank(account.getWalletAddressMask(), maskWallet(account.getWalletAddress()));
            return firstNonBlank(account.getWalletName(), "Wallet") + " " + firstNonBlank(mask, "");
        }
        String mask = firstNonBlank(account.getBankAccountMask(), maskEnding(account.getBankAccount(), 4));
        return firstNonBlank(account.getBankName(), "Bank") + " " + firstNonBlank(mask, "");
    }

    private String encrypt(String plain, byte[] aad) {
        if (plain == null) {
            return null;
        }
        SecretKeySpec key = keys.get(activeKeyId);
        if (key == null) {
            throw new IllegalStateException("Account encryption key is not configured");
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return "v1." + activeKeyId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Account data encryption failed", ex);
        }
    }

    private String decryptOrLegacy(String encrypted, String legacy, byte[] aad) {
        if (encrypted == null || encrypted.isBlank()) {
            return legacy;
        }
        try {
            String[] parts = encrypted.split("\\.", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported encrypted value");
            }
            SecretKeySpec key = keys.get(parts[1]);
            if (key == null) {
                throw new IllegalStateException("Missing decryption key " + parts[1]);
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[2]);
            byte[] nonce = java.util.Arrays.copyOfRange(payload, 0, NONCE_BYTES);
            byte[] cipherText = java.util.Arrays.copyOfRange(payload, NONCE_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Account data decryption failed", ex);
        }
    }

    private Map<String, SecretKeySpec> parseKeys(String configured) {
        Map<String, SecretKeySpec> parsed = new LinkedHashMap<>();
        if (configured == null || configured.isBlank()) {
            return parsed;
        }
        for (String item : configured.split(";")) {
            String[] pair = item.trim().split(":", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Account encryption keys must use keyId:base64 format");
            }
            byte[] raw = Base64.getDecoder().decode(pair[1]);
            if (raw.length != 32) {
                throw new IllegalArgumentException("Account encryption key must contain exactly 32 bytes");
            }
            parsed.put(pair[0], new SecretKeySpec(raw, "AES"));
        }
        return parsed;
    }

    private byte[] aad(Long userId, String field) {
        return (String.valueOf(userId) + ":" + field).getBytes(StandardCharsets.UTF_8);
    }

    private String maskEnding(String value, int visible) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String compact = value.replaceAll("\\s+", "");
        int keep = Math.min(visible, compact.length());
        return "****" + compact.substring(compact.length() - keep);
    }

    private String maskWallet(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 10) {
            return maskEnding(value, 4);
        }
        return value.substring(0, 6) + "****" + value.substring(value.length() - 4);
    }

    private String maskName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1) + "***";
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static boolean isProduction(Environment environment) {
        return environment != null && Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }
}
