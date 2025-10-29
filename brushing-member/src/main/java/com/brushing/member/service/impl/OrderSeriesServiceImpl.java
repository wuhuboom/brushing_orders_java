package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.*;

import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderGoodsMapper;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.service.IOrderMemberUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderSeriesMapper;
import com.brushing.member.domain.OrderSeries;
import java.math.RoundingMode;
import com.brushing.member.service.IOrderSeriesService;

/**
 * 连单Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-04
 */
@Service
public class OrderSeriesServiceImpl implements IOrderSeriesService 
{
    @Autowired
    private OrderSeriesMapper orderSeriesMapper;

    @Autowired
    private OrderGoodsMapper orderGoodsMapper;

    @Autowired
    private OrderMemberUserMapper memberUserMapper;

    @Autowired
    private OrderMemberLevelMapper memberLevelMapper;

    /**
     * 查询连单
     * 
     * @param id 连单主键
     * @return 连单
     */
    @Override
    public OrderSeries selectOrderSeriesById(Long id)
    {
        return orderSeriesMapper.selectOrderSeriesById(id);
    }

    /**
     * 查询连单列表
     * 
     * @param orderSeries 连单
     * @return 连单
     */
    @Override
    public List<OrderSeries> selectOrderSeriesList(OrderSeries orderSeries)
    {
        return orderSeriesMapper.selectOrderSeriesList(orderSeries);
    }

