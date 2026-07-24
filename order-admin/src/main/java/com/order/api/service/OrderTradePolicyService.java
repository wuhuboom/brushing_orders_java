package com.order.api.service;

import com.order.web.controller.tool.TimeRangeChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.order.api.service.OrderErrorCodes.CONFIG_UNAVAILABLE;
import static com.order.api.service.OrderErrorCodes.INVALID_CONFIG;
import static com.order.api.service.OrderErrorCodes.OUTSIDE_TRADE_WINDOW;

@Service
public class OrderTradePolicyService {
    private final TradeConfigSnapshotService snapshotService;

    public OrderTradePolicyService(TradeConfigSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Transactional(readOnly = true)
    public TradePolicy activePolicy() {
        TradeConfigSnapshotService.TradeConfigSnapshot snapshot;
        try {
            snapshot = snapshotService.snapshot();
        } catch (TradeConfigSnapshotService.SnapshotException exception) {
            int code = exception.getReason() == TradeConfigSnapshotService.Reason.UNAVAILABLE
                    ? CONFIG_UNAVAILABLE : INVALID_CONFIG;
            throw OrderApiException.unavailable(code, exception.getMessage());
        }
        Map<String, Object> values = snapshot.values();
        Object tradeTimeRange = values.get("tradeTimeRange");
        try {
            if (!TimeRangeChecker.isCurrentTimeInRange(
                    Optional.ofNullable(tradeTimeRange), snapshot.timeZone())) {
                throw OrderApiException.forbidden(
                        OUTSIDE_TRADE_WINDOW,
                        "Not within the time frame for grabbing orders");
            }
        } catch (OrderApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid trade time configuration");
        }

        BigDecimal minimumBalance = requiredDecimal(values, "minTradeBalance");
        BigDecimal parentRebatePercentage = requiredDecimal(values, "parentRebatePercentage");
        PercentageRange matchRange = percentageRange(values.get("matchRangePercentage"));
        if (minimumBalance.compareTo(BigDecimal.ZERO) < 0
                || parentRebatePercentage.compareTo(BigDecimal.ZERO) < 0
                || parentRebatePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid trade configuration");
        }
        return new TradePolicy(minimumBalance, parentRebatePercentage, matchRange);
    }

    private BigDecimal requiredDecimal(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            throw OrderApiException.unavailable(CONFIG_UNAVAILABLE, "System configuration is not available");
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid trade configuration");
        }
    }

    private PercentageRange percentageRange(Object value) {
        if (value == null) {
            throw OrderApiException.unavailable(CONFIG_UNAVAILABLE, "System configuration is not available");
        }
        try {
            if (value instanceof Number) {
                BigDecimal fixed = new BigDecimal(String.valueOf(value));
                return checkedRange(fixed, fixed);
            }
            String text = String.valueOf(value).trim();
            int separator = text.indexOf('-', 1);
            if (separator < 0) {
                BigDecimal fixed = new BigDecimal(text);
                return checkedRange(fixed, fixed);
            }
            BigDecimal minimum = new BigDecimal(text.substring(0, separator).trim());
            BigDecimal maximum = new BigDecimal(text.substring(separator + 1).trim());
            return checkedRange(minimum, maximum);
        } catch (NumberFormatException exception) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid match range configuration");
        }
    }

    private PercentageRange checkedRange(BigDecimal minimum, BigDecimal maximum) {
        if (minimum.compareTo(BigDecimal.ZERO) <= 0
                || minimum.compareTo(maximum) > 0
                || maximum.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid match range configuration");
        }
        return new PercentageRange(minimum, maximum);
    }

    public record TradePolicy(
            BigDecimal minimumBalance,
            BigDecimal parentRebatePercentage,
            PercentageRange matchRange) {
    }

    public record PercentageRange(BigDecimal minimum, BigDecimal maximum) {
    }
}
