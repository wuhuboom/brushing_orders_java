package com.order.api.service;

public final class ApiPagination {
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private ApiPagination() {
    }

    public static int pageNumber(Integer value) {
        return Math.max(DEFAULT_PAGE_NUM, value == null ? DEFAULT_PAGE_NUM : value);
    }

    public static int pageSize(Integer value) {
        int resolved = value == null ? DEFAULT_PAGE_SIZE : value;
        return Math.max(1, Math.min(resolved, MAX_PAGE_SIZE));
    }
}
