package com.order.member.service;

import java.util.List;
import com.order.member.domain.GoodsMemberLevel;

/**
 * 等级Service接口
 * 
 * @author order
 * @date 2025-10-11
 */
public interface IGoodsMemberLevelService 
{
    /**
     * 查询等级
     * 
     * @param id 等级主键
     * @return 等级
     */
    public GoodsMemberLevel selectGoodsMemberLevelById(Long id);

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
     * 批量删除等级
     * 
     * @param ids 需要删除的等级主键集合
     * @return 结果
     */
    public int deleteGoodsMemberLevelByIds(Long[] ids);

    /**
     * 删除等级信息
     * 
     * @param id 等级主键
     * @return 结果
     */
    public int deleteGoodsMemberLevelById(Long id);
}
