package com.order.api.controller;

import com.github.pagehelper.PageHelper;
import com.order.api.controller.dto.PageDto;
import com.order.api.controller.dto.WithdrawalAccDto;
import com.order.api.controller.dto.WithdrawalDto;
import com.order.api.controller.dto.WithdrawalPage;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.StringUtils;
import com.order.member.domain.*;
import com.order.member.service.*;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import com.order.web.controller.tool.TimeRangeChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "账户管理")
@RestController
@RequestMapping("/api/account")
public class AccountController extends BaseController {

    @Autowired
    private IOrderWithdrawalService orderWithdrawalService;

    @Autowired
    private IOrderWithdrawalTypeService orderWithdrawalTypeService;

    @Autowired
    private IGoodsWithdrawalAccountService accountService;

    @Autowired
    private IOrderUserService orderUserService;

    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    @Autowired
    private IOrderConfigService orderConfigService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IOrderSequenceManagerService orderSequenceManagerService;

    @Autowired
    private IGoodsRechargeRecordService rechargeRecordService;

    @Autowired
    private IGoodsTransactionFlowService transactionFlowService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();




    @Operation(summary = "获取出金类型",description = "type: 0-银行卡 1-钱包,typeName: 类型名称, id: 类型ID")
    @GetMapping("/withdrawalType")
    public AjaxResult withdrawalType(){
        List<OrderWithdrawalType> orderWithdrawalTypes = orderWithdrawalTypeService.selectOrderWithdrawalTypeList(null);
        List<Map<String,Object>> list = new ArrayList<>();
        for (OrderWithdrawalType type : orderWithdrawalTypes) {
            list.add(Map.of("id",type.getId(),"typeName",type.getName(),"type",type.getType()));
        }
        return success(list);
    }

