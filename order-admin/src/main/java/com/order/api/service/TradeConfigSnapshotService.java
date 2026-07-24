package com.order.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TradeConfigSnapshotService {
    public static final String CACHE_NAME = "tradeConfigSnapshot";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() { };

    private final IOrderConfigService configService;
    private final ISysTimeZoneService timeZoneService;
    private final ObjectMapper objectMapper;

    public TradeConfigSnapshotService(
            IOrderConfigService configService,
            ISysTimeZoneService timeZoneService,
            ObjectMapper objectMapper) {
        this.configService = configService;
        this.timeZoneService = timeZoneService;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = CACHE_NAME, key = "'active'")
    @Transactional(readOnly = true)
    public TradeConfigSnapshot snapshot() {
        OrderConfig config = configService.selectOrderConfigByType("trade");
        if (config == null || config.getContent() == null || config.getContent().isBlank()) {
            throw new SnapshotException(Reason.UNAVAILABLE, "Trade configuration is unavailable");
        }
        final Map<String, Object> values;
        try {
            values = Collections.unmodifiableMap(
                    new LinkedHashMap<>(objectMapper.readValue(config.getContent(), MAP_TYPE)));
        } catch (Exception exception) {
            throw new SnapshotException(Reason.INVALID, "Trade configuration is invalid", exception);
        }

        SysTimeZone active = timeZoneService.getActive();
        if (active == null || active.getTzName() == null || active.getTzName().isBlank()) {
            throw new SnapshotException(Reason.UNAVAILABLE, "Active timezone is not configured");
        }
        try {
            ZoneId.of(active.getTzName());
        } catch (RuntimeException exception) {
            throw new SnapshotException(Reason.INVALID, "Active timezone is invalid", exception);
        }
        return new TradeConfigSnapshot(new LinkedHashMap<>(values), active.getTzName());
    }

    public enum Reason {
        UNAVAILABLE,
        INVALID
    }

    public static final class SnapshotException extends RuntimeException {
        private final Reason reason;

        public SnapshotException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public SnapshotException(Reason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }

    public record TradeConfigSnapshot(
            Map<String, Object> values,
            String timeZone) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public TradeConfigSnapshot {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        public ZoneId zoneId() {
            return ZoneId.of(timeZone);
        }
    }
}
