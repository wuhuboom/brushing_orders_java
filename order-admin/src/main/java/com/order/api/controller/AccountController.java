package com.order.api.controller;

import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalRequest;
import com.order.api.controller.dto.PageDto;
import com.order.api.controller.dto.WithdrawalAccDto;
import com.order.api.controller.dto.WithdrawalDto;
import com.order.api.controller.dto.WithdrawalPage;
import com.order.api.service.WithdrawalAccountApplicationService;
import com.order.api.service.WithdrawalApplicationService;
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

    public AccountController(
            WithdrawalAccountApplicationService accountService,
            WithdrawalApplicationService withdrawalService) {
        this.accountService = accountService;
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/withdrawal-types")
    @Operation(summary = "获取可用提现类型")
    public AjaxResult withdrawalTypes() {
        return AjaxResult.success(accountService.listTypes());
    }

    @GetMapping("/withdrawal-accounts")
    @Operation(summary = "获取当前用户提现账户")
    public AjaxResult withdrawalAccounts(@RequestAttribute("userId") Long userId) {
        return AjaxResult.success(accountService.list(userId));
    }

    @PostMapping("/withdrawal-accounts")
    @Operation(summary = "新增提现账户")
    public ResponseEntity<AjaxResult> createWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody WithdrawalAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.success(accountService.create(userId, request)));
    }

    @GetMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "获取提现账户详情")
    public AjaxResult withdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return AjaxResult.success(accountService.get(userId, id));
    }

    @PutMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "修改提现账户")
    public AjaxResult updateWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalAccountRequest request) {
        return AjaxResult.success(accountService.update(userId, id, request));
    }

    @DeleteMapping("/withdrawal-accounts/{id}")
    @Operation(summary = "软删除提现账户")
    public ResponseEntity<Void> deleteWithdrawalAccount(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        accountService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "幂等发起提现")
    public ResponseEntity<AjaxResult> withdraw(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.success(withdrawalService.submit(userId, request)));
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

    // ---- Legacy compatibility routes ----

    @Deprecated
    @GetMapping("/withdrawalType")
    @Operation(summary = "获取出金类型", deprecated = true)
    public AjaxResult withdrawalTypeLegacy() {
        return AjaxResult.success(accountService.listTypes());
    }

    @Deprecated
    @PostMapping("/addWalletBank")
    @Operation(summary = "新增或修改提现账户", deprecated = true)
    public AjaxResult addWalletBankLegacy(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody WithdrawalAccDto dto) {
        WithdrawalAccountRequest request = new WithdrawalAccountRequest(
                dto.getWithdrawalTypeId(),
                "0".equals(dto.getIsDefault()),
                dto.getBankName(),
                dto.getDepositType(),
                dto.getBranchCode(),
                dto.getBranchName(),
                dto.getBankAccount(),
                dto.getAccountHolder(),
                dto.getAccountName(),
                dto.getWalletName(),
                dto.getWalletAddress());
        Object result = dto.getId() == null
                ? accountService.create(userId, request)
                : accountService.update(userId, dto.getId(), request);
        return AjaxResult.success(result);
    }

    @Deprecated
    @GetMapping("/getUserBankWallet")
    @Operation(summary = "获取提现账户", deprecated = true)
    public AjaxResult getUserBankWalletLegacy(@RequestAttribute("userId") Long userId) {
        return AjaxResult.success(accountService.listLegacy(userId));
    }

    @Deprecated
    @GetMapping("/getBankWallet/{id}")
    @Operation(summary = "获取提现账户详情", deprecated = true)
    public AjaxResult getBankWalletLegacy(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return AjaxResult.success(accountService.getLegacy(userId, id));
    }

    @Deprecated
    @GetMapping("/delBankWallet/{id}")
    @Operation(summary = "删除提现账户", deprecated = true)
    public AjaxResult delBankWalletLegacy(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        accountService.delete(userId, id);
        return AjaxResult.success();
    }

    @Deprecated
    @PostMapping("/withdrawal")
    @Operation(summary = "发起提现", deprecated = true)
    public AjaxResult withdrawalLegacy(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody WithdrawalDto dto) {
        return AjaxResult.success(withdrawalService.submitLegacy(
                userId, dto.getAmount(), dto.getTradePassword(), dto.getWalletId()));
    }

    @Deprecated
    @GetMapping("/getWithdrawals")
    @Operation(summary = "查询提现记录", deprecated = true)
    public TableDataInfo getWithdrawalsLegacy(
            @RequestAttribute("userId") Long userId,
            @Valid WithdrawalPage page) {
        return withdrawalService.withdrawalHistory(userId, page.getStatus(),
                defaultValue(page.getPageNum(), 1), defaultValue(page.getPageSize(), 20));
    }

    @Deprecated
    @GetMapping("/getDeposit")
    @Operation(summary = "查询充值记录", deprecated = true)
    public TableDataInfo getDepositLegacy(
            @RequestAttribute("userId") Long userId,
            @Valid PageDto page) {
        return withdrawalService.deposits(userId,
                defaultValue(page.getPageNum(), 1), defaultValue(page.getPageSize(), 20));
    }

    @Deprecated
    @GetMapping("/getTransactions")
    @Operation(summary = "查询资金流水", deprecated = true)
    public TableDataInfo getTransactionsLegacy(
            @RequestAttribute("userId") Long userId,
            @Valid PageDto page) {
        return withdrawalService.transactions(userId,
                defaultValue(page.getPageNum(), 1), defaultValue(page.getPageSize(), 20));
    }

    private int defaultValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
