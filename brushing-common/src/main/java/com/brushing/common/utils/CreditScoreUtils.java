package com.brushing.common.utils;

import java.math.BigDecimal;

/**
 * Shared validation and comparison rules for member credit scores.
 */
public final class CreditScoreUtils {

    public static final long MIN_SCORE = 1L;
    public static final long MAX_SCORE = 100L;

    private CreditScoreUtils() {
    }

    public static boolean isValidScore(Long creditScore) {
        return creditScore != null
                && creditScore >= MIN_SCORE
                && creditScore <= MAX_SCORE;
    }

    public static boolean isValidMinimum(BigDecimal minimumCreditScore) {
        if (minimumCreditScore == null
                || minimumCreditScore.compareTo(BigDecimal.valueOf(MIN_SCORE)) < 0
                || minimumCreditScore.compareTo(BigDecimal.valueOf(MAX_SCORE)) > 0) {
            return false;
        }
        return minimumCreditScore.stripTrailingZeros().scale() <= 0;
    }

    public static boolean meetsMinimum(Long creditScore, BigDecimal minimumCreditScore) {
        if (minimumCreditScore == null) {
            return true;
        }
        return creditScore != null
                && BigDecimal.valueOf(creditScore).compareTo(minimumCreditScore) >= 0;
    }
}
