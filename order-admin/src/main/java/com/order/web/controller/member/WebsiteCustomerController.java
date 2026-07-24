package com.order.web.controller.member;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.member.service.ICommerceManagementService;

/** Website customer management. */
@RestController
@RequestMapping("/websites/customers")
public class WebsiteCustomerController extends BaseController
{
    @Autowired
    private ICommerceManagementService service;

    @PreAuthorize("@ss.hasPermi('website:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectWebsiteCustomerList(params));
    }

    @PreAuthorize("@ss.hasPermi('website:customer:query')")
    @GetMapping("/{id}")
    public AjaxResult info(@PathVariable Long id)
    {
        return success(service.selectWebsiteCustomerById(id));
    }

    @PreAuthorize("@ss.hasPermi('website:customer:add')")
    @Log(title = "官网客户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.insertWebsiteCustomer(data));
    }

    @PreAuthorize("@ss.hasPermi('website:customer:remove')")
    @Log(title = "官网客户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteWebsiteCustomerByIds(ids));
    }
}
