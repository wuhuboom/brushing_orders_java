package com.order.api.controller;


import com.github.pagehelper.PageHelper;
import com.order.api.controller.dto.PageDto;
import com.order.api.service.ApiPagination;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.member.domain.Goods;
import com.order.member.service.IGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品管理",description =
        "错误码对照表：\n" +
                "701: No data （暂无数据）")
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
        return success(goods);
    }

    @GetMapping("/getGoodsListByPage")
    @Operation(summary = "通过分页获取商品信息")
    public TableDataInfo getGoodsListByPage(@Valid PageDto page){
        int pageNum = ApiPagination.pageNumber(page == null ? null : page.getPageNum());
        int pageSize = ApiPagination.pageSize(page == null ? null : page.getPageSize());
        PageHelper.startPage(pageNum, pageSize);
        List<Goods> goods = goodsService.selectGoodsPage();
        return getDataTable(goods);
    }

}
