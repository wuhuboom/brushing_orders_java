package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.member.domain.OrderGoodsType;
import com.brushing.member.service.IOrderGoodsTypeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.brushing.common.annotation.Log;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.enums.BusinessType;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.service.IOrderGoodsService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 商品列表Controller
 * 
 * @author brushing
 * @date 2025-08-01
 */
@RestController
@RequestMapping("/member/goods")
public class OrderGoodsController extends BaseController
{
    @Autowired
    private IOrderGoodsService orderGoodsService;

    @Autowired
    private IOrderGoodsTypeService orderGoodsTypeService;

    /**
     * 查询商品列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderGoods orderGoods)
    {
        startPage();
        List<OrderGoods> list = orderGoodsService.selectOrderGoodsList(orderGoods);
        return getDataTable(list);
    }

    @GetMapping("/getTypeList")
    public AjaxResult getTypeList(){
        List<OrderGoodsType> orderGoodsTypes = orderGoodsTypeService.selectOrderGoodsTypeList(null);
        return success(orderGoodsTypes);
    }

    /**
     * 导出商品列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:export')")
    @Log(title = "商品列表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderGoods orderGoods)
    {
        List<OrderGoods> list = orderGoodsService.selectOrderGoodsList(orderGoods);
        ExcelUtil<OrderGoods> util = new ExcelUtil<OrderGoods>(OrderGoods.class);
        util.exportExcel(response, list, "商品列表数据");
    }

    /**
     * 获取商品列表详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:goods:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(orderGoodsService.selectOrderGoodsById(id));
    }

    /**
     * 新增商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:add')")
    @Log(title = "商品列表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderGoods orderGoods)
    {
        return toAjax(orderGoodsService.insertOrderGoods(orderGoods));
    }

    /**
     * 修改商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:edit')")
    @Log(title = "商品列表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderGoods orderGoods)
    {
        return toAjax(orderGoodsService.updateOrderGoods(orderGoods));
    }

    /**
     * 删除商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:remove')")
    @Log(title = "商品列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(orderGoodsService.deleteOrderGoodsByIds(ids));
    }
}
