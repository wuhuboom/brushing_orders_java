package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderBonusTable;
import org.apache.ibatis.annotations.Param;

/**
 * 彩金Mapper接口
 * 
 * @author order
 * @date 2025-11-04
 */
public interface OrderBonusTableMapper 
{
    /**
     * 查询彩金
     * 
     * @param id 彩金主键
     * @return 彩金
     */
    OrderBonusTable selectOrderBonusTableById(Long id);

    /**
     * 查询彩金列表
     * 
     * @param orderBonusTable 彩金
     * @return 彩金集合
     */
    List<OrderBonusTable> selectOrderBonusTableList(OrderBonusTable orderBonusTable);

    /**
     * 新增彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    int insertOrderBonusTable(OrderBonusTable orderBonusTable);

    /**
     * 修改彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    int updateOrderBonusTable(OrderBonusTable orderBonusTable);

    /**
     * 删除彩金
     * 
     * @param id 彩金主键
     * @return 结果
     */
    int deleteOrderBonusTableById(Long id);

    /**
     * 批量删除彩金
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteOrderBonusTableByIds(Long[] ids);

    /**
     * 查询符合已接收且已分发且未过期的彩金（按 userId 和 orderNum 匹配）
     *
     * 必填参数：userId（Long）、orderNum（Long）
     *
     * @param userId 用户ID（必填）
     * @param orderNum 单数（必填）
     * @return 彩金集合
     */
   public OrderBonusTable selectActiveDistributedReceivedByUserAndOrder(@Param("userId") Long userId,
                                                                              @Param("orderNum") Long orderNum);
    public List<OrderBonusTable> selectBonusByType(Long userId);

    public OrderBonusTable userHaveBonus(@Param("userId") Long userId,@Param("orderNum") Integer orderNum);

    OrderBonusTable selectNextCompletionBonus(Long userId);

    OrderBonusTable selectOwnedBonusForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    int claimBonus(@Param("id") Long id, @Param("userId") Long userId);
}
