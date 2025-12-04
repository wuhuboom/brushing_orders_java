package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsCustomerService;

/**
 * 客服Mapper接口
 * 
 * @author order
 * @date 2025-11-11
 */
public interface GoodsCustomerServiceMapper 
{
    /**
     * 查询客服
     * 
     * @param id 客服主键
     * @return 客服
     */
    public GoodsCustomerService selectGoodsCustomerServiceById(String id);

    /**
     * 查询客服列表
     * 
     * @param goodsCustomerService 客服
     * @return 客服集合
     */
    public List<GoodsCustomerService> selectGoodsCustomerServiceList(GoodsCustomerService goodsCustomerService);

    /**
     * 新增客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    public int insertGoodsCustomerService(GoodsCustomerService goodsCustomerService);

    /**
     * 修改客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    public int updateGoodsCustomerService(GoodsCustomerService goodsCustomerService);

    /**
     * 删除客服
     * 
     * @param id 客服主键
     * @return 结果
     */
    public int deleteGoodsCustomerServiceById(String id);

    /**
     * 批量删除客服
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsCustomerServiceByIds(String[] ids);
}
