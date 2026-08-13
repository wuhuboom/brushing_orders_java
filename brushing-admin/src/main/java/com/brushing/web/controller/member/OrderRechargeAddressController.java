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
import com.brushing.member.domain.OrderRechargeAddress;
import com.brushing.member.service.IOrderRechargeAddressService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 充值地址Controller
 *
 * @author brushing
 * @date 2026-01-08
 */
@RestController
@RequestMapping("/member/address")
public class OrderRechargeAddressController extends BaseController
{
    @Autowired
    private IOrderRechargeAddressService orderRechargeAddressService;

    /**
     * 查询充值地址列表
     */
    @PreAuthorize("@ss.hasPermi('member:address:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderRechargeAddress orderRechargeAddress)
    {
        startPage();
        List<OrderRechargeAddress> list = orderRechargeAddressService.selectOrderRechargeAddressList(orderRechargeAddress);
        return getDataTable(list);
    }

    /**
     * 导出充值地址列表
     */
    @PreAuthorize("@ss.hasPermi('member:address:export')")
    @Log(title = "充值地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderRechargeAddress orderRechargeAddress)
    {
        List<OrderRechargeAddress> list = orderRechargeAddressService.selectOrderRechargeAddressList(orderRechargeAddress);
        ExcelUtil<OrderRechargeAddress> util = new ExcelUtil<OrderRechargeAddress>(OrderRechargeAddress.class);
        util.exportExcel(response, list, "充值地址数据");
    }

    /**
     * 获取充值地址详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:address:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderRechargeAddressService.selectOrderRechargeAddressById(id));
    }

    /**
     * 新增充值地址
     */
    @PreAuthorize("@ss.hasPermi('member:address:add')")
    @Log(title = "充值地址", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderRechargeAddress orderRechargeAddress)
    {
        return toAjax(orderRechargeAddressService.insertOrderRechargeAddress(orderRechargeAddress));
    }

    /**
     * 修改充值地址
     */
    @PreAuthorize("@ss.hasPermi('member:address:edit')")
    @Log(title = "充值地址", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderRechargeAddress orderRechargeAddress)
    {
        return toAjax(orderRechargeAddressService.updateOrderRechargeAddress(orderRechargeAddress));
    }

    /**
     * 删除充值地址
     */
    @PreAuthorize("@ss.hasPermi('member:address:remove')")
    @Log(title = "充值地址", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderRechargeAddressService.deleteOrderRechargeAddressByIds(ids));
    }
}
