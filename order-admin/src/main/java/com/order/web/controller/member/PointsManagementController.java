package com.order.web.controller.member;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.common.utils.SecurityUtils;
import com.order.member.service.ICommerceManagementService;

/** Points mall management. */
@RestController
@RequestMapping("/points")
public class PointsManagementController extends BaseController
{
    @Autowired
    private ICommerceManagementService service;

    @PreAuthorize("@ss.hasPermi('points:gift:list')")
    @GetMapping("/gifts/list")
    public TableDataInfo giftList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectGiftList(params));
    }

    @PreAuthorize("@ss.hasPermi('points:gift:query')")
    @GetMapping("/gifts/{id}")
    public AjaxResult giftInfo(@PathVariable Long id)
    {
        return success(service.selectGiftById(id));
    }

    @PreAuthorize("@ss.hasPermi('points:gift:add')")
    @Log(title = "积分礼品", businessType = BusinessType.INSERT)
    @PostMapping("/gifts")
    public AjaxResult addGift(@RequestBody Map<String, Object> data)
    {
        data.put("createBy", SecurityUtils.getUsername());
        return toAjax(service.insertGift(data));
    }

    @PreAuthorize("@ss.hasPermi('points:gift:edit')")
    @Log(title = "积分礼品", businessType = BusinessType.UPDATE)
    @PutMapping("/gifts")
    public AjaxResult editGift(@RequestBody Map<String, Object> data)
    {
        data.put("updateBy", SecurityUtils.getUsername());
        return toAjax(service.updateGift(data));
    }

    @PreAuthorize("@ss.hasPermi('points:gift:remove')")
    @Log(title = "积分礼品", businessType = BusinessType.DELETE)
    @DeleteMapping("/gifts/{ids}")
    public AjaxResult removeGift(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteGiftByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('points:gift:adjust')")
    @Log(title = "积分礼品库存销量调整", businessType = BusinessType.UPDATE)
    @PutMapping("/gifts/{id}/adjust")
    public AjaxResult adjustGift(@PathVariable Long id, @RequestBody Map<String, Object> data)
    {
        return toAjax(service.adjustGift(id, data));
    }

    @PreAuthorize("@ss.hasPermi('points:account:list')")
    @GetMapping("/accounts/list")
    public TableDataInfo accountList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectPointsAccountList(params));
    }

    @PreAuthorize("@ss.hasPermi('points:account:adjust')")
    @Log(title = "积分调整", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/{userId}/adjust")
    public AjaxResult adjustPoints(@PathVariable Long userId, @RequestBody Map<String, Object> data)
    {
        data.put("createBy", SecurityUtils.getUsername());
        return toAjax(service.adjustPoints(userId, data));
    }

    @PreAuthorize("@ss.hasPermi('points:flow:list')")
    @GetMapping("/flows/list")
    public TableDataInfo flowList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectPointsFlowList(params));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:list')")
    @GetMapping("/gift-orders/list")
    public TableDataInfo orderList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectGiftOrderList(params));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:update')")
    @GetMapping("/gift-orders/{id}")
    public AjaxResult orderInfo(@PathVariable Long id)
    {
        return success(service.selectGiftOrderById(id));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:update')")
    @Log(title = "积分订单备注", businessType = BusinessType.UPDATE)
    @PutMapping("/gift-orders")
    public AjaxResult editOrder(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateGiftOrderRemark(data));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:ship')")
    @Log(title = "积分订单发货", businessType = BusinessType.UPDATE)
    @PutMapping("/gift-orders/{id}/ship")
    public AjaxResult ship(@PathVariable Long id)
    {
        return toAjax(service.shipGiftOrder(id));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:receive')")
    @Log(title = "积分订单收货", businessType = BusinessType.UPDATE)
    @PutMapping("/gift-orders/{id}/receive")
    public AjaxResult receive(@PathVariable Long id)
    {
        return toAjax(service.receiveGiftOrder(id));
    }

    @PreAuthorize("@ss.hasPermi('points:giftOrder:cancel')")
    @Log(title = "积分订单取消", businessType = BusinessType.UPDATE)
    @PutMapping("/gift-orders/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id)
    {
        return toAjax(service.cancelGiftOrder(id));
    }
}
