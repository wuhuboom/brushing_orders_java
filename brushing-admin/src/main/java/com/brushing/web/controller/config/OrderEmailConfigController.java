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
import com.brushing.set.domain.OrderEmailConfig;
import com.brushing.set.service.IOrderEmailConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 系统邮箱配置Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/emialconfig")
public class OrderEmailConfigController extends BaseController
{
    @Autowired
    private IOrderEmailConfigService orderEmailConfigService;

    /**
     * 查询系统邮箱配置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderEmailConfig orderEmailConfig)
    {
        startPage();
        List<OrderEmailConfig> list = orderEmailConfigService.selectOrderEmailConfigList(orderEmailConfig);
        return getDataTable(list);
    }

    /**
     * 导出系统邮箱配置列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:export')")
    @Log(title = "系统邮箱配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderEmailConfig orderEmailConfig)
    {
        List<OrderEmailConfig> list = orderEmailConfigService.selectOrderEmailConfigList(orderEmailConfig);
        ExcelUtil<OrderEmailConfig> util = new ExcelUtil<OrderEmailConfig>(OrderEmailConfig.class);
        util.exportExcel(response, list, "系统邮箱配置数据");
    }

    /**
     * 获取系统邮箱配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderEmailConfigService.selectOrderEmailConfigById(id));
    }

    /**
     * 新增系统邮箱配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:add')")
    @Log(title = "系统邮箱配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderEmailConfig orderEmailConfig)
    {
        return toAjax(orderEmailConfigService.insertOrderEmailConfig(orderEmailConfig));
    }

    /**
     * 修改系统邮箱配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:edit')")
    @Log(title = "系统邮箱配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderEmailConfig orderEmailConfig)
    {
        return toAjax(orderEmailConfigService.updateOrderEmailConfig(orderEmailConfig));
    }

    /**
     * 删除系统邮箱配置
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:remove')")
    @Log(title = "系统邮箱配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderEmailConfigService.deleteOrderEmailConfigByIds(ids));
    }
}
