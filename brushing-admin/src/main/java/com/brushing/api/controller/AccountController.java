package com.brushing.api.controller;


import com.brushing.api.controller.vo.PageDto;
import com.brushing.api.controller.vo.WithdrawalDto;
import com.brushing.api.controller.vo.WithrawalPage;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.service.IOrderAccountChangeService;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.member.service.IOrderTopupService;
import com.brushing.member.service.IOrderWithdrawalService;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Tag(name = "账户管理")
@RestController
@RequestMapping("/api/account")
public class AccountController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private IOrderTopupService topupService;

    @Autowired
    private IOrderMemberUserService memberUserService;

    @Autowired
    private IOrderWithdrawalService withdrawalService;

    @Autowired
    private IOrderAccountChangeService accountChangeService;


    @Autowired
    private RedisCache redisCache;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ERR_NOT_IN_TIME_RANGE = "Not in the time frame";
    private static final String ERR_BELOW_MIN_AMOUNT = "Less than the minimum withdrawal amount";
    private static final String ERR_EXCEED_MAX_AMOUNT = "Exceeding the maximum cash withdrawal";
    private static final String ERR_INVALID_PASSWORD = "Incorrect transaction password";
    private static final String ERR_INSUFFICIENT_ORDERS = "Insufficient number of orders";
    private static final String ERR_PENDING_WITHDRAWAL = "There is an open withdrawal order";
    private static final String ERR_INSUFFICIENT_BALANCE = "The balance is insufficient";
    private static final String ERR_SYSTEM_CONFIG_UNAVAILABLE = "System configuration is not available";
    private static final String ERR_USER_NOT_FOUND = "The user does not exist";
    private static final String ERR_RETRY_LATER = "Please try again later";


    @GetMapping("/getDeposit")
    @Operation(summary = "获取用户充值记录", description = "amout:金额，username：名称 ，code：编号，createTime:创建时间")
    public TableDataInfo getDeposit(PageDto dto, @RequestAttribute("username") String username) {
        OrderMemberUser byUsername = memberUserService.findByUsername(username);
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<OrderTopup> orderTopups = topupService.selectOrderTopupByUserId(byUsername.getId());
        return getDataTable(orderTopups);
    }

    @PostMapping("/withdrawal")
    @Operation(summary = "发起提现", description = "amount:金额，tradePassword：交易密码")
    public AjaxResult withdrawal(@RequestBody WithdrawalDto dto, @RequestAttribute("username") String username) {
        log.info("用户 {} 发起提现请求，金额: {}", username, dto.getAmount());
        // 1. 获取配置和用户信息
        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        if (controlConfig == null) {
            log.error("系统配置不可用");
            return error(ERR_SYSTEM_CONFIG_UNAVAILABLE);
        }
        OrderMemberUser user = memberUserService.findByUsername(username);
        if (user == null) {
            log.error("用户 {} 不存在", username);
            return error(ERR_USER_NOT_FOUND);
        }
        if (user.getUserLevel() == null) {
            log.error("用户 {} 等级信息缺失", username);
            return error("User level information is missing");
        }
        if (!user.getWithdrawStatus().equals("0")) {
            String withdrawTip = user.getWithdrawTip();
            String errMsg= StringUtils.isEmpty(withdrawTip)?"Withdrawal is not possible at the moment":withdrawTip;
            return error(errMsg);
        }
        if (controlConfig.getWithdrawEnabled().equals("1")){
            return error("Withdrawal is not open");
        }
        // 2. 验证时间范围和金额
        LocalTime now = LocalTime.now();
        if (!isWithinWithdrawTimeRange(now, controlConfig.getWithdrawTimeStart(), controlConfig.getWithdrawTimeEnd())) {
            log.warn("用户 {} 提现时间不在范围内，当前时间: {}", username, now);
            return error(ERR_NOT_IN_TIME_RANGE);
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        OrderMemberLevel userLevel = user.getUserLevel();
        BigDecimal minWithdrawAmount = userLevel.getMinWithdrawAmount();
        BigDecimal maxWithdrawAmount = userLevel.getMaxWithdrawAmount();
        if (minWithdrawAmount.compareTo(amount) > 0) {
            log.warn("用户 {} 提现金额 {} 小于最低限额 {}", username, amount, controlConfig.getMinWithdrawAmount());
            return error(ERR_BELOW_MIN_AMOUNT);
        }
        if (amount.compareTo(maxWithdrawAmount) > 0) {
            log.warn("用户 {} 提现金额 {} 超过最大限额 {}", username, amount, controlConfig.getMaxWithdrawAmount());
            return error(ERR_EXCEED_MAX_AMOUNT);
        }

        // 3. 验证密码和订单状态
        if (StringUtils.isEmpty(dto.getTradePassword()) || StringUtils.isEmpty(user.getTradePassword())) {
            log.warn("用户 {} 交易密码无效", username);
            return error(ERR_INVALID_PASSWORD);
        }
        if (!passwordEncoder.matches(dto.getTradePassword(), user.getTradePassword())) {
            log.warn("用户 {} 交易密码错误", username);
            return error(ERR_INVALID_PASSWORD);
        }
        if (user.getDealCount() != user.getUserLevel().getOrderCount()) {
            log.warn("用户 {} 订单数不足，当前: {}, 要求: {}", username, user.getDealCount(), user.getUserLevel().getOrderCount());
            return error(ERR_INSUFFICIENT_ORDERS);
        }
        List<OrderWithdrawal> pendingWithdrawals = withdrawalService.selectOrderWithdrawalByUserId(user.getId());
        if (!pendingWithdrawals.isEmpty()) {
            log.warn("用户 {} 存在未完成提现订单", username);
            return error(ERR_PENDING_WITHDRAWAL);
        }
        if (user.getBalance().compareTo(amount) < 0) {
            log.warn("用户 {} 余额不足，余额: {}, 提现金额: {}", username, user.getBalance(), amount);
            return error(ERR_INSUFFICIENT_BALANCE);
        }
        if (StringUtils.isEmpty(user.getWithdrawName()) || StringUtils.isEmpty(user.getWithdrawAddress()) || StringUtils.isEmpty(user.getWithdrawType())) {
            return error("Please check your withdrawal settings");
        }
        //更新用户余额
        BigDecimal balance = user.getBalance();
        //得到新的余额
        BigDecimal subtract = balance.subtract(amount);
        user.setBalance(subtract);
        //记录账变信息
        recordAccountChange(user.getId(),username,"3",balance,new BigDecimal("0").subtract(amount),subtract,"用户Id: "+user.getLevelId()+", 用户名: "+username+", 提现金额为: "+amount);

        // 4. 创建提现订单
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        String code = generateUniqueOrderId();
        withdrawal.setCode(code);
        withdrawal.setAmount(amount);
        withdrawal.setUserId(user.getId());
        withdrawal.setCreditedAmount(calculateCreditedAmount(amount, userLevel.getWithdrawFee()));
        withdrawal.setFee(calculateFee(amount, userLevel.getWithdrawFee()));
        withdrawal.setWithdrawFee(userLevel.getWithdrawFee());
        withdrawal.setApplicationTime(DateUtils.getNowDate());
        withdrawal.setWithdrawType(user.getWithdrawType());
        withdrawal.setWithdrawAddress(user.getWithdrawAddress());
        withdrawal.setWithdrawName(user.getWithdrawName());
        // 5. 保存订单
        log.info("用户 {} 提现订单创建成功，订单号: {}", username, code);
        //新增提现次数
        user.setTodayWithdrawCount(user.getTotalWithdrawCount()+1);
        user.setTodayWithdrawCount(user.getTodayWithdrawCount()+1);
        user.setTodayResetCount(user.getTotalResetCount()+1);
        user.setTotalResetCount(user.getTotalResetCount()+1);
        //更新用户信息
        memberUserService.updateOrderMemberUser(user);
        return toAjax(withdrawalService.insertOrderWithdrawal(withdrawal));
    }

    private boolean isWithinWithdrawTimeRange(LocalTime now, LocalTime start, LocalTime end) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal feePercent) {
        if (feePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feeRate = feePercent.divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);
            return amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCreditedAmount(BigDecimal amount, BigDecimal feePercent) {
        BigDecimal fee = calculateFee(amount, feePercent);
        return amount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateUniqueOrderId() {
        String code;
        do {
            code = OrderNoGenerator.generateOrderId();
        } while (withdrawalService.selectOrderWithdrawalByCode(code) != null);
        return code;
    }

    @GetMapping("/getWithdrawals")
    @Operation(summary = "获取用户提现记录", description = "code:编号，amount：提现金额 ，creditedAmount：到账金额，" +
            "fee:手续费,applicationTime:申请时间,auditTime:审核时间,status:状态 0：通过 1：待审核 2 拒绝 ，withdrawName：名称 ，withdrawAddress：地址 ，withdrawType：钱包名称,withdrawFee：费率")
    public TableDataInfo getWithdrawals(WithrawalPage page, @RequestAttribute("username") String username) {
        OrderMemberUser user = memberUserService.findByUsername(username);
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setUserId(user.getId());
        withdrawal.setStatus(page.getStatus());
        List<OrderWithdrawal> orderWithdrawals = withdrawalService.selectOrderWithdrawalList(withdrawal);
        return getDataTable(orderWithdrawals);
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