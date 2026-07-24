package com.order.web.controller.advice;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.SecurityUtils;
import com.order.system.service.ISystemAlignmentService;

/** Applies role-based phone masking consistently to JSON list/detail responses. */
@ControllerAdvice
public class PhoneMaskingResponseBodyAdvice implements ResponseBodyAdvice<Object>
{
    private static final Set<String> PHONE_FIELDS = Set.of("phone", "phonenumber", "mobile", "mobilephone");

    @Autowired
    private ISystemAlignmentService alignmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType)
    {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response)
    {
        if (!(body instanceof AjaxResult) && !(body instanceof TableDataInfo)) return body;
        Long userId;
        try { userId = SecurityUtils.getUserId(); }
        catch (Exception ignored) { return body; }
        if (!alignmentService.shouldHidePhone(userId)) return body;
        JsonNode tree = objectMapper.valueToTree(body);
        mask(tree);
        return tree;
    }

    private void mask(JsonNode node)
    {
        if (node == null) return;
        if (node.isObject())
        {
            ObjectNode object = (ObjectNode) node;
            object.fields().forEachRemaining(entry -> {
                if (PHONE_FIELDS.contains(entry.getKey().toLowerCase()) && entry.getValue().isTextual())
                {
                    object.set(entry.getKey(), TextNode.valueOf(maskValue(entry.getValue().asText())));
                }
                else mask(entry.getValue());
            });
        }
        else if (node.isArray()) node.forEach(this::mask);
    }

    private String maskValue(String value)
    {
        if (value == null || value.length() < 7) return value;
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
