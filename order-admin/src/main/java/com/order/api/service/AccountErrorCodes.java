package com.order.api.service;

/**
 * Stable business codes retained for legacy clients while REST clients use HTTP status codes.
 */
public final class AccountErrorCodes {
    private AccountErrorCodes() {
    }

    public static final int OUTSIDE_WITHDRAWAL_WINDOW = 501;
    public static final int TRADE_PASSWORD = 504;
    public static final int TASK_REQUIREMENT = 505;
    public static final int PENDING_WITHDRAWAL = 506;
    public static final int BALANCE = 507;
    public static final int USER_NOT_FOUND = 509;
    public static final int MEMBER_LEVEL = 510;
    public static final int WITHDRAWAL_NOT_FOUND = 511;
    public static final int WITHDRAWAL_ACCOUNT = 513;
    public static final int WITHDRAWAL_DISABLED = 514;
    public static final int INVALID_REQUEST = 515;
    public static final int AMOUNT_BELOW_MINIMUM = 516;
    public static final int AMOUNT_ABOVE_MAXIMUM = 517;
    public static final int ACCOUNT_MUTATION_CONFLICT = 518;
    public static final int PLATFORM_DAILY_LIMIT = 519;
    public static final int REVIEW_CONFLICT = 520;
    public static final int CREDIT_SCORE = 521;
    public static final int LEVEL_MINIMUM_BALANCE = 522;
    public static final int DAILY_WITHDRAWAL_COUNT = 523;
    public static final int DAILY_WITHDRAWAL_AMOUNT = 524;
    public static final int IDEMPOTENCY_CONFLICT = 525;
    public static final int WITHDRAWAL_ACCOUNT_ACCESS = 526;
}
