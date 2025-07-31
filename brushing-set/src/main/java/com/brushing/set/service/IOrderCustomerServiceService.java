package com.brushing.set.service;

import java.util.List;
import com.brushing.set.domain.OrderCustomerService;

/**
 * 客服管理Service接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface IOrderCustomerServiceService 
{
    /**
     * 查询客服管理
     * 
     * @param id 客服管理主键
     * @return 客服管理
     */
    public OrderCustomerService selectOrderCustomerServiceById(Long id);

    /**
     * 查询客服管理列表
     * 
     * @param orderCustomerService 客服管理
     * @return 客服管理集合
     */
    public List<OrderCustomerService> selectOrderCustomerServiceList(OrderCustomerService orderCustomerService);

    /**
     * 新增客服管理
     * 
     * @param orderCustomerService 客服管理
     * @return 结果
     */
    public int insertOrderCustomerService(OrderCustomerService orderCustomerService);

    /**
     * 修改客服管理
     * 
     * @param orderCustomerService 客服管理
     * @return 结果
     */
    public int updateOrderCustomerService(OrderCustomerService orderCustomerService);

    /**
     * 批量删除客服管理
     * 
     * @param ids 需要删除的客服管理主键集合
     * @return 结果
     */
    public int deleteOrderCustomerServiceByIds(Long[] ids);

    /**
     * 删除客服管理信息
     * 
     * @param id 客服管理主键
     * @return 结果
     */
    public int deleteOrderCustomerServiceById(Long id);
}
