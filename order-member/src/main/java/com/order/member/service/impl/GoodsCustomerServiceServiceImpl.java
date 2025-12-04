package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsCustomerServiceMapper;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.service.IGoodsCustomerServiceService;

/**
 * 客服Service业务层处理
 * 
 * @author order
 * @date 2025-11-11
 */
@Service
public class GoodsCustomerServiceServiceImpl implements IGoodsCustomerServiceService 
{
    @Autowired
    private GoodsCustomerServiceMapper goodsCustomerServiceMapper;

    /**
     * 查询客服
     * 
     * @param id 客服主键
     * @return 客服
     */
    @Override
    public GoodsCustomerService selectGoodsCustomerServiceById(String id)
    {
        return goodsCustomerServiceMapper.selectGoodsCustomerServiceById(id);
    }

    /**
     * 查询客服列表
     * 
     * @param goodsCustomerService 客服
     * @return 客服
     */
    @Override
    public List<GoodsCustomerService> selectGoodsCustomerServiceList(GoodsCustomerService goodsCustomerService)
    {
        return goodsCustomerServiceMapper.selectGoodsCustomerServiceList(goodsCustomerService);
    }

    /**
     * 新增客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    @Override
    public int insertGoodsCustomerService(GoodsCustomerService goodsCustomerService)
    {
        goodsCustomerService.setCreateTime(DateUtils.getNowDate());
        return goodsCustomerServiceMapper.insertGoodsCustomerService(goodsCustomerService);
    }

    /**
     * 修改客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    @Override
    public int updateGoodsCustomerService(GoodsCustomerService goodsCustomerService)
    {
        return goodsCustomerServiceMapper.updateGoodsCustomerService(goodsCustomerService);
    }

    /**
     * 批量删除客服
     * 
     * @param ids 需要删除的客服主键
     * @return 结果
     */
    @Override
    public int deleteGoodsCustomerServiceByIds(String[] ids)
    {
        return goodsCustomerServiceMapper.deleteGoodsCustomerServiceByIds(ids);
    }

    /**
     * 删除客服信息
     * 
     * @param id 客服主键
     * @return 结果
     */
    @Override
    public int deleteGoodsCustomerServiceById(String id)
    {
        return goodsCustomerServiceMapper.deleteGoodsCustomerServiceById(id);
    }
}
