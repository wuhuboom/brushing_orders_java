package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderTaksTemplateInfo;

/**
 * 连单模板任务
Service接口
 *
 * @author brushing
 * @date 2026-01-10
 */
public interface IOrderTaksTemplateInfoService
{
    /**
     * 查询连单模板任务

     *
     * @param id 连单模板任务
主键
     * @return 连单模板任务

     */
    public OrderTaksTemplateInfo selectOrderTaksTemplateInfoById(Long id);

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
     * 批量删除连单模板任务

     *
     * @param ids 需要删除的连单模板任务
主键集合
     * @return 结果
     */
    public int deleteOrderTaksTemplateInfoByIds(Long[] ids);

    /**
     * 删除连单模板任务
信息
     *
     * @param id 连单模板任务
主键
     * @return 结果
     */
    public int deleteOrderTaksTemplateInfoById(Long id);
}
