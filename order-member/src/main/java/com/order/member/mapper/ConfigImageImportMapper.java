package com.order.member.mapper;

import com.order.member.domain.OrderConfig;

/** Current, locking reads used only by transactional ZIP image imports. */
public interface ConfigImageImportMapper {
    OrderConfig selectByTypeForUpdate(String type);
}
