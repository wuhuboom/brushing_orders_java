package com.brushing.set.mapper;

import java.util.List;
import com.brushing.set.domain.OrderGlobalConfig;

/**
 * 全局配置（中英文内容）Mapper接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface OrderGlobalConfigMapper 
{
    /**
     * 查询全局配置（中英文内容）
     * 
     * @param id 全局配置（中英文内容）主键
     * @return 全局配置（中英文内容）
     */
    public OrderGlobalConfig selectOrderGlobalConfigById(Long id);

    /**
     * 查询全局配置（中英文内容）列表
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 全局配置（中英文内容）集合
     */
    public List<OrderGlobalConfig> selectOrderGlobalConfigList(OrderGlobalConfig orderGlobalConfig);

    /**
     * 新增全局配置（中英文内容）
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 结果
     */
    public int insertOrderGlobalConfig(OrderGlobalConfig orderGlobalConfig);

    /**
     * 修改全局配置（中英文内容）
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 结果
     */
    public int updateOrderGlobalConfig(OrderGlobalConfig orderGlobalConfig);

    /**
     * 删除全局配置（中英文内容）
     * 
     * @param id 全局配置（中英文内容）主键
     * @return 结果
     */
    public int deleteOrderGlobalConfigById(Long id);

    /**
     * 批量删除全局配置（中英文内容）
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderGlobalConfigByIds(Long[] ids);
}
