package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderTaskTemplate;

/**
 * 连单模板Service接口
 *
 * @author brushing
 * @date 2026-01-10
 */
public interface IOrderTaskTemplateService
{
    /**
     * 查询连单模板
     *
     * @param id 连单模板主键
     * @return 连单模板
     */
    public OrderTaskTemplate selectOrderTaskTemplateById(Long id);

    /**
     * 查询连单模板列表
     *
     * @param orderTaskTemplate 连单模板
     * @return 连单模板集合
     */
    public List<OrderTaskTemplate> selectOrderTaskTemplateList(OrderTaskTemplate orderTaskTemplate);

    /**
     * 新增连单模板
     *
     * @param orderTaskTemplate 连单模板
     * @return 结果
     */
    public int insertOrderTaskTemplate(OrderTaskTemplate orderTaskTemplate);

    /**
     * 修改连单模板
     *
     * @param orderTaskTemplate 连单模板
     * @return 结果
     */
    public int updateOrderTaskTemplate(OrderTaskTemplate orderTaskTemplate);

    /**
     * 批量删除连单模板
     *
     * @param ids 需要删除的连单模板主键集合
     * @return 结果
     */
    public int deleteOrderTaskTemplateByIds(Long[] ids);

    /**
     * 删除连单模板信息
     *
     * @param id 连单模板主键
     * @return 结果
     */
    public int deleteOrderTaskTemplateById(Long id);
}
