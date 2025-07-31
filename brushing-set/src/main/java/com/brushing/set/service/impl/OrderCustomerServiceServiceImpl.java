package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderCustomerServiceMapper;
import com.brushing.set.domain.OrderCustomerService;
import com.brushing.set.service.IOrderCustomerServiceService;

/**
 * 客服管理Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderCustomerServiceServiceImpl implements IOrderCustomerServiceService 
{
    @Autowired
    private OrderCustomerServiceMapper orderCustomerServiceMapper;

    /**
     * 查询客服管理
     * 
     * @param id 客服管理主键
     * @return 客服管理
     */
    @Override
    public OrderCustomerService selectOrderCustomerServiceById(Long id)
    {
        return orderCustomerServiceMapper.selectOrderCustomerServiceById(id);
    }

    /**
     * 查询客服管理列表
     * 
     * @param orderCustomerService 客服管理
     * @return 客服管理
     */
    @Override
    public List<OrderCustomerService> selectOrderCustomerServiceList(OrderCustomerService orderCustomerService)
    {
        return orderCustomerServiceMapper.selectOrderCustomerServiceList(orderCustomerService);
    }

    /**
     * 新增客服管理
     * 
     * @param orderCustomerService 客服管理
     * @return 结果
     */
    @Override
    public int insertOrderCustomerService(OrderCustomerService orderCustomerService)
    {
        orderCustomerService.setCreateTime(DateUtils.getNowDate());
        return orderCustomerServiceMapper.insertOrderCustomerService(orderCustomerService);
    }

    /**
     * 修改客服管理
     * 
     * @param orderCustomerService 客服管理
     * @return 结果
     */
    @Override
    public int updateOrderCustomerService(OrderCustomerService orderCustomerService)
    {
        orderCustomerService.setUpdateTime(DateUtils.getNowDate());
        return orderCustomerServiceMapper.updateOrderCustomerService(orderCustomerService);
    }

    /**
     * 批量删除客服管理
     * 
     * @param ids 需要删除的客服管理主键
     * @return 结果
     */
    @Override
    public int deleteOrderCustomerServiceByIds(Long[] ids)
    {
        return orderCustomerServiceMapper.deleteOrderCustomerServiceByIds(ids);
    }

    /**
     * 删除客服管理信息
     * 
     * @param id 客服管理主键
     * @return 结果
     */
    @Override
    public int deleteOrderCustomerServiceById(Long id)
    {
        return orderCustomerServiceMapper.deleteOrderCustomerServiceById(id);
    }
}
