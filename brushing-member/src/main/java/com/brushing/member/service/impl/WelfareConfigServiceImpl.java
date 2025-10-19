package com.brushing.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.WelfareConfigMapper;
import com.brushing.member.domain.WelfareConfig;
import com.brushing.member.service.IWelfareConfigService;

/**
 * 福利配置Service业务层处理
 * 
 * @author brushing
 * @date 2025-10-18
 */
@Service
public class WelfareConfigServiceImpl implements IWelfareConfigService 
{
    @Autowired
    private WelfareConfigMapper welfareConfigMapper;

    /**
     * 查询福利配置
     * 
     * @param id 福利配置主键
     * @return 福利配置
     */
    @Override
    public WelfareConfig selectWelfareConfigById(Long id)
    {
        return welfareConfigMapper.selectWelfareConfigById(id);
    }

    /**
     * 查询福利配置列表
     * 
     * @param welfareConfig 福利配置
     * @return 福利配置
     */
    @Override
    public List<WelfareConfig> selectWelfareConfigList(WelfareConfig welfareConfig)
    {
        return welfareConfigMapper.selectWelfareConfigList(welfareConfig);
    }

    /**
     * 新增福利配置
     * 
     * @param welfareConfig 福利配置
     * @return 结果
     */
    @Override
    public int insertWelfareConfig(WelfareConfig welfareConfig)
    {
        return welfareConfigMapper.insertWelfareConfig(welfareConfig);
    }

    /**
     * 修改福利配置
     * 
     * @param welfareConfig 福利配置
     * @return 结果
     */
    @Override
    public int updateWelfareConfig(WelfareConfig welfareConfig)
    {
        return welfareConfigMapper.updateWelfareConfig(welfareConfig);
    }

    /**
     * 批量删除福利配置
     * 
     * @param ids 需要删除的福利配置主键
     * @return 结果
     */
    @Override
    public int deleteWelfareConfigByIds(Long[] ids)
    {
        return welfareConfigMapper.deleteWelfareConfigByIds(ids);
    }

    /**
     * 删除福利配置信息
     * 
     * @param id 福利配置主键
     * @return 结果
     */
    @Override
    public int deleteWelfareConfigById(Long id)
    {
        return welfareConfigMapper.deleteWelfareConfigById(id);
    }
}
