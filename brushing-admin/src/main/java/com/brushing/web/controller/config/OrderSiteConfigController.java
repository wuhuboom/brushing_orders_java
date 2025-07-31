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
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 网站设置Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/siteconfig")
public class OrderSiteConfigController extends BaseController
{
    @Autowired
    private IOrderSiteConfigService orderSiteConfigService;

    /**
     * 查询网站设置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderSiteConfig orderSiteConfig)
    {
        startPage();
        List<OrderSiteConfig> list = orderSiteConfigService.selectOrderSiteConfigList(orderSiteConfig);
        return getDataTable(list);
    }

    /**
     * 导出网站设置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:export')")
    @Log(title = "网站设置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderSiteConfig orderSiteConfig)
    {
        List<OrderSiteConfig> list = orderSiteConfigService.selectOrderSiteConfigList(orderSiteConfig);
        ExcelUtil<OrderSiteConfig> util = new ExcelUtil<OrderSiteConfig>(OrderSiteConfig.class);
        util.exportExcel(response, list, "网站设置数据");
    }

    /**
     * 获取网站设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderSiteConfigService.selectOrderSiteConfigById(id));
    }

    /**
     * 新增网站设置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:add')")
    @Log(title = "网站设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderSiteConfig orderSiteConfig)
    {
        return toAjax(orderSiteConfigService.insertOrderSiteConfig(orderSiteConfig));
    }

    /**
     * 修改网站设置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:edit')")
    @Log(title = "网站设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderSiteConfig orderSiteConfig)
    {
        return toAjax(orderSiteConfigService.updateOrderSiteConfig(orderSiteConfig));
    }

    /**
     * 删除网站设置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:remove')")
    @Log(title = "网站设置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderSiteConfigService.deleteOrderSiteConfigByIds(ids));
    }
}
