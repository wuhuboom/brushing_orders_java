package com.order.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CryptoUtilsTest {

    @Test
    public void testEncryptDecrypt() {
        String secret = "MYSECRETVALUE123";
        String cipher = CryptoUtils.encrypt(secret);
        assertNotNull(cipher);
        assertNotEquals(secret, cipher);
        String plain = CryptoUtils.decrypt(cipher);
        assertEquals(secret, plain);
    }

    @Test
    public void testDecryptNull() {
        assertNull(CryptoUtils.decrypt(null));
    }
}

