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
import com.brushing.set.domain.OrderGlobalConfig;
import com.brushing.set.service.IOrderGlobalConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 全局配置（中英文内容）Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/globalconfig")
public class OrderGlobalConfigController extends BaseController
{
    @Autowired
    private IOrderGlobalConfigService orderGlobalConfigService;

    /**
     * 查询全局配置（中英文内容）列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderGlobalConfig orderGlobalConfig)
    {
        startPage();
        List<OrderGlobalConfig> list = orderGlobalConfigService.selectOrderGlobalConfigList(orderGlobalConfig);
        return getDataTable(list);
    }

    /**
     * 导出全局配置（中英文内容）列表
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:export')")
    @Log(title = "全局配置（中英文内容）", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderGlobalConfig orderGlobalConfig)
    {
        List<OrderGlobalConfig> list = orderGlobalConfigService.selectOrderGlobalConfigList(orderGlobalConfig);
        ExcelUtil<OrderGlobalConfig> util = new ExcelUtil<OrderGlobalConfig>(OrderGlobalConfig.class);
        util.exportExcel(response, list, "全局配置（中英文内容）数据");
    }

    /**
     * 获取全局配置（中英文内容）详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderGlobalConfigService.selectOrderGlobalConfigById(id));
    }

    /**
     * 新增全局配置（中英文内容）
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:add')")
    @Log(title = "全局配置（中英文内容）", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderGlobalConfig orderGlobalConfig)
    {
        return toAjax(orderGlobalConfigService.insertOrderGlobalConfig(orderGlobalConfig));
    }

    /**
     * 修改全局配置（中英文内容）
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:edit')")
    @Log(title = "全局配置（中英文内容）", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderGlobalConfig orderGlobalConfig)
    {
        return toAjax(orderGlobalConfigService.updateOrderGlobalConfig(orderGlobalConfig));
    }

    /**
     * 删除全局配置（中英文内容）
     */
    @PreAuthorize("@ss.hasPermi('set:siteconfig:remove')")
    @Log(title = "全局配置（中英文内容）", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderGlobalConfigService.deleteOrderGlobalConfigByIds(ids));
    }
}
