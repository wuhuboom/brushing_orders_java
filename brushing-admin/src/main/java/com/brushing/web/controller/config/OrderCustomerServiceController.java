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
import com.brushing.set.domain.OrderCustomerService;
import com.brushing.set.service.IOrderCustomerServiceService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 客服管理Controller
 * 
 * @author brushing
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/set/customer")
public class OrderCustomerServiceController extends BaseController
{
    @Autowired
    private IOrderCustomerServiceService orderCustomerServiceService;

    /**
     * 查询客服管理列表
     */
    @PreAuthorize("@ss.hasPermi('set:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderCustomerService orderCustomerService)
    {
        startPage();
        List<OrderCustomerService> list = orderCustomerServiceService.selectOrderCustomerServiceList(orderCustomerService);
        return getDataTable(list);
    }

    /**
     * 导出客服管理列表
     */
    @PreAuthorize("@ss.hasPermi('set:customer:export')")
    @Log(title = "客服管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderCustomerService orderCustomerService)
    {
        List<OrderCustomerService> list = orderCustomerServiceService.selectOrderCustomerServiceList(orderCustomerService);
        ExcelUtil<OrderCustomerService> util = new ExcelUtil<OrderCustomerService>(OrderCustomerService.class);
        util.exportExcel(response, list, "客服管理数据");
    }

    /**
     * 获取客服管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('set:customer:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderCustomerServiceService.selectOrderCustomerServiceById(id));
    }

    /**
     * 新增客服管理
     */
    @PreAuthorize("@ss.hasPermi('set:customer:add')")
    @Log(title = "客服管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderCustomerService orderCustomerService)
    {
        return toAjax(orderCustomerServiceService.insertOrderCustomerService(orderCustomerService));
    }

    /**
     * 修改客服管理
     */
    @PreAuthorize("@ss.hasPermi('set:customer:edit')")
    @Log(title = "客服管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderCustomerService orderCustomerService)
    {
        return toAjax(orderCustomerServiceService.updateOrderCustomerService(orderCustomerService));
    }

    /**
     * 删除客服管理
     */
    @PreAuthorize("@ss.hasPermi('set:customer:remove')")
    @Log(title = "客服管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderCustomerServiceService.deleteOrderCustomerServiceByIds(ids));
    }
}
