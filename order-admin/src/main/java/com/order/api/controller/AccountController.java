package com.order.api.controller;

import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalRequest;
import com.order.api.service.WithdrawalAccountApplicationService;
import com.order.api.service.WithdrawalApplicationService;
import com.order.api.service.WithdrawalAccountAccessService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "账户管理")
@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final WithdrawalAccountApplicationService accountService;
    private final WithdrawalApplicationService withdrawalService;
    private final WithdrawalAccountAccessService accessService;

    public AccountController(
            WithdrawalAccountApplicationService accountService,
            WithdrawalApplicationService withdrawalService,
            WithdrawalAccountAccessService accessService) {
        this.accountService = accountService;
        this.withdrawalService = withdrawalService;
        this.accessService = accessService;
    }

    @GetMapping("/withdrawal-types")
    @Operation(summary = "获取可用提现类型")
    public AjaxResult withdrawalTypes() {
        return AjaxResult.success("Success", accountService.listTypes());
    }

    @GetMapping("/withdrawal-accounts")
    @Operation(summary = "获取当前用户提现账户")
    public AjaxResult withdrawalAccounts(@RequestAttribute("userId") Long userId) {
        return AjaxResult.success("Success", accountService.list(userId));
    }

    @PostMapping("/withdrawal-accounts")
    @Operation(summary = "新增提现账户")
    public ResponseEntity<AjaxResult> createWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String token,
            @Valid @RequestBody WithdrawalAccountRequest request) {
        accessService.require(userId, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.success("Success", accountService.create(userId, request)));
    }

    @GetMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "获取提现账户详情")
    public AjaxResult withdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String token) {
        accessService.require(userId, token);
        return AjaxResult.success("Success", accountService.getForEdit(userId, id));
    }

    @PutMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "修改提现账户")
    public AjaxResult updateWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @Valid @RequestBody WithdrawalAccountRequest request) {
        accessService.require(userId, token);
        return AjaxResult.success("Success", accountService.update(userId, id, request));
    }

    @DeleteMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "软删除提现账户")
    public ResponseEntity<Void> deleteWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String token) {
        accessService.require(userId, token);
        accountService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "幂等发起提现")
    public ResponseEntity<AjaxResult> withdraw(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.success("Success", withdrawalService.submit(userId, request)));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "查询当前用户提现记录")
    public TableDataInfo withdrawals(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return withdrawalService.withdrawalHistory(userId, status, pageNum, pageSize);
    }

    @GetMapping("/deposits")
    @Operation(summary = "查询当前用户充值记录")
    public TableDataInfo deposits(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return withdrawalService.deposits(userId, pageNum, pageSize);
    }

    @GetMapping("/transactions")
    @Operation(summary = "查询当前用户资金流水")
    public TableDataInfo transactions(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return withdrawalService.transactions(userId, pageNum, pageSize);
    }
}
