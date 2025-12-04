package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsMemberLevel;

/**
 * 等级Mapper接口
 * 
 * @author order
 * @date 2025-10-11
 */
public interface GoodsMemberLevelMapper 
{
    /**
     * 查询等级
     * 
     * @param id 等级主键
     * @return 等级
     */
    public GoodsMemberLevel selectGoodsMemberLevelById(Long id);

    public GoodsMemberLevel selectLowestPriceLevel();

    /**
     * 查询等级列表
     * 
     * @param goodsMemberLevel 等级
     * @return 等级集合
     */
    public List<GoodsMemberLevel> selectGoodsMemberLevelList(GoodsMemberLevel goodsMemberLevel);

    /**
     * 新增等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    public int insertGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel);

    /**
     * 修改等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    public int updateGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel);

    /**
     * 删除等级
     * 
     * @param id 等级主键
     * @return 结果
     */
    public int deleteGoodsMemberLevelById(Long id);

    /**
     * 批量删除等级
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsMemberLevelByIds(Long[] ids);
}
