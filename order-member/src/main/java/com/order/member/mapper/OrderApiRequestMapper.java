package com.order.member.mapper;

import com.order.member.domain.OrderApiRequest;
import org.apache.ibatis.annotations.Param;

public interface OrderApiRequestMapper {
    OrderApiRequest selectByRequestKey(
            @Param("userId") Long userId,
            @Param("operationType") String operationType,
            @Param("requestId") String requestId);

    int insertOrderApiRequest(OrderApiRequest request);
}
