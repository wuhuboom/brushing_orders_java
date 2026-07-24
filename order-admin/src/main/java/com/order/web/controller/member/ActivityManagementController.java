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

/** Activity management. */
@RestController
@RequestMapping("/activities")
public class ActivityManagementController extends BaseController
{
    @Autowired
    private ICommerceManagementService service;

    @PreAuthorize("@ss.hasAnyPermi('activity:activity:list,activity:prize:list,activity:account:list,activity:account:prize,activity:partner:list')")
    @GetMapping("/activities/list")
    public TableDataInfo activityList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectActivityList(params));
    }

    @PreAuthorize("@ss.hasPermi('activity:activity:query')")
    @GetMapping("/activities/{id}")
    public AjaxResult activityInfo(@PathVariable Long id)
    {
        return success(service.selectActivityById(id));
    }

    @PreAuthorize("@ss.hasPermi('activity:activity:add')")
    @Log(title = "活动", businessType = BusinessType.INSERT)
    @PostMapping("/activities")
    public AjaxResult addActivity(@RequestBody Map<String, Object> data)
    {
        data.put("createBy", SecurityUtils.getUsername());
        return toAjax(service.insertActivity(data));
    }

    @PreAuthorize("@ss.hasPermi('activity:activity:edit')")
    @Log(title = "活动", businessType = BusinessType.UPDATE)
    @PutMapping("/activities")
    public AjaxResult editActivity(@RequestBody Map<String, Object> data)
    {
        data.put("updateBy", SecurityUtils.getUsername());
        return toAjax(service.updateActivity(data));
    }

    @PreAuthorize("@ss.hasPermi('activity:activity:remove')")
    @Log(title = "活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/activities/{ids}")
    public AjaxResult removeActivity(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteActivityByIds(ids));
    }

    @PreAuthorize("@ss.hasAnyPermi('activity:prize:list,activity:account:list,activity:account:prize')")
    @GetMapping("/prizes/list")
    public TableDataInfo prizeList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectActivityPrizeList(params));
    }

    @PreAuthorize("@ss.hasPermi('activity:prize:query')")
    @GetMapping("/prizes/{id}")
    public AjaxResult prizeInfo(@PathVariable Long id)
    {
        return success(service.selectActivityPrizeById(id));
    }

    @PreAuthorize("@ss.hasPermi('activity:prize:add')")
    @Log(title = "活动奖品", businessType = BusinessType.INSERT)
    @PostMapping("/prizes")
    public AjaxResult addPrize(@RequestBody Map<String, Object> data)
    {
        data.put("createBy", SecurityUtils.getUsername());
        return toAjax(service.insertActivityPrize(data));
    }

    @PreAuthorize("@ss.hasPermi('activity:prize:edit')")
    @Log(title = "活动奖品", businessType = BusinessType.UPDATE)
    @PutMapping("/prizes")
    public AjaxResult editPrize(@RequestBody Map<String, Object> data)
    {
        data.put("updateBy", SecurityUtils.getUsername());
        return toAjax(service.updateActivityPrize(data));
    }

    @PreAuthorize("@ss.hasPermi('activity:prize:remove')")
    @Log(title = "活动奖品", businessType = BusinessType.DELETE)
    @DeleteMapping("/prizes/{ids}")
    public AjaxResult removePrize(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteActivityPrizeByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:list')")
    @GetMapping("/accounts/list")
    public TableDataInfo accountList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectActivityAccountList(params));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:adjust')")
    @Log(title = "活动次数调整", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/{userId}/adjust")
    public AjaxResult adjustTimes(@PathVariable Long userId, @RequestBody Map<String, Object> data)
    {
        return toAjax(service.adjustActivityTimes(userId, data));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:prize')")
    @GetMapping("/accounts/{userId}/prizes/list")
    public TableDataInfo accountPrizeList(@PathVariable Long userId, @RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectAccountPrizeList(userId, params));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:prize')")
    @GetMapping("/account-prizes/{id}")
    public AjaxResult accountPrizeInfo(@PathVariable Long id)
    {
        return success(service.selectAccountPrizeById(id));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:prize')")
    @Log(title = "用户活动奖品设置", businessType = BusinessType.INSERT)
    @PostMapping("/accounts/{userId}/prizes")
    public AjaxResult addAccountPrize(@PathVariable Long userId, @RequestBody Map<String, Object> data)
    {
        return toAjax(service.insertAccountPrize(userId, data));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:prize')")
    @Log(title = "用户活动奖品设置", businessType = BusinessType.UPDATE)
    @PutMapping("/accounts/{userId}/prizes")
    public AjaxResult editAccountPrize(@PathVariable Long userId, @RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateAccountPrize(userId, data));
    }

    @PreAuthorize("@ss.hasPermi('activity:account:prize')")
    @Log(title = "用户活动奖品设置", businessType = BusinessType.DELETE)
    @DeleteMapping("/account-prizes/{ids}")
    public AjaxResult removeAccountPrize(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteAccountPrizeByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('activity:partner:list')")
    @GetMapping("/partners/list")
    public TableDataInfo partnerList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectActivityPartnerList(params));
    }

    @PreAuthorize("@ss.hasPermi('activity:partner:hidden')")
    @Log(title = "活动参与记录显示隐藏", businessType = BusinessType.UPDATE)
    @PutMapping("/partners/hidden/{isHidden}/{ids}")
    public AjaxResult partnerHidden(@PathVariable String isHidden, @PathVariable Long[] ids)
    {
        return toAjax(service.updatePartnerHidden(ids, isHidden));
    }
}
