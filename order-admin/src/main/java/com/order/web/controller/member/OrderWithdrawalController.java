package com.order.web.controller.member;

import com.order.api.controller.dto.AccountApiDtos.ReviewStatusRequest;
import com.order.api.controller.dto.AccountApiDtos.SensitiveWithdrawalAccountUpdateRequest;
import com.order.api.service.WithdrawalApplicationService;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.common.utils.poi.ExcelUtil;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.service.IOrderWithdrawalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/member/withdrawal")
public class OrderWithdrawalController extends BaseController {
    private final IOrderWithdrawalService withdrawalQueryService;
    private final WithdrawalApplicationService withdrawalApplicationService;

    public OrderWithdrawalController(
            IOrderWithdrawalService withdrawalQueryService,
            WithdrawalApplicationService withdrawalApplicationService) {
        this.withdrawalQueryService = withdrawalQueryService;
        this.withdrawalApplicationService = withdrawalApplicationService;
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderWithdrawal withdrawal) {
        startPage();
        List<OrderWithdrawal> list = withdrawalQueryService.selectOrderWithdrawalList(withdrawal);
        withdrawalApplicationService.revealAdminAccounts(list);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:export')")
    @Log(title = "提现", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderWithdrawal withdrawal) {
        List<OrderWithdrawal> list = withdrawalQueryService.selectOrderWithdrawalList(withdrawal);
        withdrawalApplicationService.revealAdminAccounts(list);
        new ExcelUtil<>(OrderWithdrawal.class).exportExcel(response, list, "提现数据");
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(withdrawalApplicationService.revealAdminAccount(
                withdrawalQueryService.selectOrderWithdrawalById(id)));
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:sensitive')")
    @GetMapping("/{id}/sensitive-account")
    public AjaxResult getSensitiveAccount(@PathVariable Long id) {
        return success(withdrawalApplicationService.sensitiveAccount(id));
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:sensitive')")
    @Log(title = "提现地址", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/sensitive-account")
    public AjaxResult updateSensitiveAccount(
            @PathVariable Long id,
            @Valid @RequestBody SensitiveWithdrawalAccountUpdateRequest request) {
        withdrawalApplicationService.updateSensitiveAccount(id, request, getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:edit')")
    @Log(title = "提现审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewStatusRequest request) {
        withdrawalApplicationService.review(id, request.status(), request.remarks(), getUsername());
        return success();
    }

    /**
     * Compatibility adapter. Amount, userId and account fields are deliberately ignored.
     */
    @Deprecated
    @PreAuthorize("@ss.hasPermi('member:withdrawal:edit')")
    @Log(title = "提现审核", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderWithdrawal withdrawal) {
        if (withdrawal.getId() == null) {
            return error("提现ID不能为空");
        }
        if (withdrawal.getStatus() != null && !withdrawal.getStatus().isBlank()) {
            withdrawalApplicationService.review(
                    withdrawal.getId(), withdrawal.getStatus(), withdrawal.getRemarks(), getUsername());
            return success();
        }
        if (withdrawal.getIsHidden() != null
                && !"0".equals(withdrawal.getIsHidden())
                && !"1".equals(withdrawal.getIsHidden())) {
            return AjaxResult.error(400, "显示状态无效");
        }
        OrderWithdrawal update = new OrderWithdrawal();
        update.setId(withdrawal.getId());
        update.setRemarks(withdrawal.getRemarks());
        update.setIsHidden(withdrawal.getIsHidden());
        update.setUpdateBy(getUsername());
        return toAjax(withdrawalQueryService.updateOrderWithdrawal(update));
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:add')")
    @PostMapping
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public AjaxResult add() {
        return AjaxResult.error(405, "提现单只能由会员提现流程创建");
    }

    @PreAuthorize("@ss.hasPermi('member:withdrawal:remove')")
    @DeleteMapping("/{ids}")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public AjaxResult remove(@PathVariable Long[] ids) {
        return AjaxResult.error(405, "提现记录属于资金审计数据，禁止删除");
    }
}
