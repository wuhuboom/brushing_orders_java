package com.brushing.common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditScoreUtilsTest {

    @Test
    void acceptsOnlyIntegerScoresFromOneToOneHundred() {
        assertTrue(CreditScoreUtils.isValidScore(1L));
        assertTrue(CreditScoreUtils.isValidScore(100L));
        assertFalse(CreditScoreUtils.isValidScore(null));
        assertFalse(CreditScoreUtils.isValidScore(0L));
        assertFalse(CreditScoreUtils.isValidScore(101L));

        assertTrue(CreditScoreUtils.isValidMinimum(new BigDecimal("1")));
        assertTrue(CreditScoreUtils.isValidMinimum(new BigDecimal("100.00")));
        assertFalse(CreditScoreUtils.isValidMinimum(null));
        assertFalse(CreditScoreUtils.isValidMinimum(new BigDecimal("1.5")));
        assertFalse(CreditScoreUtils.isValidMinimum(new BigDecimal("101")));
    }

    @Test
    void comparesMemberScoreWithConfiguredMinimum() {
        assertTrue(CreditScoreUtils.meetsMinimum(80L, new BigDecimal("80")));
        assertTrue(CreditScoreUtils.meetsMinimum(81L, new BigDecimal("80")));
        assertFalse(CreditScoreUtils.meetsMinimum(79L, new BigDecimal("80")));
        assertFalse(CreditScoreUtils.meetsMinimum(null, new BigDecimal("80")));
        assertTrue(CreditScoreUtils.meetsMinimum(null, null));
    }
}
