package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderRechargeAddress;

/**
 * 充值地址Mapper接口
 *
 * @author brushing
 * @date 2026-01-08
 */
public interface OrderRechargeAddressMapper
{
    /**
     * 查询充值地址
     *
     * @param id 充值地址主键
     * @return 充值地址
     */
    public OrderRechargeAddress selectOrderRechargeAddressById(Long id);

    /**
     * 查询充值地址列表
     *
     * @param orderRechargeAddress 充值地址
     * @return 充值地址集合
     */
    public List<OrderRechargeAddress> selectOrderRechargeAddressList(OrderRechargeAddress orderRechargeAddress);

    /**
     * 新增充值地址
     *
     * @param orderRechargeAddress 充值地址
     * @return 结果
     */
    public int insertOrderRechargeAddress(OrderRechargeAddress orderRechargeAddress);

    /**
     * 修改充值地址
     *
     * @param orderRechargeAddress 充值地址
     * @return 结果
     */
    public int updateOrderRechargeAddress(OrderRechargeAddress orderRechargeAddress);

    // 新增：当设置某条为 status='0' 时，重置其他记录为 '1'
    public int resetAllStatusToOne();

    public int resetOtherStatusToOne(Long id);

    /**
     * 删除充值地址
     *
     * @param id 充值地址主键
     * @return 结果
     */
    public int deleteOrderRechargeAddressById(Long id);

    /**
     * 批量删除充值地址
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderRechargeAddressByIds(Long[] ids);
}
