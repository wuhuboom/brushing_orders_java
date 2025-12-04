package com.order.member.service.impl;

import java.util.List;

import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsTypeMapper;
import com.order.member.domain.GoodsType;
import com.order.member.service.IGoodsTypeService;

/**
 * 类目管理Service业务层处理
 * 
 * @author order
 * @date 2025-10-11
 */
@Service
public class GoodsTypeServiceImpl implements IGoodsTypeService 
{
    @Autowired
    private GoodsTypeMapper goodsTypeMapper;

    /**
     * 查询类目管理
     * 
     * @param id 类目管理主键
     * @return 类目管理
     */
    @Override
    public GoodsType selectGoodsTypeById(Long id)
    {
        return goodsTypeMapper.selectGoodsTypeById(id);
    }

    /**
     * 查询类目管理列表
     * 
     * @param goodsType 类目管理
     * @return 类目管理
     */
    @Override
    public List<GoodsType> selectGoodsTypeList(GoodsType goodsType)
    {
        return goodsTypeMapper.selectGoodsTypeList(goodsType);
    }

    /**
     * 新增类目管理
     * 
     * @param goodsType 类目管理
     * @return 结果
     */
    @Override
    public int insertGoodsType(GoodsType goodsType)
    {
        goodsType.setCreateTime(DateUtils.getNowDate());
        return goodsTypeMapper.insertGoodsType(goodsType);
    }

    /**
     * 修改类目管理
     * 
     * @param goodsType 类目管理
     * @return 结果
     */
    @Override
    public int updateGoodsType(GoodsType goodsType)
    {
        return goodsTypeMapper.updateGoodsType(goodsType);
    }

    /**
     * 批量删除类目管理
     * 
     * @param ids 需要删除的类目管理主键
     * @return 结果
     */
    @Override
    public int deleteGoodsTypeByIds(Long[] ids)
    {
        return goodsTypeMapper.deleteGoodsTypeByIds(ids);
    }

    /**
     * 删除类目管理信息
     * 
     * @param id 类目管理主键
     * @return 结果
     */
    @Override
    public int deleteGoodsTypeById(Long id)
    {
        return goodsTypeMapper.deleteGoodsTypeById(id);
    }
}
