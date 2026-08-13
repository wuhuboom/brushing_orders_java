package com.order.api.service;

import java.util.Map;

/**
 * Canonical English messages for the public {@code /api/**} response envelope.
 *
 * <p>The top-level {@code msg} is an integration field. It must be stable,
 * independent of persisted translations and must never expose exception or
 * database text. Localized, user-facing copy remains available from
 * {@code /api/config/error-messages} in the response {@code data} field.</p>
 */
public final class PublicApiMessageCatalog {
    private static final String SUCCESS = "Success";
    private static final String INVALID_REQUEST = "Invalid request";
    private static final String NOT_FOUND = "Resource not found";
    private static final String CONFLICT = "Request conflict";
    private static final String FORBIDDEN = "Operation not allowed";
    private static final String SERVER_ERROR = "Please try again later";

    private static final Map<Integer, String> MESSAGES = Map.ofEntries(
            Map.entry(200, SUCCESS),
            Map.entry(201, SUCCESS),
            Map.entry(400, INVALID_REQUEST),
            Map.entry(401, "Unauthorized"),
            Map.entry(403, FORBIDDEN),
            Map.entry(404, NOT_FOUND),
            Map.entry(405, "Method not allowed"),
            Map.entry(500, SERVER_ERROR),
            Map.entry(501, "Current time is outside the withdrawal window"),
            Map.entry(504, "Invalid trade password"),
            Map.entry(505, "Required tasks are not complete"),
            Map.entry(506, "A pending withdrawal already exists"),
            Map.entry(507, "Insufficient balance"),
            Map.entry(509, "User not found"),
            Map.entry(510, "Member level is not configured"),
            Map.entry(513, "Withdrawal account is unavailable"),
            Map.entry(514, "Withdrawal operation is disabled"),
            Map.entry(515, INVALID_REQUEST),
            Map.entry(516, "Withdrawal amount is below the minimum"),
            Map.entry(517, "Withdrawal amount exceeds the maximum"),
            Map.entry(518, "Withdrawal account is in use or was changed concurrently"),
            Map.entry(519, "Platform daily withdrawal limit has been reached"),
            Map.entry(521, "Credit score requirement is not met"),
            Map.entry(522, "Member level minimum balance would not be met"),
            Map.entry(523, "Daily withdrawal count has been reached"),
            Map.entry(524, "Daily withdrawal amount has been reached"),
            Map.entry(525, "Idempotency key was already used"),
            Map.entry(526, "Withdrawal account access token is invalid or expired"),
            Map.entry(601, "Invalid username or password"),
            Map.entry(602, "Current password is incorrect"),
            Map.entry(603, "Invalid username"),
            Map.entry(604, "Invalid password"),
            Map.entry(605, "Invalid trade password"),
            Map.entry(606, "Invalid phone number"),
            Map.entry(607, "Invalid gender"),
            Map.entry(608, "Invalid invite code"),
            Map.entry(609, "Username already exists"),
            Map.entry(610, "Invite code does not exist"),
            Map.entry(611, "Password is required"),
            Map.entry(612, "Invalid password length"),
            Map.entry(613, "Trade password is incorrect"),
            Map.entry(614, "Avatar is invalid"),
            Map.entry(617, INVALID_REQUEST),
            Map.entry(621, "Phone number already exists"),
            Map.entry(622, "Too many attempts"),
            Map.entry(701, "No data"),
            Map.entry(703, "Upload failed"),
            Map.entry(901, "Trading configuration is unavailable"),
            Map.entry(902, "Current time is outside the trading window"),
            Map.entry(904, "User not found"),
            Map.entry(905, "Account status does not allow orders"),
            Map.entry(906, "Balance is below the trading requirement"),
            Map.entry(908, "Please try again later"),
            Map.entry(910, "Invalid order configuration"),
            Map.entry(911, "No matching product is available"),
            Map.entry(912, "Invalid member level configuration"),
            Map.entry(913, "Order not found"),
            Map.entry(914, "Invalid user status"),
            Map.entry(916, "Insufficient balance"),
            Map.entry(918, "Order state conflict"),
            Map.entry(921, "Bonus not found"),
            Map.entry(922, "Bonus is unavailable"),
            Map.entry(923, INVALID_REQUEST));

    private PublicApiMessageCatalog() {
    }

    public static String message(int code) {
        String known = MESSAGES.get(code);
        if (known != null) {
            return known;
        }
        if (code >= 200 && code < 300) {
            return SUCCESS;
        }
        if (code == 400 || code == 422) {
            return INVALID_REQUEST;
        }
        if (code == 401) {
            return "Unauthorized";
        }
        if (code == 403) {
            return FORBIDDEN;
        }
        if (code == 404) {
            return NOT_FOUND;
        }
        if (code == 405) {
            return "Method not allowed";
        }
        if (code == 409) {
            return CONFLICT;
        }
        if (code >= 500 && code < 600) {
            return SERVER_ERROR;
        }
        return code > 0 ? "Request failed" : SERVER_ERROR;
    }
}
