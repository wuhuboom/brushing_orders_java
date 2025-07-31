package com.brushing.web.controller.config;

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
import com.brushing.set.domain.OrderBanner;
import com.brushing.set.service.IOrderBannerService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 轮播图管理Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/banner")
public class OrderBannerController extends BaseController
{
    @Autowired
    private IOrderBannerService orderBannerService;

    /**
     * 查询轮播图管理列表
     */
    @PreAuthorize("@ss.hasPermi('set:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderBanner orderBanner)
    {
        startPage();
        List<OrderBanner> list = orderBannerService.selectOrderBannerList(orderBanner);
        return getDataTable(list);
    }

    /**
     * 导出轮播图管理列表
     */
    @PreAuthorize("@ss.hasPermi('set:banner:export')")
    @Log(title = "轮播图管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderBanner orderBanner)
    {
        List<OrderBanner> list = orderBannerService.selectOrderBannerList(orderBanner);
        ExcelUtil<OrderBanner> util = new ExcelUtil<OrderBanner>(OrderBanner.class);
        util.exportExcel(response, list, "轮播图管理数据");
    }

    /**
     * 获取轮播图管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:banner:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderBannerService.selectOrderBannerById(id));
    }

    /**
     * 新增轮播图管理
     */
    @PreAuthorize("@ss.hasPermi('set:banner:add')")
    @Log(title = "轮播图管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderBanner orderBanner)
    {
        return toAjax(orderBannerService.insertOrderBanner(orderBanner));
    }

    /**
     * 修改轮播图管理
     */
    @PreAuthorize("@ss.hasPermi('set:banner:edit')")
    @Log(title = "轮播图管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderBanner orderBanner)
    {
        return toAjax(orderBannerService.updateOrderBanner(orderBanner));
    }

    /**
     * 删除轮播图管理
     */
    @PreAuthorize("@ss.hasPermi('set:banner:remove')")
    @Log(title = "轮播图管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderBannerService.deleteOrderBannerByIds(ids));
    }
}
