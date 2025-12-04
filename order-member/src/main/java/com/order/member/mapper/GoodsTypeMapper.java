package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsType;

/**
 * 类目管理Mapper接口
 * 
 * @author order
 * @date 2025-10-11
 */
public interface GoodsTypeMapper 
{
    /**
     * 查询类目管理
     * 
     * @param id 类目管理主键
     * @return 类目管理
     */
    public GoodsType selectGoodsTypeById(Long id);

    /**
     * 查询类目管理列表
     * 
     * @param goodsType 类目管理
     * @return 类目管理集合
     */
    public List<GoodsType> selectGoodsTypeList(GoodsType goodsType);

    /**
     * 新增类目管理
     * 
     * @param goodsType 类目管理
     * @return 结果
     */
    public int insertGoodsType(GoodsType goodsType);

    /**
     * 修改类目管理
     * 
     * @param goodsType 类目管理
     * @return 结果
     */
    public int updateGoodsType(GoodsType goodsType);

    /**
     * 删除类目管理
     * 
     * @param id 类目管理主键
     * @return 结果
     */
    public int deleteGoodsTypeById(Long id);

    /**
     * 批量删除类目管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsTypeByIds(Long[] ids);
}
