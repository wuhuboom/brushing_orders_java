package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderTaksTemplateInfoMapper;
import com.brushing.member.domain.OrderTaksTemplateInfo;
import com.brushing.member.service.IOrderTaksTemplateInfoService;
import com.brushing.common.exception.ServiceException;

/**
 * 连单模板任务
Service业务层处理
 *
 * @author brushing
 * @date 2026-01-10
 */
@Service
public class OrderTaksTemplateInfoServiceImpl implements IOrderTaksTemplateInfoService
{
    @Autowired
    private OrderTaksTemplateInfoMapper orderTaksTemplateInfoMapper;

    /**
     * 查询连单模板任务

     *
     * @param id 连单模板任务
主键
     * @return 连单模板任务

     */
    @Override
    public OrderTaksTemplateInfo selectOrderTaksTemplateInfoById(Long id)
    {
        return orderTaksTemplateInfoMapper.selectOrderTaksTemplateInfoById(id);
    }

    /**
     * 查询连单模板任务
列表
     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 连单模板任务

     */
    @Override
    public List<OrderTaksTemplateInfo> selectOrderTaksTemplateInfoList(OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        return orderTaksTemplateInfoMapper.selectOrderTaksTemplateInfoList(orderTaksTemplateInfo);
    }

    /**
     * 新增连单模板任务

     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 结果
     */
    @Override
    public int insertOrderTaksTemplateInfo(OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        // 检查同一 templateId 下 orderIndex 是否重复
        if (orderTaksTemplateInfo.getTemplateId() != null && orderTaksTemplateInfo.getOrderIndex() != null) {
            int cnt = orderTaksTemplateInfoMapper.countByTemplateIdAndOrderIndex(orderTaksTemplateInfo.getTemplateId(), orderTaksTemplateInfo.getOrderIndex(), null);
            if (cnt > 0) {
                throw new ServiceException("同一模板单数不能重复");
            }
        }

        orderTaksTemplateInfo.setCreateTime(DateUtils.getNowDate());
        try {
            return orderTaksTemplateInfoMapper.insertOrderTaksTemplateInfo(orderTaksTemplateInfo);
        } catch (DataAccessException ex) {
            // 可能是数据库唯一约束导致的异常，再次转换为业务异常以便前端友好提示
            throw new ServiceException("同一模板单数不能重复");
        }
    }

    /**
     * 修改连单模板任务

     *
     * @param orderTaksTemplateInfo 连单模板任务

     * @return 结果
     */
    @Override
    public int updateOrderTaksTemplateInfo(OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        // 检查同一 templateId 下 orderIndex 是否重复（排除自身 id）
        if (orderTaksTemplateInfo.getTemplateId() != null && orderTaksTemplateInfo.getOrderIndex() != null && orderTaksTemplateInfo.getId() != null) {
            int cnt = orderTaksTemplateInfoMapper.countByTemplateIdAndOrderIndex(orderTaksTemplateInfo.getTemplateId(), orderTaksTemplateInfo.getOrderIndex(), orderTaksTemplateInfo.getId());
            if (cnt > 0) {
                throw new ServiceException("同一模板下第几单(orderIndex)不能重复");
            }
        }

        try {
            return orderTaksTemplateInfoMapper.updateOrderTaksTemplateInfo(orderTaksTemplateInfo);
        } catch (DataAccessException ex) {
            throw new ServiceException("同一模板下第几单(orderIndex)不能重复");
        }
    }

    /**
     * 批量删除连单模板任务

     *
     * @param ids 需要删除的连单模板任务
主键
     * @return 结果
     */
    @Override
    public int deleteOrderTaksTemplateInfoByIds(Long[] ids)
    {
        return orderTaksTemplateInfoMapper.deleteOrderTaksTemplateInfoByIds(ids);
    }

    /**
     * 删除连单模板任务
信息
     *
     * @param id 连单模板任务
主键
     * @return 结果
     */
    @Override
    public int deleteOrderTaksTemplateInfoById(Long id)
    {
        return orderTaksTemplateInfoMapper.deleteOrderTaksTemplateInfoById(id);
    }
}
