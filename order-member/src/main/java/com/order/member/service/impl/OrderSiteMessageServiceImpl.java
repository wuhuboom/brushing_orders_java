package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.member.mapper.OrderSiteMessageMapper;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.service.IOrderSiteMessageService;

/**
 * 站内信Service业务层处理
 * 
 * @author order
 * @date 2025-11-10
 */
@Service
@Transactional
public class OrderSiteMessageServiceImpl implements IOrderSiteMessageService 
{
    private final OrderSiteMessageMapper orderSiteMessageMapper;
    private final ITranslationsService translationsService;

    public OrderSiteMessageServiceImpl(
            OrderSiteMessageMapper orderSiteMessageMapper,
            ITranslationsService translationsService)
    {
        this.orderSiteMessageMapper = orderSiteMessageMapper;
        this.translationsService = translationsService;
    }

    /**
     * 查询站内信
     * 
     * @param id 站内信主键
     * @return 站内信
     */
    @Override
    public OrderSiteMessage selectOrderSiteMessageById(Long id)
    {
        return attachTranslations(orderSiteMessageMapper.selectOrderSiteMessageById(id));
    }

    /**
     * 查询站内信列表
     * 
     * @param orderSiteMessage 站内信
     * @return 站内信
     */
    @Override
    public List<OrderSiteMessage> selectOrderSiteMessageList(OrderSiteMessage orderSiteMessage)
    {
        return orderSiteMessageMapper.selectOrderSiteMessageList(orderSiteMessage);
    }

    /**
     * 新增站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    @Override
    public int insertOrderSiteMessage(OrderSiteMessage orderSiteMessage)
    {
        if (orderSiteMessage.getIsEnabled() == null) {
            orderSiteMessage.setIsEnabled(1);
        }
        normalizeMemberList(orderSiteMessage);
        orderSiteMessage.setCreateTime(DateUtils.getNowDate());
        saveTranslations(orderSiteMessage);
        return orderSiteMessageMapper.insertOrderSiteMessage(orderSiteMessage);
    }

    /**
     * 修改站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    @Override
    public int updateOrderSiteMessage(OrderSiteMessage orderSiteMessage)
    {
        normalizeMemberList(orderSiteMessage);
        saveTranslations(orderSiteMessage);
        return orderSiteMessageMapper.updateOrderSiteMessage(orderSiteMessage);
    }

    /**
     * 批量删除站内信
     * 
     * @param ids 需要删除的站内信主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteMessageByIds(Long[] ids)
    {
        return orderSiteMessageMapper.deleteOrderSiteMessageByIds(ids);
    }

    /**
     * 删除站内信信息
     * 
     * @param id 站内信主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteMessageById(Long id)
    {
        return orderSiteMessageMapper.deleteOrderSiteMessageById(id);
    }

    private void normalizeMemberList(OrderSiteMessage message)
    {
        if (message.getMemberList() != null) {
            String normalized = String.join(",",
                    java.util.Arrays.stream(message.getMemberList().split(","))
                            .map(String::trim)
                            .filter(value -> value.matches("\\d+"))
                            .distinct()
                            .toList());
            message.setMemberList(normalized);
        }
    }

    private void saveTranslations(OrderSiteMessage message)
    {
        Translations translations = message.getTranslations();
        if (translations == null || (translations.getId() == null && !translations.hasAnyValue())) {
            return;
        }
        if (translations.getId() == null) {
            translationsService.insertTranslations(translations);
        } else {
            translationsService.updateTranslations(translations);
        }
        message.setTranslationsId(translations.getId());
    }

    private OrderSiteMessage attachTranslations(OrderSiteMessage message)
    {
        if (message != null && message.getTranslationsId() != null) {
            message.setTranslations(
                    translationsService.selectTranslationsById(message.getTranslationsId()));
        }
        return message;
    }
}
