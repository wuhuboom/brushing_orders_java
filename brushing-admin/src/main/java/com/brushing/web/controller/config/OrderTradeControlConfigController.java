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
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderTradeControlConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 交易控制配置Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/tardeconfig")
public class OrderTradeControlConfigController extends BaseController
{
    @Autowired
    private IOrderTradeControlConfigService orderTradeControlConfigService;

    /**
     * 查询交易控制配置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderTradeControlConfig orderTradeControlConfig)
    {
        startPage();
        List<OrderTradeControlConfig> list = orderTradeControlConfigService.selectOrderTradeControlConfigList(orderTradeControlConfig);
        return getDataTable(list);
    }

    /**
     * 导出交易控制配置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:export')")
    @Log(title = "交易控制配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderTradeControlConfig orderTradeControlConfig)
    {
        List<OrderTradeControlConfig> list = orderTradeControlConfigService.selectOrderTradeControlConfigList(orderTradeControlConfig);
        ExcelUtil<OrderTradeControlConfig> util = new ExcelUtil<OrderTradeControlConfig>(OrderTradeControlConfig.class);
        util.exportExcel(response, list, "交易控制配置数据");
    }

    /**
     * 获取交易控制配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderTradeControlConfigService.selectOrderTradeControlConfigById(id));
    }

    /**
     * 新增交易控制配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:add')")
    @Log(title = "交易控制配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderTradeControlConfig orderTradeControlConfig)
    {
        return toAjax(orderTradeControlConfigService.insertOrderTradeControlConfig(orderTradeControlConfig));
    }

    /**
     * 修改交易控制配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:edit')")
    @Log(title = "交易控制配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderTradeControlConfig orderTradeControlConfig)
    {
        return toAjax(orderTradeControlConfigService.updateOrderTradeControlConfig(orderTradeControlConfig));
    }

    /**
     * 删除交易控制配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:remove')")
    @Log(title = "交易控制配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderTradeControlConfigService.deleteOrderTradeControlConfigByIds(ids));
    }
}
