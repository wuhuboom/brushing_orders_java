package com.order.api.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Formats public API dates using the active business timezone while keeping the
 * original instant available in a sibling Unix millisecond field.
 */
@RestControllerAdvice(basePackages = "com.order.api.controller")
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ApiDateTimeResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final String DATE_CONTEXT = ApiDateTimeResponseAdvice.class.getName();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    private final ObjectMapper apiMapper;
    private final ISysTimeZoneService timeZoneService;

    public ApiDateTimeResponseAdvice(ObjectMapper objectMapper, ISysTimeZoneService timeZoneService) {
        this.timeZoneService = timeZoneService;
        SimpleModule module = new SimpleModule("public-api-date-times");
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(
                    SerializationConfig config, BeanDescription bean, List<BeanPropertyWriter> properties) {
                properties.replaceAll(property -> Date.class.isAssignableFrom(property.getType().getRawClass())
                        ? new DatePropertyWriter(property) : property);
                return properties;
            }
        });
        // Keep the application's mapper and management responses unchanged.
        apiMapper = objectMapper.copy().registerModule(module);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || !request.getURI().getPath().startsWith("/api/")) {
            return body;
        }
        try (TokenBuffer buffer = new TokenBuffer(apiMapper, false)) {
            apiMapper.writer()
                    .withAttribute(DATE_CONTEXT, new DateContext(timeZoneService))
                    .writeValue(buffer, body);
            try (JsonParser parser = buffer.asParser()) {
                return apiMapper.readTree(parser);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to format API response dates", exception);
        }
    }

    private static final class DateContext {
        private final ISysTimeZoneService timeZoneService;
        private ZoneId zone;

        private DateContext(ISysTimeZoneService timeZoneService) {
            this.timeZoneService = timeZoneService;
        }

        private ZoneId zone() {
            if (zone == null) {
                SysTimeZone active = timeZoneService.getActive();
                if (active == null || active.getTzName() == null || active.getTzName().isBlank()) {
                    throw new IllegalStateException("Active timezone is not configured");
                }
                zone = ZoneId.of(active.getTzName());
            }
            return zone;
        }
    }

    private static final class DatePropertyWriter extends BeanPropertyWriter {
        private final String timestampName;

        private DatePropertyWriter(BeanPropertyWriter original) {
            super(original);
            timestampName = getName() + "Timestamp";
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator generator, SerializerProvider provider) throws Exception {
            Date value = (Date) get(bean);
            if (value == null) {
                generator.writeNullField(getName());
                generator.writeNullField(timestampName);
                return;
            }
            DateContext context = (DateContext) provider.getAttribute(DATE_CONTEXT);
            generator.writeStringField(getName(), FORMAT.format(Instant.ofEpochMilli(value.getTime()).atZone(context.zone())));
            generator.writeNumberField(timestampName, value.getTime());
        }
    }
}