    /**
     * 新增连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    @Override
    public int insertOrderSeries(OrderSeries orderSeries) {
        // 重复检查：从传入 orderIndex 开始（无 -1），匹配 insert 分配范围
        Integer startCheckIndex = orderSeries.getOrderIndex();  // 假设传入的是下一个起始位置
        Long[] goodsIds = orderSeries.getGoodsIds();

        List<Integer> orderIndexes = new ArrayList<>();
        for (int i = 0; i < goodsIds.length; i++) {
            orderIndexes.add(startCheckIndex + i);
        }
        List<OrderSeries> orderSeries1 = orderSeriesMapper.selectByUserIdAndOrderIndexes(orderSeries.getUserId(), orderIndexes);
        if (orderSeries1.size() > 0) {
            return 5; // 如果已经存在订单，返回5
        }

        BigDecimal frozenAmount = orderSeries.getFrozenAmount();  // 获取当前冻结金额
        if (frozenAmount == null) {
            frozenAmount = BigDecimal.ZERO;
        }

        OrderMemberUser orderMemberUser = memberUserMapper.selectOrderMemberUser(orderSeries.getUserId());

        BigDecimal balance = orderMemberUser.getBalance(); // 获取余额
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        OrderMemberLevel userLevel = memberLevelMapper.selectOrderMemberLevelById(orderMemberUser.getLevelId());

        // 统一计算总金额
        BigDecimal totalAmount = frozenAmount.add(balance);

        // 如果 frozenAmount == 0 或 balance == 0，走简单分支（使用真实价格）
        if (frozenAmount.compareTo(BigDecimal.ZERO) == 0 || balance.compareTo(BigDecimal.ZERO) <= 0) {
            // 简单分支：使用商品真实价格
            Map<Long, OrderGoods> goodsMap = new HashMap<>();  // 优化：缓存商品信息
            for (int i = 0; i < goodsIds.length; i++) {
                OrderGoods orderGoods = orderGoodsMapper.selectOrderGoodsById(goodsIds[i]);
                if (orderGoods == null) {
                    return 6; // 商品不存在，返回6
                }
                goodsMap.put(goodsIds[i], orderGoods);
            }

            // 插入（使用缓存的 goods）
            Integer insertStart = orderSeries.getOrderIndex() - 1;  // 保留 -1，用于递增起始
            for (int i = 0; i < goodsIds.length; i++) {
                OrderGoods orderGoods = goodsMap.get(goodsIds[i]);

                // 创建订单
                OrderSeries series = new OrderSeries();
                if (userLevel.getOrderCount() <= insertStart) {
                    insertStart = 1;
                    series.setOrderIndex(insertStart);
                } else {
                    insertStart += 1;
                    series.setOrderIndex(insertStart);
                }

                series.setCommissionRatio(orderSeries.getCommissionRatio());
                series.setProductId(goodsIds[i]);
                series.setUserId(orderSeries.getUserId());
                series.setPrice(orderGoods.getPrice()); // 设置商品价格
                series.setCreateTime(DateUtils.getNowDate());
                series.setCreateBy(orderSeries.getCreateBy());

                // 插入订单
                orderSeriesMapper.insertOrderSeries(series);
            }
            return 1; // 成功插入订单
        }

        // 随机分支：frozenAmount >0 && balance >0，不检查不足，直接分配 totalAmount
        int totalItems = goodsIds.length;

        // 如果商品数量大于0，开始计算每个商品的价格
        Random random = new Random();

        // 只有一个连单时的特殊处理
        if (totalItems == 1) {
            BigDecimal price = totalAmount; // 只有一个订单时，价格直接等于总金额
            OrderGoods orderGoods = orderGoodsMapper.selectOrderGoodsById(goodsIds[0]);
            if (orderGoods == null) {
                return 6; // 商品不存在，返回6
            }

            Integer insertStart = orderSeries.getOrderIndex() - 1;  // 保留 -1
            OrderSeries series = new OrderSeries();
            if (userLevel.getOrderCount() <= insertStart) {
                insertStart = 1;
            } else {
                insertStart += 1;
            }
            series.setOrderIndex(insertStart);
            series.setCommissionRatio(orderSeries.getCommissionRatio());
            series.setProductId(goodsIds[0]);
            series.setUserId(orderSeries.getUserId());
            series.setPrice(price); // 设置商品价格
            series.setCreateTime(DateUtils.getNowDate());
            series.setCreateBy(orderSeries.getCreateBy());
            orderSeriesMapper.insertOrderSeries(series); // 插入订单
            return 1; // 成功插入订单
        }

        // 前 (totalItems - 1) 个订单从 balance 随机分配，每次上限为当前剩余余额 / 2，确保总和 < balance
        BigDecimal remainingBalance = balance;
        BigDecimal sumFirst = BigDecimal.ZERO;
        List<BigDecimal> prices = new ArrayList<>();
        Map<Long, OrderGoods> goodsMap = new HashMap<>();  // 优化：缓存

        for (int i = 0; i < totalItems - 1; i++) { // 前订单
            // 检查商品存在
            OrderGoods orderGoods = orderGoodsMapper.selectOrderGoodsById(goodsIds[i]);
            if (orderGoods == null) {
                return 6; // 商品不存在，返回6
            }
            goodsMap.put(goodsIds[i], orderGoods);

            // 每次上限：当前剩余余额 / 2
            BigDecimal maxThis = remainingBalance.divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN); // 整数除法
            if (maxThis.compareTo(BigDecimal.ZERO) <= 0) {
                maxThis = BigDecimal.ONE; // 最小1，避免0
            }

            // 随机：0 ~ maxThis
            BigDecimal rand = new BigDecimal(random.nextDouble());
            BigDecimal price = rand.multiply(maxThis).setScale(0, RoundingMode.HALF_UP); // 整数

            // 确保 price <= remainingBalance，且 >0
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                price = BigDecimal.ONE; // 至少1，确保 >0
            }
            if (price.compareTo(remainingBalance) > 0) {
                price = remainingBalance.subtract(BigDecimal.ONE); // 留1给下个，避免总和=balance
            }

            prices.add(price);
            sumFirst = sumFirst.add(price);
            remainingBalance = remainingBalance.subtract(price);
        }

        // 检查最后一个商品
        OrderGoods lastGoods = orderGoodsMapper.selectOrderGoodsById(goodsIds[totalItems - 1]);
        if (lastGoods == null) {
            return 6; // 商品不存在，返回6
        }
        goodsMap.put(goodsIds[totalItems - 1], lastGoods);

        // 最后一个订单：补齐总金额
        BigDecimal lastPrice = totalAmount.subtract(sumFirst);
        prices.add(lastPrice);

        // 确保前总和 < balance（通过留1逻辑，几乎总是 <）
        if (sumFirst.compareTo(balance) >= 0) {
            // 罕见情况，调整最后一个
            lastPrice = lastPrice.add(balance.subtract(sumFirst));
            sumFirst = balance.subtract(BigDecimal.ONE); // 强制 < balance
        }

        // 现在插入所有订单
        Integer insertStart = orderSeries.getOrderIndex() - 1;  // 保留 -1
        for (int i = 0; i < totalItems; i++) {
            BigDecimal price = prices.get(i);
            OrderGoods orderGoods = goodsMap.get(goodsIds[i]);

            // 创建订单
            OrderSeries series = new OrderSeries();
            if (userLevel.getOrderCount() <= insertStart) {
                insertStart = 1;
            } else {
                insertStart += 1;
            }
            series.setOrderIndex(insertStart);
            series.setCommissionRatio(orderSeries.getCommissionRatio());
            series.setProductId(goodsIds[i]);
            series.setUserId(orderSeries.getUserId());
            series.setPrice(price); // 设置计算的价格
            series.setCreateTime(DateUtils.getNowDate());
            series.setCreateBy(orderSeries.getCreateBy());

            orderSeriesMapper.insertOrderSeries(series); // 插入订单
        }

        return 1; // 成功插入订单
    }


    /**
     * 修改连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    @Override
    public int updateOrderSeries(OrderSeries orderSeries)
    {
        return orderSeriesMapper.updateOrderSeries(orderSeries);
    }

    /**
     * 批量删除连单
     * 
     * @param ids 需要删除的连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderSeriesByIds(Long[] ids)
    {
        return orderSeriesMapper.deleteOrderSeriesByIds(ids);
    }

    /**
     * 删除连单信息
     * 
     * @param id 连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderSeriesById(Long id)
    {
        return orderSeriesMapper.deleteOrderSeriesById(id);
    }

    @Override
    public List<OrderSeries> selectSeriesListByUserId(Long userId) {
        return orderSeriesMapper.selectSeriesListByUserId(userId);
    }

    @Override
    public List<OrderSeries> selectOrderSeriesByFrozen(Long userId) {
        return orderSeriesMapper.selectOrderSeriesByFrozen(userId);
    }
}
