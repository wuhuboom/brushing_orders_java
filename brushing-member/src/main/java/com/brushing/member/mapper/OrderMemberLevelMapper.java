package com.brushing.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.brushing.member.domain.OrderMemberLevel;
import org.apache.ibatis.annotations.Param;

/**
 * 会员等级Mapper接口
 * 
 * @author brushing
 * @date 2025-07-30
 */
public interface OrderMemberLevelMapper 
{
    /**
     * 查询会员等级
     * 
     * @param id 会员等级主键
     * @return 会员等级
     */
    public OrderMemberLevel selectOrderMemberLevelById(Long id);

    /**
     * 查询会员价格最低的等级 ，作为注册的最低等级
     * @return
     */
    public OrderMemberLevel selectLowestPriceLevel();

    /**
     * 查询会员等级列表
     * 
     * @param orderMemberLevel 会员等级
     * @return 会员等级集合
     */
    public List<OrderMemberLevel> selectOrderMemberLevelList(OrderMemberLevel orderMemberLevel);

    /**
     * 新增会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    public int insertOrderMemberLevel(OrderMemberLevel orderMemberLevel);

    /**
     * 修改会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    public int updateOrderMemberLevel(OrderMemberLevel orderMemberLevel);

    /**
     * 删除会员等级
     * 
     * @param id 会员等级主键
     * @return 结果
     */
    public int deleteOrderMemberLevelById(Long id);

    /**
     * 批量删除会员等级
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderMemberLevelByIds(Long[] ids);

    public OrderMemberLevel findLevelByBalance(@Param("balance") BigDecimal balance);
    public OrderMemberLevel selectLevelByRank(int rank);

    public int selectLevelById(Long id);
}
