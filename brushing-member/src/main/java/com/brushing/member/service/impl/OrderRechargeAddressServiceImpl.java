package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.brushing.member.mapper.OrderRechargeAddressMapper;
import com.brushing.member.domain.OrderRechargeAddress;
import com.brushing.member.service.IOrderRechargeAddressService;

/**
 * 充值地址Service业务层处理
 *
 * @author brushing
 * @date 2026-01-08
 */
@Service
public class OrderRechargeAddressServiceImpl implements IOrderRechargeAddressService
{
    @Autowired
    private OrderRechargeAddressMapper orderRechargeAddressMapper;

    /**
     * 查询充值地址
     *
     * @param id 充值地址主键
     * @return 充值地址
     */
    @Override
    public OrderRechargeAddress selectOrderRechargeAddressById(Long id)
    {
        return orderRechargeAddressMapper.selectOrderRechargeAddressById(id);
    }

    /**
     * 查询充值地址列表
     *
     * @param orderRechargeAddress 充值地址
     * @return 充值地址
     */
    @Override
    public List<OrderRechargeAddress> selectOrderRechargeAddressList(OrderRechargeAddress orderRechargeAddress)
    {
        return orderRechargeAddressMapper.selectOrderRechargeAddressList(orderRechargeAddress);
    }

    /**
     * 新增充值地址
     *
     * @param orderRechargeAddress 充值地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertOrderRechargeAddress(OrderRechargeAddress orderRechargeAddress)
    {
        orderRechargeAddress.setCreateTime(DateUtils.getNowDate());
        // 如果新增的记录 status 为 "0"，需要先将其他记录的 status 置为 '1'
        if ("0".equals(orderRechargeAddress.getStatus())) {
            orderRechargeAddressMapper.resetAllStatusToOne();
        }
        return orderRechargeAddressMapper.insertOrderRechargeAddress(orderRechargeAddress);
    }

    /**
     * 修改充值地址
     *
     * @param orderRechargeAddress 充值地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateOrderRechargeAddress(OrderRechargeAddress orderRechargeAddress)
    {
        // 如果更新的记录 status 为 "0"，需要将其他记录（排除当前记录）置为 '1'
        if ("0".equals(orderRechargeAddress.getStatus()) && orderRechargeAddress.getId() != null) {
            orderRechargeAddressMapper.resetOtherStatusToOne(orderRechargeAddress.getId());
        }
        return orderRechargeAddressMapper.updateOrderRechargeAddress(orderRechargeAddress);
    }

    /**
     * 批量删除充值地址
     *
     * @param ids 需要删除的充值地址主键
     * @return 结果
     */
    @Override
    public int deleteOrderRechargeAddressByIds(Long[] ids)
    {
        return orderRechargeAddressMapper.deleteOrderRechargeAddressByIds(ids);
    }

    /**
     * 删除充值地址信息
     *
     * @param id 充值地址主键
     * @return 结果
     */
    @Override
    public int deleteOrderRechargeAddressById(Long id)
    {
        return orderRechargeAddressMapper.deleteOrderRechargeAddressById(id);
    }
}
