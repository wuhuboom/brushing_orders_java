package com.brushing.api.controller;

import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.service.IOrderGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品管理",description =
        "错误码对照表：\n" +
                "701: No data （暂无数据）")
@RestController
@RequestMapping("/api/goods")
public class GoodsController extends BaseController {

    @Autowired
    private IOrderGoodsService orderGoodsService;

    @GetMapping("/getGoodsList")
    @Operation(summary = "获取商品信息",
            description =
                    "'name': '商品名称',\n" +
                    "'coverUrl': '商品封面图URL',\n" +
                    "'price': '商品价格',\n" +
                    "'description': '商品描述',\n"

    )
    public AjaxResult getGoodsList(){
        List<OrderGoods> orderGoods = orderGoodsService.selectRandomOrderGoods();
        if (orderGoods == null || orderGoods.isEmpty()){
            return AjaxResult.error(701, "No data yet");
        }
        return success(orderGoods);

    }
}
