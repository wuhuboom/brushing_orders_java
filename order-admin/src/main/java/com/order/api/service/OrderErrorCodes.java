package com.order.api.service;

/**
 * Stable member-order business codes. Existing values are retained for legacy clients.
 */
public final class OrderErrorCodes {
    private OrderErrorCodes() {
    }

    public static final int CONFIG_UNAVAILABLE = 901;
    public static final int OUTSIDE_TRADE_WINDOW = 902;
    public static final int USER_NOT_FOUND = 904;
    public static final int WORK_NOT_ALLOWED = 905;
    public static final int MINIMUM_BALANCE = 906;
    public static final int SYSTEM_BUSY = 908;
    public static final int INVALID_CONFIG = 910;
    public static final int NO_SUITABLE_PRODUCT = 911;
    public static final int INVALID_MEMBER_LEVEL = 912;
    public static final int INVALID_ORDER = 913;
    public static final int INVALID_USER = 914;
    public static final int INSUFFICIENT_BALANCE = 916;
    public static final int ORDER_COMPLETED = 917;
    public static final int ORDER_STATE_CONFLICT = 918;
    public static final int INVALID_BONUS = 921;
    public static final int BONUS_UNAVAILABLE = 922;
    public static final int INVALID_REQUEST = 923;
}