    @PostMapping("/addWalletBank")
    @Operation(summary = "添加银行卡/钱包",description = """
        添加或更新提现账户信息。
        - type: 提现类型，必填。0 表示银行卡，1 表示钱包。
        - withdrawalTypeId: 出金类型 ID，必填。
        - isDefault: 是否默认，选填（0 或 1）。
        - 银行卡相关字段（type=0 时必填）：bankName（银行名称）、bankAccount（银行账号）、accountHolder（账户持有人）。
        - 钱包相关字段（type=1 时必填）：accountName（账户名称）、walletName（钱包名称）、walletAddress（钱包地址）。
        - 其他可选字段：depositType（存款种类）、branchCode（支行代码）、branchName（支行名称）、accountName（账户名称）。
        """)
    public AjaxResult addWalletBank(@RequestBody WithdrawalAccDto dto, @RequestAttribute("username") String username){

        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            return AjaxResult.error(509, "用户不存在");
        }
        String type = dto.getType();
        if (StringUtils.isEmpty(type)||StringUtils.isEmpty(dto.getWithdrawalTypeId())){
            return AjaxResult.error(515, "Form validation failed");
        }
        // 验证字段完整性
        if ("1".equals(type)) {
            // 钱包验证
            if (StringUtils.isEmpty(dto.getAccountName()) || StringUtils.isEmpty(dto.getWalletName()) ||
                    StringUtils.isEmpty(dto.getWalletAddress())) {
                return AjaxResult.error(515, "Form validation failed");
            }
        } else {
            // 银行卡验证（假设 type=0 为银行卡）
            if (StringUtils.isEmpty(dto.getBankName()) || StringUtils.isEmpty(dto.getBankAccount()) ||
                    StringUtils.isEmpty(dto.getAccountHolder())) {
                return AjaxResult.error(515, "Form validation failed");
            }
        }
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setUserId(user.getId());
        account.setType(dto.getType());
        account.setWithdrawalTypeId(dto.getWithdrawalTypeId());
        account.setIsDefault(dto.getIsDefault());
        account.setBankName(dto.getBankName());
        account.setDepositType(dto.getDepositType());
        account.setBranchCode(dto.getBranchCode());
        account.setBranchName(dto.getBranchName());
        account.setBankAccount(dto.getBankAccount());
        account.setAccountHolder(dto.getAccountHolder());
        account.setAccountName(dto.getAccountName());
        account.setWalletName(dto.getWalletName());
        account.setWalletAddress(dto.getWalletAddress());
        int res=0;
        if (StringUtils.isNull(dto.getId())){
            res = accountService.insertGoodsWithdrawalAccount(account);
        }else{
            account.setId(dto.getId());
            res = accountService.updateGoodsWithdrawalAccount(account);
        }
    return toAjax(res);
    }

    @GetMapping("/getUserBankWallet")
    @Operation(summary = "获取用户的银行卡/钱包")
    public AjaxResult getUserBankWallet(@RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            return AjaxResult.error(509, "用户不存在");
        }
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setUserId(user.getId());
        List<GoodsWithdrawalAccount> list = accountService.selectGoodsWithdrawalAccountList(account);
        AjaxResult ajaxResult= new AjaxResult();
        ajaxResult.put("data",list);
        ajaxResult.put("code",200);
        return ajaxResult;
    }

    @Operation(summary = "通过id删除银行卡/钱包")
    @GetMapping("/delBankWallet/{id}")
    public AjaxResult delBankWallet(@PathVariable("id")Long id,@RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            return AjaxResult.error(509, "user not found");
        }
        int i = accountService.deleteGoodsWithdrawalAccountById(id);
        return toAjax(i);
    }


    @Operation(summary = "通过id获取银行卡/钱包")
    @GetMapping("/getBankWallet/{id}")
    public AjaxResult getBankWallet(@PathVariable("id")Long id,@RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            return AjaxResult.error(509, "user not found");
        }
        return success( accountService.selectGoodsWithdrawalAccountById(id));
    }

    @PostMapping("/withdrawal")
    @Operation(summary = "发起提现", description = "amount:金额，tradePassword：交易密码,walletId银行或者钱包id")
    @Transactional
    public AjaxResult withdrawal(@RequestBody WithdrawalDto dto, @RequestAttribute("username") String username) {
       try{
           OrderUser user = orderUserService.selectOrderUserByName(username);
           if (user == null) {
               return AjaxResult.error(509, "user not found");
           }
           GoodsMemberLevel memberLevel = user.getMemberLevel();
           if (memberLevel == null) {
               return AjaxResult.error(510, "user not found");
           }
           //查询是否在抢单时间范围内
           SysTimeZone active = sysTimeZoneService.getActive();
           Optional<Object> configValue = orderConfigService.getConfigValue("trade", "tradeTimeRange");
           boolean currentTimeInRange = TimeRangeChecker.isCurrentTimeInRange(configValue,active.getTzName());
           if (!currentTimeInRange) {
               return AjaxResult.error(501, "当前时间不可提现");
           }
           if (StringUtils.isEmpty(dto.getTradePassword()) || StringUtils.isEmpty(user.getTradePassword())) {
               return AjaxResult.error(504, "交易密码不能为空");
           }
           if (!passwordEncoder.matches(dto.getTradePassword(), user.getTradePassword())) {
               return AjaxResult.error(504, "交易密码错误");
           }
           Long taskCountPerDay = memberLevel.getOrderCountPerDay();
           if (taskCountPerDay > user.getTaskProgress()) {
               return AjaxResult.error(505, "请完成任务后再进行提现");
           }
           BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
           if (user.getBalance().compareTo(amount) < 0) {
               return AjaxResult.error(507, "余额不足");
           }
           if (StringUtils.isNull(dto.getWalletId())){
               return AjaxResult.error(513, "Please check your withdrawal settings");
           }
           List<OrderWithdrawal> orderWithdrawals = orderWithdrawalService.selectOrderWithdrawalByUserId(user.getId());
           if (! orderWithdrawals.isEmpty()) {
               return AjaxResult.error(506, "您有未处理的提现请求，请等待处理完成后再进行新的提现申请");
           }

           BigDecimal balance = user.getBalance();
           BigDecimal subtract = balance.subtract(amount);
           user.setBalance(subtract);
           OrderWithdrawal withdrawal = new OrderWithdrawal();
           withdrawal.setUserId(user.getId());
           withdrawal.setAmount(amount);
           withdrawal.setWithdrawalAccountId(dto.getWalletId());
           withdrawal.setTransactionType("tx");
           withdrawal.setFee(calculateFee(amount, memberLevel.getWithdrawFeeRate()));
           withdrawal.setOrderNumber(orderSequenceManagerService.generateCode("TRADE_NO"));
           int i = orderWithdrawalService.insertOrderWithdrawal(withdrawal);
           orderUserService.updateOrderUser(user);
           transactionService.recordFlow(user.getId(),"tx",new BigDecimal(0).subtract(amount),balance,"");
           return  toAjax(i);
       } catch (Exception e) {
           throw new RuntimeException("Please try again later");
       }

    }

    @GetMapping("/getWithdrawals")
    @Operation(summary = "获取用户提现记录")
    public TableDataInfo getWithdrawals(WithdrawalPage page, @RequestAttribute("username") String username) {
        try{
            OrderUser user = orderUserService.selectOrderUserByName(username);
            if (user == null) {
                throw new RuntimeException("Please try again later");
            }
            PageHelper.startPage(page.getPageNum(), page.getPageSize());
            OrderWithdrawal withdrawal = new OrderWithdrawal();
            withdrawal.setUserId(user.getId());
            withdrawal.setStatus(page.getStatus());
            List<OrderWithdrawal> orderWithdrawals = orderWithdrawalService.selectOrderWithdrawalList(withdrawal);
            return getDataTable(orderWithdrawals);
        }catch (RuntimeException e){
            throw new RuntimeException("Please try again later");
        }
    }

    @GetMapping("/getDeposit")
    @Operation(summary = "获取用户充值记录", description = "amout:金额，orderNumber：编号，createTime:创建时间")
    public TableDataInfo getDeposit(PageDto dto, @RequestAttribute("username") String username) {
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            throw new RuntimeException("Please try again later");
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        GoodsRechargeRecord goodsRechargeRecord=new GoodsRechargeRecord();
        goodsRechargeRecord.setUserId(user.getId());
        List<GoodsRechargeRecord> goodsRechargeRecords = rechargeRecordService.selectGoodsRechargeRecordList(goodsRechargeRecord);
        return getDataTable(goodsRechargeRecords);
    }

    @GetMapping("/getTransactions")
    @Operation(summary = "获取用户交易记录", description = "transactionAmount:金额，serialCode：编号，createTime:创建时间")
    public TableDataInfo getTransactions(PageDto dto, @RequestAttribute("username") String username) {
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (user == null) {
            throw new RuntimeException("Please try again later");
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        GoodsTransactionFlow goodsTransactionFlow= new GoodsTransactionFlow();
        goodsTransactionFlow.setUserId(user.getId());
        List<GoodsTransactionFlow> goodsTransactionFlows = transactionFlowService.selectGoodsTransactionFlowList(goodsTransactionFlow);
        return getDataTable(goodsTransactionFlows);
    }

    private BigDecimal calculateCreditedAmount(BigDecimal amount, BigDecimal feePercent) {
        BigDecimal fee = calculateFee(amount, feePercent);
        return amount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal feePercent) {
        if (feePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feeRate = feePercent.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            return amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

}
