package com.brushing.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.brushing.member.domain.OrderGoods;
import org.apache.ibatis.annotations.Param;

/**
 * 酒店商品 Mapper 接口（与 order_goods 结构一致，表名为 order_goods_hotel）
 */
public interface OrderGoodsHotelMapper {
    public OrderGoods selectOrderGoodsById(Long id);
    public OrderGoods selectNearestPriceGoods(@Param("price") BigDecimal price);
    public List<OrderGoods> selectOrderGoodsList(OrderGoods orderGoods);
    public List<OrderGoods> selectRandomOrderGoods();
    public List<OrderGoods> selectRandomOrderGoodsTwo();
    public int insertOrderGoods(OrderGoods orderGoods);
    public int updateOrderGoods(OrderGoods orderGoods);
    public int deleteOrderGoodsById(String id);
    public int deleteOrderGoodsByIds(String[] ids);
}
