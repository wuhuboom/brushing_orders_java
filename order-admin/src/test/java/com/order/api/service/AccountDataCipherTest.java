package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.GoodsWithdrawalAccount;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountDataCipherTest {
    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptedOnlyRoundTripsAndClearsPlaintext() {
        AccountDataCipher cipher = new AccountDataCipher(
                "ENCRYPTED_ONLY", "v1", "v1:" + KEY, new ObjectMapper());
        GoodsWithdrawalAccount account = account();

        cipher.protect(account);

        assertNotNull(account.getBankAccountEncrypted());
        assertNotEquals("6222021234567890", account.getBankAccountEncrypted());
        assertEquals("****7890", account.getBankAccountMask());
        assertEquals(null, account.getBankAccount());

        cipher.reveal(account);
        assertEquals("6222021234567890", account.getBankAccount());
        assertEquals("Alice", account.getAccountHolder());
        assertEquals("0x1234567890abcdef", account.getWalletAddress());
    }

    @Test
    void gcmRejectsTamperedCiphertext() {
        AccountDataCipher cipher = new AccountDataCipher(
                "DUAL_WRITE", "v1", "v1:" + KEY, new ObjectMapper());
        GoodsWithdrawalAccount account = account();
        cipher.protect(account);
        String encrypted = account.getBankAccountEncrypted();
        String[] parts = encrypted.split("\\.", 3);
        byte[] payload = Base64.getUrlDecoder().decode(parts[2]);
        payload[payload.length / 2] ^= 1;
        account.setBankAccountEncrypted(parts[0] + "." + parts[1] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload));

        assertThrows(IllegalStateException.class, () -> cipher.reveal(account));
    }

    @Test
    void encryptionModeRequiresConfiguredKey() {
        assertThrows(IllegalStateException.class,
                () -> new AccountDataCipher("DUAL_WRITE", "v1", "", new ObjectMapper()));
    }

    @Test
    void productionRejectsLegacyPlaintextMode() {
        assertThrows(IllegalStateException.class,
                () -> new AccountDataCipher(
                        "LEGACY_READ", "v1", "", new ObjectMapper(), true));
    }

    @Test
    void withdrawalSnapshotKeepsAttachment() {
        AccountDataCipher cipher = new AccountDataCipher(
                "DUAL_WRITE", "v1", "v1:" + KEY, new ObjectMapper());
        GoodsWithdrawalAccount account = account();
        account.setAttachment("/profile/upload/wallet-proof.png");

        String encrypted = cipher.encryptSnapshot(account);

        assertEquals(
                "/profile/upload/wallet-proof.png",
                cipher.decryptSnapshot(account.getUserId(), encrypted).get("attachment"));
    }

    private GoodsWithdrawalAccount account() {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setUserId(7L);
        account.setType("0");
        account.setBankAccount("6222021234567890");
        account.setAccountHolder("Alice");
        account.setAccountName("Primary");
        account.setWalletAddress("0x1234567890abcdef");
        return account;
    }
}
