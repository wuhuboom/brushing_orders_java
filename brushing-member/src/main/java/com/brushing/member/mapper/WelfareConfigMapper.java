package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.WelfareConfig;

/**
 * 福利配置Mapper接口
 * 
 * @author brushing
 * @date 2025-10-18
 */
public interface WelfareConfigMapper 
{
    /**
     * 查询福利配置
     * 
     * @param id 福利配置主键
     * @return 福利配置
     */
    public WelfareConfig selectWelfareConfigById(Long id);

    public WelfareConfig selectWelfareConfigByType(String type);

    /**
     * 查询福利配置列表
     * 
     * @param welfareConfig 福利配置
     * @return 福利配置集合
     */
    public List<WelfareConfig> selectWelfareConfigList(WelfareConfig welfareConfig);

    /**
     * 新增福利配置
     * 
     * @param welfareConfig 福利配置
     * @return 结果
     */
    public int insertWelfareConfig(WelfareConfig welfareConfig);

    /**
     * 修改福利配置
     * 
     * @param welfareConfig 福利配置
     * @return 结果
     */
    public int updateWelfareConfig(WelfareConfig welfareConfig);

    /**
     * 删除福利配置
     * 
     * @param id 福利配置主键
     * @return 结果
     */
    public int deleteWelfareConfigById(Long id);

    /**
     * 批量删除福利配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWelfareConfigByIds(Long[] ids);
}
