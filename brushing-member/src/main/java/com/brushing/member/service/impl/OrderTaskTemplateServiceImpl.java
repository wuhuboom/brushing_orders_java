package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderTaskTemplateMapper;
import com.brushing.member.domain.OrderTaskTemplate;
import com.brushing.member.service.IOrderTaskTemplateService;

/**
 * 连单模板Service业务层处理
 *
 * @author brushing
 * @date 2026-01-10
 */
@Service
public class OrderTaskTemplateServiceImpl implements IOrderTaskTemplateService
{
    @Autowired
    private OrderTaskTemplateMapper orderTaskTemplateMapper;

    /**
     * 查询连单模板
     *
     * @param id 连单模板主键
     * @return 连单模板
     */
    @Override
    public OrderTaskTemplate selectOrderTaskTemplateById(Long id)
    {
        return orderTaskTemplateMapper.selectOrderTaskTemplateById(id);
    }

    /**
     * 查询连单模板列表
     *
     * @param orderTaskTemplate 连单模板
     * @return 连单模板
     */
    @Override
    public List<OrderTaskTemplate> selectOrderTaskTemplateList(OrderTaskTemplate orderTaskTemplate)
    {
        return orderTaskTemplateMapper.selectOrderTaskTemplateList(orderTaskTemplate);
    }

    /**
     * 新增连单模板
     *
     * @param orderTaskTemplate 连单模板
     * @return 结果
     */
    @Override
    public int insertOrderTaskTemplate(OrderTaskTemplate orderTaskTemplate)
    {
        orderTaskTemplate.setCreateTime(DateUtils.getNowDate());
        return orderTaskTemplateMapper.insertOrderTaskTemplate(orderTaskTemplate);
    }

    /**
     * 修改连单模板
     *
     * @param orderTaskTemplate 连单模板
     * @return 结果
     */
    @Override
    public int updateOrderTaskTemplate(OrderTaskTemplate orderTaskTemplate)
    {
        return orderTaskTemplateMapper.updateOrderTaskTemplate(orderTaskTemplate);
    }

    /**
     * 批量删除连单模板
     *
     * @param ids 需要删除的连单模板主键
     * @return 结果
     */
    @Override
    public int deleteOrderTaskTemplateByIds(Long[] ids)
    {
        return orderTaskTemplateMapper.deleteOrderTaskTemplateByIds(ids);
    }

    /**
     * 删除连单模板信息
     *
     * @param id 连单模板主键
     * @return 结果
     */
    @Override
    public int deleteOrderTaskTemplateById(Long id)
    {
        return orderTaskTemplateMapper.deleteOrderTaskTemplateById(id);
    }
}
