package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.common.utils.StringUtils;
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
import com.brushing.member.domain.OrderShop;
import com.brushing.member.service.IOrderShopService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 商品店铺Controller
 * 
 * @author brushing
 * @date 2025-11-17
 */
@RestController
@RequestMapping("/member/shop")
public class OrderShopController extends BaseController
{
    @Autowired
    private IOrderShopService orderShopService;

    /**
     * 查询商品店铺列表
     */
    @PreAuthorize("@ss.hasPermi('member:shop:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderShop orderShop)
    {
        startPage();
        List<OrderShop> list = orderShopService.selectOrderShopList(orderShop);
        return getDataTable(list);
    }

    /**
     * 导出商品店铺列表
     */
    @PreAuthorize("@ss.hasPermi('member:shop:export')")
    @Log(title = "商品店铺", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderShop orderShop)
    {
        List<OrderShop> list = orderShopService.selectOrderShopList(orderShop);
        ExcelUtil<OrderShop> util = new ExcelUtil<OrderShop>(OrderShop.class);
        util.exportExcel(response, list, "商品店铺数据");
    }

    /**
     * 获取商品店铺详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:shop:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderShopService.selectOrderShopById(id));
    }

    /**
     * 新增商品店铺
     */
    @PreAuthorize("@ss.hasPermi('member:shop:add')")
    @Log(title = "商品店铺", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderShop orderShop)
    {
        OrderShop orderShop1 = orderShopService.selectOrderShopByVipLevel(orderShop.getVipLevel());
        if (StringUtils.isNotNull(orderShop1)) {
            return AjaxResult.error("该等级的店铺已存在，不能重复添加！");
        }
        return toAjax(orderShopService.insertOrderShop(orderShop));
    }

    /**
     * 修改商品店铺
     */
    @PreAuthorize("@ss.hasPermi('member:shop:edit')")
    @Log(title = "商品店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderShop orderShop)
    {
        return toAjax(orderShopService.updateOrderShop(orderShop));
    }

    /**
     * 删除商品店铺
     */
    @PreAuthorize("@ss.hasPermi('member:shop:remove')")
    @Log(title = "商品店铺", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderShopService.deleteOrderShopByIds(ids));
    }
}
