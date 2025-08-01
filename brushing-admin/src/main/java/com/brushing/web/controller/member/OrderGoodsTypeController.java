package com.brushing.web.controller.member;

import java.util.List;
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
import com.brushing.member.domain.OrderGoodsType;
import com.brushing.member.service.IOrderGoodsTypeService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 商品类别Controller
 * 
 * @author brushing
 * @date 2025-08-01
 */
@RestController
@RequestMapping("/member/goodstype")
public class OrderGoodsTypeController extends BaseController
{
    @Autowired
    private IOrderGoodsTypeService orderGoodsTypeService;

    /**
     * 查询商品类别列表
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderGoodsType orderGoodsType)
    {
        startPage();
        List<OrderGoodsType> list = orderGoodsTypeService.selectOrderGoodsTypeList(orderGoodsType);
        return getDataTable(list);
    }

    /**
     * 导出商品类别列表
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:export')")
    @Log(title = "商品类别", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderGoodsType orderGoodsType)
    {
        List<OrderGoodsType> list = orderGoodsTypeService.selectOrderGoodsTypeList(orderGoodsType);
        ExcelUtil<OrderGoodsType> util = new ExcelUtil<OrderGoodsType>(OrderGoodsType.class);
        util.exportExcel(response, list, "商品类别数据");
    }

    /**
     * 获取商品类别详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderGoodsTypeService.selectOrderGoodsTypeById(id));
    }

    /**
     * 新增商品类别
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:add')")
    @Log(title = "商品类别", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderGoodsType orderGoodsType)
    {
        return toAjax(orderGoodsTypeService.insertOrderGoodsType(orderGoodsType));
    }

    /**
     * 修改商品类别
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:edit')")
    @Log(title = "商品类别", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderGoodsType orderGoodsType)
    {
        return toAjax(orderGoodsTypeService.updateOrderGoodsType(orderGoodsType));
    }

    /**
     * 删除商品类别
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:remove')")
    @Log(title = "商品类别", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderGoodsTypeService.deleteOrderGoodsTypeByIds(ids));
    }
}
