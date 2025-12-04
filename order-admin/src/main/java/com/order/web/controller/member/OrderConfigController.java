package com.order.web.controller.member;

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
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.enums.BusinessType;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 网站设置Controller
 * 
 * @author order
 * @date 2025-11-16
 */
@RestController
@RequestMapping("/member/orderconfig")
public class OrderConfigController extends BaseController
{
    @Autowired
    private IOrderConfigService orderConfigService;

    /**
     * 查询网站设置列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderConfig orderConfig)
    {
        startPage();
        List<OrderConfig> list = orderConfigService.selectOrderConfigList(orderConfig);
        return getDataTable(list);
    }

    /**
     * 导出网站设置列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:export')")
    @Log(title = "网站设置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderConfig orderConfig)
    {
        List<OrderConfig> list = orderConfigService.selectOrderConfigList(orderConfig);
        ExcelUtil<OrderConfig> util = new ExcelUtil<OrderConfig>(OrderConfig.class);
        util.exportExcel(response, list, "网站设置数据");
    }

    /**
     * 获取网站设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderConfigService.selectOrderConfigById(id));
    }

    /**
     * 新增网站设置
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:add')")
    @Log(title = "网站设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderConfig orderConfig)
    {
        return toAjax(orderConfigService.insertOrderConfig(orderConfig));
    }

    /**
     * 修改网站设置
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:edit')")
    @Log(title = "网站设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderConfig orderConfig)
    {
        return toAjax(orderConfigService.updateOrderConfig(orderConfig));
    }

    /**
     * 删除网站设置
     */
    @PreAuthorize("@ss.hasPermi('member:orderconfig:remove')")
    @Log(title = "网站设置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderConfigService.deleteOrderConfigByIds(ids));
    }
}
