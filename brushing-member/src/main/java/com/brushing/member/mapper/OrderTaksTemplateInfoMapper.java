package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderTaksTemplateInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 连单模板任务
Mapper接口
 *
 * @author brushing
 * @date 2026-01-10
 */
public interface OrderTaksTemplateInfoMapper
{
    /**
     * 查询连单模板任务

     *
     * @param id 连单模板任务
主键
     * @return 连单模板任务

     */
    public OrderTaksTemplateInfo selectOrderTaksTemplateInfoById(Long id);

    public List<OrderTaksTemplateInfo> selectOrderTaksTemplateInfoByTemplateId(Long id);

    /**
     * 查询连单模板任务
列表
     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 连单模板任务
集合
     */
    public List<OrderTaksTemplateInfo> selectOrderTaksTemplateInfoList(OrderTaksTemplateInfo orderTaksTemplateInfo);

    /**
     * 新增连单模板任务

     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 结果
     */
    public int insertOrderTaksTemplateInfo(OrderTaksTemplateInfo orderTaksTemplateInfo);

    /**
     * 修改连单模板任务

     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 结果
     */
    public int updateOrderTaksTemplateInfo(OrderTaksTemplateInfo orderTaksTemplateInfo);

    /**
     * 删除连单模板任务

     *
     * @param id 连单模板任务
主键
     * @return 结果
     */
    public int deleteOrderTaksTemplateInfoById(Long id);

    /**
     * 批量删除连单模板任务

     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderTaksTemplateInfoByIds(Long[] ids);

    /**
     * 统计同一模板下指定 orderIndex 的记录数（用于校验重复）
     * 如果 excludeId 不为空，则排除该 id（用于 update 场景）
     */
    public int countByTemplateIdAndOrderIndex(@Param("templateId") Long templateId, @Param("orderIndex") Integer orderIndex, @Param("excludeId") Long excludeId);
}
