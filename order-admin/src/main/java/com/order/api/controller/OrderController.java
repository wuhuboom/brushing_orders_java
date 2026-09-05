package com.order.api.controller;

import com.order.api.controller.dto.OrderApiDtos.CreationResult;
import com.order.api.service.OrderApplicationService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "创建订单或返回待领取彩金")
    public AjaxResult create(
            @RequestAttribute("userId") Long userId,
            @RequestHeader("Idempotency-Key") String requestId) {
        return creationResponse(orderService.create(userId, requestId));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交订单")
    public AjaxResult submit(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return AjaxResult.success("Success", orderService.submit(userId, id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询当前用户订单详情")
    public AjaxResult order(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return AjaxResult.success("Success", orderService.order(userId, id));
    }

    @GetMapping
    @Operation(summary = "查询当前用户订单")
    public TableDataInfo orders(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return orderService.orders(userId, status, pageNum, pageSize);
    }

    private AjaxResult creationResponse(CreationResult result) {
        return AjaxResult.success("Success", result.data())
                .put("resultType", result.resultType().name());
    }
}
