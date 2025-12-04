package com.order.web.controller.member;

import java.math.BigDecimal;
import java.util.List;

import com.order.member.domain.OrderUser;
import com.order.member.service.IGoodsTransactionFlowService;
import com.order.member.service.IOrderUserService;
import com.order.member.service.ITransactionService;
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
import com.order.member.domain.OrderWithdrawal;
import com.order.member.service.IOrderWithdrawalService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 提现Controller
 * 
 * @author order
 * @date 2025-11-11
 */
@RestController
@RequestMapping("/member/withdrawal")
public class OrderWithdrawalController extends BaseController
{
    @Autowired
    private IOrderWithdrawalService orderWithdrawalService;

    @Autowired
    private IOrderUserService userService;

    @Autowired
    private ITransactionService transactionService;

    /**
     * 查询提现列表
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
     * 导出提现列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:export')")
    @Log(title = "提现", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderWithdrawal orderWithdrawal)
    {
        List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
        ExcelUtil<OrderWithdrawal> util = new ExcelUtil<OrderWithdrawal>(OrderWithdrawal.class);
        util.exportExcel(response, list, "提现数据");
    }

    /**
     * 获取提现详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderWithdrawalService.selectOrderWithdrawalById(id));
    }

    /**
     * 新增提现
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:add')")
    @Log(title = "提现", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderWithdrawal orderWithdrawal)
    {
        return toAjax(orderWithdrawalService.insertOrderWithdrawal(orderWithdrawal));
    }

    /**
     * 修改提现
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:edit')")
    @Log(title = "提现", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderWithdrawal orderWithdrawal)
    {
        Long id = orderWithdrawal.getId();
        OrderWithdrawal withdrawal = orderWithdrawalService.selectOrderWithdrawalById(id);
        if (!withdrawal.getStatus().equals("1")){
            return error("只能修改待处理的提现申请");
        }
        if (orderWithdrawal.getStatus().equals("2")) {
            OrderUser orderUser = userService.selectOrderUserById(orderWithdrawal.getUserId());
            BigDecimal amount = orderWithdrawal.getAmount();
            BigDecimal balance = orderUser.getBalance();
            orderUser.setBalance(balance.add(amount));
            userService.updateOrderUser(orderUser);
            transactionService.recordFlow(orderUser.getId(),"txbh",amount,balance,"");
        }
        return toAjax(orderWithdrawalService.updateOrderWithdrawal(orderWithdrawal));
    }

    /**
     * 删除提现
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:remove')")
    @Log(title = "提现", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderWithdrawalService.deleteOrderWithdrawalByIds(ids));
    }
}
