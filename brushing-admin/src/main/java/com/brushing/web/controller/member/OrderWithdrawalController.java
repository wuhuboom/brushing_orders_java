package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.common.utils.DateUtils;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
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
import com.brushing.member.domain.OrderWithdrawal;
import com.brushing.member.service.IOrderWithdrawalService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 提现记录Controller
 * 
 * @author brushing
 * @date 2025-08-07
 */
@RestController
@RequestMapping("/member/withdrawal")
public class OrderWithdrawalController extends BaseController
{
    @Autowired
    private IOrderWithdrawalService orderWithdrawalService;

    @Autowired
    private IOrderMemberUserService userService;

    /**
     * 查询提现记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderWithdrawal orderWithdrawal)
    {
        startPage();
        List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
        return getDataTable(list);
    }

    /**
     * 导出提现记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:export')")
    @Log(title = "提现记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderWithdrawal orderWithdrawal)
    {
        List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
        ExcelUtil<OrderWithdrawal> util = new ExcelUtil<OrderWithdrawal>(OrderWithdrawal.class);
        util.exportExcel(response, list, "提现记录数据");
    }

    /**
     * 获取提现记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderWithdrawalService.selectOrderWithdrawalById(id));
    }

    /**
     * 新增提现记录
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:add')")
    @Log(title = "提现记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderWithdrawal orderWithdrawal)
    {
        return toAjax(orderWithdrawalService.insertOrderWithdrawal(orderWithdrawal));
    }

    /**
     * 修改提现记录
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:edit')")
    @Log(title = "提现记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderWithdrawal orderWithdrawal)
    {
        orderWithdrawal.setAuditTime(DateUtils.getNowDate());
        if (orderWithdrawal.getStatus().equals("0")){
            OrderMemberUser orderMemberUser = userService.selectOrderMemberUserById(orderWithdrawal.getUserId());
            orderMemberUser.setDealCount(0);
            orderMemberUser.setTodayWithdrawCount(orderMemberUser.getTotalWithdrawCount()+1);
            orderMemberUser.setTodayWithdrawCount(orderMemberUser.getTodayWithdrawCount()+1);
            orderMemberUser.setTodayResetCount(orderMemberUser.getTotalResetCount()+1);
            orderMemberUser.setTotalResetCount(orderMemberUser.getTotalResetCount()+1);
            userService.updateOrderMemberUser(orderMemberUser);
        }
        return toAjax(orderWithdrawalService.updateOrderWithdrawal(orderWithdrawal));
    }

    /**
     * 删除提现记录
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:remove')")
    @Log(title = "提现记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderWithdrawalService.deleteOrderWithdrawalByIds(ids));
    }
}
