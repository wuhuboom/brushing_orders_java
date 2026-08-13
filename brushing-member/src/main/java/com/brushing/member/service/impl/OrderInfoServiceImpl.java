package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderInfoMapper;
import com.brushing.member.domain.OrderInfo;
import com.brushing.member.service.IOrderInfoService;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.brushing.member.mapper.OrderGoodsHotelMapper;

/**
 * 订单列表Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-02
 */
@Service
public class OrderInfoServiceImpl implements IOrderInfoService 
{
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderGoodsHotelMapper orderGoodsHotelMapper;

    // local TTL cache to avoid repeated information_schema queries but still detect newly created tables
    private static final long TABLE_CACHE_TTL_MS = 5_000L; // 5 seconds
    private static class TableStatus { boolean exists; long checkedAt; }
    private final Map<String, TableStatus> tableExistenceCache = new ConcurrentHashMap<>();

    /**
     * 查询订单列表
     * 
     * @param id 订单列表主键
     * @return 订单列表
     */
    @Override
    public OrderInfo selectOrderInfoById(Long id)
    {
        return orderInfoMapper.selectOrderInfoById(id);
    }

    @Override
    public OrderInfo selectOrderInfoByCode(String code) {
        return orderInfoMapper.selectOrderInfoByCode(code);
    }

    /**
     * 查询订单列表列表
     * 
     * @param orderInfo 订单列表
     * @return 订单列表
     */
    @Override
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo)
    {
        // set flag for mapper to decide whether to join order_goods_hotel
        if (orderInfo != null) {
            orderInfo.setHasOrderGoodsHotel(hasTable("order_goods_hotel"));
        }
        List<OrderInfo> list = orderInfoMapper.selectOrderInfoList(orderInfo);
        // If some orders lack product info (not found in order_goods), try fallback to order_goods_hotel if table exists
        if (list != null && !list.isEmpty()){
            boolean hasHotel = hasTable("order_goods_hotel");
            if (hasHotel) {
                for (OrderInfo oi : list){
                    if (oi.getProduct() == null || oi.getProduct().getId() == null){
                        try {
                            Long pid = oi.getProductId();
                            if (pid != null){
                                oi.setProduct(orderGoodsHotelMapper.selectOrderGoodsById(pid));
                            }
                        } catch (Exception ignore) {
                            // ignore fallback errors
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public List<OrderInfo> selectOrderInfosByUser(OrderInfo orderInfo) {
        if (orderInfo != null) {
            orderInfo.setHasOrderGoodsHotel(hasTable("order_goods_hotel"));
        }
        return orderInfoMapper.selectOrderInfosByUser(orderInfo);
    }

    /**
     * 新增订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    @Override
    public int insertOrderInfo(OrderInfo orderInfo)
    {
        return orderInfoMapper.insertOrderInfo(orderInfo);
    }

    /**
     * 修改订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    @Override
    public int updateOrderInfo(OrderInfo orderInfo)
    {
        orderInfo.setUpdateTime(DateUtils.getNowDate());
        return orderInfoMapper.updateOrderInfo(orderInfo);
    }

    /**
     * 批量删除订单列表
     * 
     * @param ids 需要删除的订单列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoByIds(String[] ids)
    {
        return orderInfoMapper.deleteOrderInfoByIds(ids);
    }

    /**
     * 删除订单列表信息
     * 
     * @param id 订单列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoById(String id)
    {
        return orderInfoMapper.deleteOrderInfoById(id);
    }

    @Override
    public int countUnfinishedOrders(Long userId) {
        return orderInfoMapper.countUnfinishedOrders(userId);
    }

    @Override
    public OrderInfo selectLatestUnfinishedOrder(Long userId) {
        return orderInfoMapper.selectLatestUnfinishedOrder(userId);
    }

    @Override
    public List<OrderInfo> selectOrderInfoBySeries(Long userId) {
        return orderInfoMapper.selectOrderInfoBySeries(userId);
    }

    @Override
    public int countStatusOneInOrderInfo() {
        return orderInfoMapper.countStatusOneInOrderInfo();
    }

    private boolean hasTable(String tableName){
        long now = System.currentTimeMillis();
        TableStatus status = tableExistenceCache.get(tableName);
        if (status != null && (now - status.checkedAt) < TABLE_CACHE_TTL_MS) {
            return status.exists;
        }
        // refresh check
        boolean exists = false;
        try{
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Integer.class, tableName);
            exists = cnt != null && cnt > 0;
        }catch(Exception ex){
            exists = false;
        }
        TableStatus ns = new TableStatus(); ns.exists = exists; ns.checkedAt = now;
        tableExistenceCache.put(tableName, ns);
        return exists;
    }
}
