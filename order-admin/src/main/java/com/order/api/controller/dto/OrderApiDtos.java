package com.order.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderInfo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Stable public projections for the member order API.
 */
public final class OrderApiDtos {
    private OrderApiDtos() {
    }

    public enum ResultType {
        ORDER,
        BONUS
    }

    public record CreationResult(
            ResultType resultType,
            OrderResponse order,
            BonusResponse bonus) {

        public static CreationResult order(OrderInfo value) {
            return new CreationResult(ResultType.ORDER, OrderResponse.from(value), null);
        }

        public static CreationResult bonus(OrderBonusTable value) {
            return new CreationResult(ResultType.BONUS, null, BonusResponse.from(value));
        }

        public Object data() {
            return resultType == ResultType.ORDER ? order : bonus;
        }
    }

    public record OrderResponse(
            Long id,
            String orderNumber,
            String type,
            Long orderCount,
            BigDecimal amount,
            BigDecimal rebatePercentage,
            BigDecimal rebate,
            String status,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) Date expiryTime,
            Long productId,
            String productImage,
            String productTitle,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) Date createTime) {

        public static OrderResponse from(OrderInfo value) {
            if (value == null) {
                return null;
            }
            return new OrderResponse(
                    value.getId(),
                    value.getOrderNumber(),
                    value.getType(),
                    value.getOrderCount(),
                    value.getAmount(),
                    value.getRebatePercentage(),
                    value.getRebate(),
                    value.getStatus(),
                    value.getExpiryTime(),
                    value.getProductId(),
                    value.getProductImage(),
                    value.getProductTitle(),
                    value.getCreateTime());
        }
    }

    public record BonusResponse(
            Long id,
            Long orderNum,
            BigDecimal amount,
            Long animationDuration,
            Long displayDuration,
            String distributionType,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) Date expiryTime) {

        public static BonusResponse from(OrderBonusTable value) {
            if (value == null) {
                return null;
            }
            return new BonusResponse(
                    value.getId(),
                    value.getOrderNum(),
                    value.getAmount(),
                    value.getAnimationDuration(),
                    value.getDisplayDuration(),
                    value.getDistributionType(),
                    value.getExpiryTime());
        }
    }

    public record BonusClaimResponse(
            Long id,
            BigDecimal amount,
            BigDecimal balance,
            boolean alreadyClaimed) {
    }

    public record SubmitResponse(
            Long id,
            String status,
            boolean alreadyCompleted) {
    }
}
