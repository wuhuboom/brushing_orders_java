package com.order.api.controller;


import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.member.domain.Goods;
import com.order.member.service.IGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品管理",description =
        "错误码对照表：\n" +
                "701：暂无数据")
@RestController
@RequestMapping("/api/goods")
public class GoodsApiController extends BaseController {
    private final IGoodsService goodsService;

    public GoodsApiController(IGoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/getGoodsList")
    @Operation(summary = "获取商品信息",
            description = "    - id: 商品ID\n" +
                    "    - title: 标题\n" +
                    "    - isEnabled: 是否启用\n" +
                    "    - price: 价格\n" +
                    "    - serialNumber: 序号\n" +
                    "    - image: 图片URL\n" +
                    "    - subTitle: 二级标题\n" +
                    "    - unitPrice: 单价\n" +
                    "    - quantity: 数量\n" +
                    "    - starRating: 星级\n" +
                    "    - rating: 评分\n" +
                    "    - description: 说明/详情\n"
    )
    public AjaxResult getGoodsList(){
        List<Goods> goods = goodsService.selectRandomGoods();
        return AjaxResult.success("Success", goods);
    }
}
