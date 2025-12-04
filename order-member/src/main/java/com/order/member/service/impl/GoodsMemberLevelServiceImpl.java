package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.service.IGoodsMemberLevelService;

/**
 * 等级Service业务层处理
 * 
 * @author order
 * @date 2025-10-11
 */
@Service
public class GoodsMemberLevelServiceImpl implements IGoodsMemberLevelService 
{
    @Autowired
    private GoodsMemberLevelMapper goodsMemberLevelMapper;

    /**
     * 查询等级
     * 
     * @param id 等级主键
     * @return 等级
     */
    @Override
    public GoodsMemberLevel selectGoodsMemberLevelById(Long id)
    {
        return goodsMemberLevelMapper.selectGoodsMemberLevelById(id);
    }

    /**
     * 查询等级列表
     * 
     * @param goodsMemberLevel 等级
     * @return 等级
     */
    @Override
    public List<GoodsMemberLevel> selectGoodsMemberLevelList(GoodsMemberLevel goodsMemberLevel)
    {
        return goodsMemberLevelMapper.selectGoodsMemberLevelList(goodsMemberLevel);
    }

    /**
     * 新增等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    @Override
    public int insertGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel)
    {
        goodsMemberLevel.setCreateTime(DateUtils.getNowDate());
        return goodsMemberLevelMapper.insertGoodsMemberLevel(goodsMemberLevel);
    }

    /**
     * 修改等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    @Override
    public int updateGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel)
    {
        return goodsMemberLevelMapper.updateGoodsMemberLevel(goodsMemberLevel);
    }

    /**
     * 批量删除等级
     * 
     * @param ids 需要删除的等级主键
     * @return 结果
     */
    @Override
    public int deleteGoodsMemberLevelByIds(Long[] ids)
    {
        return goodsMemberLevelMapper.deleteGoodsMemberLevelByIds(ids);
    }

    /**
     * 删除等级信息
     * 
     * @param id 等级主键
     * @return 结果
     */
    @Override
    public int deleteGoodsMemberLevelById(Long id)
    {
        return goodsMemberLevelMapper.deleteGoodsMemberLevelById(id);
    }
}
