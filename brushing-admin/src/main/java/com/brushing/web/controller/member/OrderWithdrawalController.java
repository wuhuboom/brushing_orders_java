package com.brushing.web.controller.member;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.OrderTopup;
import com.brushing.member.service.IOrderAccountChangeService;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.service.ISysUserService;
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
    private IOrderMemberUserService orderMemberUserService;

    @Autowired
    private IOrderWithdrawalService withdrawalService;

    @Autowired
    private IOrderAccountChangeService accountChangeService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    /**
     * 查询提现记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawal:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderWithdrawal orderWithdrawal)
    {
        Long userId = getUserId();
        SysUser sysUser = userService.selectUserById(userId);
        if (sysUser.getUserName().equals("admin")){
            startPage();
            List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
            return getDataTable(list);
        }
        String agentUser = sysUser.getAgentUser();
        if (StringUtils.isEmpty(agentUser)){
            startPage();
            List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
            return getDataTable(list);
        }
        OrderMemberUser byUsername = orderMemberUserService.findByUsername(agentUser);
        if (StringUtils.isNull(byUsername)){
            startPage();
            List<OrderWithdrawal> list = orderWithdrawalService.selectOrderWithdrawalList(orderWithdrawal);
            return getDataTable(list);
        }
        startPage();
        orderWithdrawal.setAgentUserId(byUsername.getId());
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
        OrderWithdrawal withdrawal = orderWithdrawalService.selectOrderWithdrawalById(orderWithdrawal.getId());
        orderWithdrawal.setAuditTime(DateUtils.getNowDate());
        if (orderWithdrawal.getStatus().equals("0")){
            OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(orderWithdrawal.getUserId());
            OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
            if (orderSiteConfig.getAutoReset().equals("0")){
                orderMemberUser.setDealCount(0);
            }
            orderMemberUserService.updateOrderMemberUser(orderMemberUser);
        }
        if (orderWithdrawal.getStatus().equals("2")){
            OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(orderWithdrawal.getUserId());
            BigDecimal balance = orderMemberUser.getBalance();
            BigDecimal amount = withdrawal.getAmount();
            BigDecimal add = balance.add(amount);
            recordAccountChange(orderMemberUser.getId(),orderMemberUser.getUsername(),"4",balance,amount,add,"提现审核不通过, 操作人ID:" +
                    " "+getUserId()+", 操作人用户名: "+getUsername()+", 提现金额: "+amount);
            orderMemberUser.setBalance(add);
            orderMemberUserService.updateOrderMemberUser(orderMemberUser);

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

    private void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action) {
        String changeNo = generateUniqueChangeNo();
        if (changeNo == null) {
            throw new ServiceException("Please try again later");
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(action);
        change.setCreateTime(new Date());
        accountChangeService.insertOrderAccountChange(change);
    }

    private String generateUniqueChangeNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String changeNo = OrderNoGenerator.generateOrderId();
            if (accountChangeService.selectOrderAccountChangeByCode(changeNo) == null) {
                return changeNo;
            }
        }
        return null;
    }
}
