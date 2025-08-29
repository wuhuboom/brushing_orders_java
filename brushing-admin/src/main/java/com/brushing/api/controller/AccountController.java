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
import java.util.List;

@Tag(
        name = "账户管理",
        description =
                "错误码对照表：\n" +
                        "501: Not in the time frame （不在允许的提现时间范围内）\n" +
                        "502: Less than the minimum withdrawal amount （提现金额小于最低限额）\n" +
                        "503: Exceeding the maximum cash withdrawal （提现金额超过最大限额）\n" +
                        "504: Incorrect transaction password （交易密码错误）\n" +
                        "505: Insufficient number of orders （订单数不足）\n" +
                        "506: There is an open withdrawal order （存在未完成的提现订单）\n" +
                        "507: The balance is insufficient （余额不足）\n" +
                        "508: System configuration is not available （系统配置不可用）\n" +
                        "509: The user does not exist （用户不存在）\n" +
                        "510: User level information is missing （用户等级信息缺失）\n" +
                        "511: Please try again later （请稍后再试）\n" +
                        "512: Withdrawal is not open （提现未开启）\n" +
                        "513: Please check your withdrawal settings （提现方式未设置）\n" +
                        "514: Withdrawal is not possible at the moment （当前不可提现）"
)
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

    // 错误消息常量
    private static final String ERR_NOT_IN_TIME_RANGE = "Not in the time frame";
    private static final String ERR_BELOW_MIN_AMOUNT = "Less than the minimum withdrawal amount";
    private static final String ERR_EXCEED_MAX_AMOUNT = "Exceeding the maximum cash withdrawal";
    private static final String ERR_INVALID_PASSWORD = "Incorrect transaction password";
    private static final String ERR_INSUFFICIENT_ORDERS = "Insufficient number of orders";
    private static final String ERR_PENDING_WITHDRAWAL = "There is an open withdrawal order";
    private static final String ERR_INSUFFICIENT_BALANCE = "The balance is insufficient";
    private static final String ERR_SYSTEM_CONFIG_UNAVAILABLE = "System configuration is not available";
    private static final String ERR_USER_NOT_FOUND = "The user does not exist";
    private static final String ERR_USER_LEVEL_NOT_FOUND = "User level information is missing";
    private static final String ERR_RETRY_LATER = "Please try again later";
    private static final String ERR_WITHDRAWAL_STATUS = "Withdrawal is not open";

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

        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        if (controlConfig == null) {
            return AjaxResult.error(508, ERR_SYSTEM_CONFIG_UNAVAILABLE);
        }

        OrderMemberUser user = memberUserService.findByUsername(username);
        if (user == null) {
            return AjaxResult.error(509, ERR_USER_NOT_FOUND);
        }
        if (user.getUserLevel() == null) {
            return AjaxResult.error(510, ERR_USER_LEVEL_NOT_FOUND);
        }
        if (!user.getWithdrawStatus().equals("0")) {
            String withdrawTip = user.getWithdrawTip();
            String errMsg = StringUtils.isEmpty(withdrawTip) ? "Withdrawal is not possible at the moment" : withdrawTip;
            return AjaxResult.error(514, errMsg);
        }
        if (controlConfig.getWithdrawEnabled().equals("1")) {
            return AjaxResult.error(512, ERR_WITHDRAWAL_STATUS);
        }

        LocalTime now = LocalTime.now();
        if (!isWithinWithdrawTimeRange(now, controlConfig.getWithdrawTimeStart(), controlConfig.getWithdrawTimeEnd())) {
            return AjaxResult.error(501, ERR_NOT_IN_TIME_RANGE);
        }

        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        OrderMemberLevel userLevel = user.getUserLevel();
        BigDecimal minWithdrawAmount = userLevel.getMinWithdrawAmount();
        BigDecimal maxWithdrawAmount = userLevel.getMaxWithdrawAmount();

        if (minWithdrawAmount.compareTo(amount) > 0) {
            return AjaxResult.error(502, ERR_BELOW_MIN_AMOUNT);
        }
        if (amount.compareTo(maxWithdrawAmount) > 0) {
            return AjaxResult.error(503, ERR_EXCEED_MAX_AMOUNT);
        }

        if (StringUtils.isEmpty(dto.getTradePassword()) || StringUtils.isEmpty(user.getTradePassword())) {
            return AjaxResult.error(504, ERR_INVALID_PASSWORD);
        }
        if (!passwordEncoder.matches(dto.getTradePassword(), user.getTradePassword())) {
            return AjaxResult.error(504, ERR_INVALID_PASSWORD);
        }
        if (user.getDealCount() != user.getUserLevel().getOrderCount()) {
            return AjaxResult.error(505, ERR_INSUFFICIENT_ORDERS);
        }
        List<OrderWithdrawal> pendingWithdrawals = withdrawalService.selectOrderWithdrawalByUserId(user.getId());
        if (!pendingWithdrawals.isEmpty()) {
            return AjaxResult.error(506, ERR_PENDING_WITHDRAWAL);
        }
        if (user.getBalance().compareTo(amount) < 0) {
            return AjaxResult.error(507, ERR_INSUFFICIENT_BALANCE);
        }
        if (StringUtils.isEmpty(user.getWithdrawName()) || StringUtils.isEmpty(user.getWithdrawAddress()) || StringUtils.isEmpty(user.getWithdrawType())) {
            return AjaxResult.error(513, "Please check your withdrawal settings");
        }

        BigDecimal balance = user.getBalance();
        BigDecimal subtract = balance.subtract(amount);
        user.setBalance(subtract);
        recordAccountChange(user.getId(), username, "3", balance, new BigDecimal("0").subtract(amount), subtract,
                "用户Id: " + user.getLevelId() + ", 用户名: " + username + ", 提现金额为: " + amount);

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

        user.setTodayWithdrawCount(user.getTotalWithdrawCount() + 1);
        user.setTodayWithdrawCount(user.getTodayWithdrawCount() + 1);
        user.setTodayResetCount(user.getTotalResetCount() + 1);
        user.setTotalResetCount(user.getTotalResetCount() + 1);

        memberUserService.updateOrderMemberUser(user);
        return toAjax(withdrawalService.insertOrderWithdrawal(withdrawal));
    }

    @GetMapping("/getWithdrawals")
    @Operation(summary = "获取用户提现记录", description = "code:编号，amount：提现金额 ，creditedAmount：到账金额，fee:手续费,applicationTime:申请时间,auditTime:审核时间,status:状态 0：通过 1：待审核 2 拒绝 ，withdrawName：名称 ，withdrawAddress：地址 ，withdrawType：钱包名称,withdrawFee：费率")
    public TableDataInfo getWithdrawals(WithrawalPage page, @RequestAttribute("username") String username) {
        OrderMemberUser user = memberUserService.findByUsername(username);
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setUserId(user.getId());
        withdrawal.setStatus(page.getStatus());
        List<OrderWithdrawal> orderWithdrawals = withdrawalService.selectOrderWithdrawalList(withdrawal);
        return getDataTable(orderWithdrawals);
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

    private void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action) {
        String changeNo = generateUniqueChangeNo();
        if (changeNo == null) {
            throw new ServiceException(ERR_RETRY_LATER);
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(action);
        change.setCreateTime(DateUtils.getNowDate());
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
