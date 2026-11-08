package com.order.member.domain.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.order.common.annotation.Excel;
import com.order.common.annotation.Excel.ColumnType;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;

/**
 * Explicit member-export projection. Credentials and identity documents must never
 * inherit their export annotations from the administration entity.
 */
public final class OrderUserExport
{
    @Excel(name = "用户ID", sort = 1, cellType = ColumnType.TEXT)
    private final Long id;

    @Excel(name = "用户名", sort = 2, width = 20)
    private final String username;

    @Excel(name = "手机号", sort = 3, cellType = ColumnType.TEXT, width = 22)
    private final String phoneNumber;

    @Excel(name = "邮箱", sort = 4, width = 28)
    private final String email;

    @Excel(name = "性别", sort = 5, readConverterExp = "0=男,1=女,2=未知")
    private final String gender;

    @Excel(name = "生日", sort = 6, dateFormat = "yyyy-MM-dd")
    private final Date birthday;

    @Excel(name = "VIP等级ID", sort = 7, cellType = ColumnType.TEXT)
    private final Long vipId;

    @Excel(name = "VIP等级", sort = 8)
    private final String vipName;

    @Excel(name = "上级ID", sort = 9, cellType = ColumnType.TEXT)
    private final Long parentId;

    @Excel(name = "上级用户名", sort = 10, width = 20)
    private final String parentUsername;

    @Excel(name = "上级邀请码", sort = 11, cellType = ColumnType.TEXT)
    private final String parentInviteCode;

    @Excel(name = "邀请码", sort = 12, cellType = ColumnType.TEXT)
    private final String inviteCode;

    @Excel(name = "直属下级数量", sort = 13, cellType = ColumnType.NUMERIC)
    private final Integer directChildrenCount;

    @Excel(name = "创建时间", sort = 14, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 24)
    private final Date createTime;

    @Excel(name = "最后登录时间", sort = 15, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 24)
    private final Date lastLoginTime;

    @Excel(name = "最后登录IP", sort = 16, width = 22)
    private final String lastLoginIp;

    @Excel(name = "最后登录地址", sort = 17, width = 30)
    private final String lastLoginAddress;

    @Excel(name = "是否在线", sort = 18, readConverterExp = "1=是,0=否")
    private final String isOnline;

    @Excel(name = "余额", sort = 19, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal balance;

    @Excel(name = "冻结余额", sort = 20, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal frozenBalance;

    @Excel(name = "总余额", sort = 21, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal totalBalance;

    @Excel(name = "底薪", sort = 22, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal baseSalary;

    @Excel(name = "今日佣金", sort = 23, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal todayCommission;

    @Excel(name = "累计佣金", sort = 24, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal totalCommission;

    @Excel(name = "今日上级佣金", sort = 25, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal todayParentCommission;

    @Excel(name = "累计已发放彩金", sort = 26, cellType = ColumnType.NUMERIC, scale = 2, width = 20)
    private final BigDecimal luckyBonus;

    @Excel(name = "待发彩金", sort = 27, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal pendingBonusAmount;

    @Excel(name = "累计充值记录金额", sort = 28, cellType = ColumnType.NUMERIC, scale = 2, width = 24)
    private final BigDecimal totalRechargeAmount;

    @Excel(name = "累计提现申请金额", sort = 29, cellType = ColumnType.NUMERIC, scale = 2, width = 24)
    private final BigDecimal totalWithdrawalAmount;

    @Excel(name = "今日提现次数", sort = 30, cellType = ColumnType.NUMERIC)
    private final Integer todayWithdrawalCount;

    @Excel(name = "单次最大提现金额(0-不限制)", sort = 31, cellType = ColumnType.NUMERIC, scale = 2, width = 32)
    private final BigDecimal maxSingleWithdrawal;

    @Excel(name = "每日提现限额", sort = 32, cellType = ColumnType.NUMERIC, scale = 2, width = 20)
    private final BigDecimal withdrawLimitPerDay;

    @Excel(name = "每日提现次数上限", sort = 33, cellType = ColumnType.NUMERIC, width = 22)
    private final Long withdrawCountPerDay;

    @Excel(name = "任务进度", sort = 34, cellType = ColumnType.NUMERIC)
    private final Long taskProgress;

    @Excel(name = "每日接单次数", sort = 35, cellType = ColumnType.NUMERIC)
    private final Long orderCountPerDay;

    @Excel(name = "每日任务组数", sort = 36, cellType = ColumnType.NUMERIC)
    private final Long taskCountPerDay;

    @Excel(name = "今日重置次数", sort = 37, cellType = ColumnType.NUMERIC)
    private final Integer todayRest;

    @Excel(name = "累计重置次数", sort = 38, cellType = ColumnType.NUMERIC)
    private final Integer totalRest;

    @Excel(name = "工作限额", sort = 39, cellType = ColumnType.NUMERIC, scale = 2)
    private final BigDecimal workLimit;

    @Excel(name = "信誉分", sort = 40, cellType = ColumnType.NUMERIC)
    private final Long reputationScore;

    @Excel(name = "签到天数", sort = 41, cellType = ColumnType.NUMERIC)
    private final Integer signDays;

    @Excel(name = "今日签到次数", sort = 42, cellType = ColumnType.NUMERIC)
    private final Integer todaySignCount;

    @Excel(name = "累计签到次数", sort = 43, cellType = ColumnType.NUMERIC)
    private final Integer totalSignDays;

    @Excel(name = "是否启用", sort = 44, readConverterExp = "0=启用,1=禁用")
    private final String isEnabled;

    @Excel(name = "允许邀请", sort = 45, readConverterExp = "0=是,1=否")
    private final String allowInvite;

    @Excel(name = "是否冻结", sort = 46, readConverterExp = "0=是,1=否")
    private final String isFrozen;

    @Excel(name = "是否假人", sort = 47, readConverterExp = "0=是,1=否")
    private final String isFake;

    @Excel(name = "禁止工作", sort = 48, readConverterExp = "0=是,1=否")
    private final String isBanned;

    @Excel(name = "产品匹配", sort = 49, readConverterExp = "0=启用,1=禁用")
    private final String productMatching;

    @Excel(name = "账户状态", sort = 50, readConverterExp = "0=启用,1=禁用")
    private final String accountStatus;

    @Excel(name = "交易状态", sort = 51, readConverterExp = "0=启用,1=禁用")
    private final String transactionStatus;

    @Excel(name = "提现状态", sort = 52, readConverterExp = "0=启用,1=禁用")
    private final String withdrawalStatus;

    @Excel(name = "协助金提现状态", sort = 53, readConverterExp = "0=启用,1=禁用", width = 20)
    private final String assistWithdrawalStatus;

    @Excel(name = "充值后禁止提现", sort = 54, readConverterExp = "0=是,1=否", width = 20)
    private final String depositBlockWithdrawal;

    @Excel(name = "关闭提现通知", sort = 55, readConverterExp = "0=是,1=否")
    private final String isWithdrawalNotification;

    @Excel(name = "启用Web3授权", sort = 56, readConverterExp = "0=是,1=否", width = 20)
    private final String web3AuthEnabled;

    @Excel(name = "是否无效", sort = 57, readConverterExp = "0=是,1=否")
    private final String isInvalid;

    @Excel(name = "是否活动", sort = 58, readConverterExp = "0=是,1=否")
    private final String isActivity;

    @Excel(name = "任务开始前验证身份", sort = 59, readConverterExp = "0=是,1=否", width = 24)
    private final String verifyIdentityBeforeTask;

    @Excel(name = "是否启用用户合同", sort = 60, readConverterExp = "0=是,1=否", width = 22)
    private final String userContractEnabled;

    @Excel(name = "是否签署用户合同", sort = 61, readConverterExp = "0=是,1=否", width = 22)
    private final String userContractSigned;

    @Excel(name = "是否启用正式合同", sort = 62, readConverterExp = "0=是,1=否", width = 22)
    private final String formalContractEnabled;

    @Excel(name = "是否签署正式合同", sort = 63, readConverterExp = "0=是,1=否", width = 22)
    private final String formalContractSigned;

    @Excel(name = "禁止提现备注", sort = 64, width = 40, wrapText = true)
    private final String withdrawalBlockRemark;

    @Excel(name = "备注", sort = 65, width = 40, wrapText = true)
    private final String remarks;

    public static OrderUserExport from(OrderUser user)
    {
        return new OrderUserExport(user);
    }

    private OrderUserExport(OrderUser user)
    {
        GoodsMemberLevel level = user.getMemberLevel();
        this.id = user.getId();
        this.username = user.getUsername();
        this.phoneNumber = user.getPhoneNumber();
        this.email = user.getEmail();
        this.gender = user.getGender();
        this.birthday = user.getBirthday();
        this.vipId = user.getVipId();
        this.vipName = level == null ? null : level.getName();
        this.parentId = user.getParentId();
        this.parentUsername = user.getParentUsername();
        this.parentInviteCode = user.getParentInviteCode();
        this.inviteCode = user.getInviteCode();
        this.directChildrenCount = user.getDirectChildrenCount();
        this.createTime = user.getCreateTime();
        this.lastLoginTime = user.getLastLoginTime();
        this.lastLoginIp = user.getLastLoginIp();
        this.lastLoginAddress = user.getLastLoginAddress();
        this.isOnline = user.getIsOnline();
        this.balance = user.getBalance();
        this.frozenBalance = user.getFrozenBalance();
        this.totalBalance = zeroIfNull(balance).add(zeroIfNull(frozenBalance));
        this.baseSalary = user.getBaseSalary();
        this.todayCommission = user.getTodayCommission();
        this.totalCommission = user.getTotalCommission();
        this.todayParentCommission = user.getTodayParentCommission();
        this.luckyBonus = user.getLuckyBonus();
        this.pendingBonusAmount = user.getPendingBonusAmount();
        this.totalRechargeAmount = user.getTotalRechargeAmount();
        this.totalWithdrawalAmount = user.getTotalWithdrawalAmount();
        this.todayWithdrawalCount = user.getTodayWithdrawalCount();
        this.maxSingleWithdrawal = user.getMaxSingleWithdrawal();
        this.withdrawLimitPerDay = level == null ? null : level.getWithdrawLimitPerDay();
        this.withdrawCountPerDay = level == null ? null : level.getWithdrawCountPerDay();
        this.taskProgress = user.getTaskProgress();
        this.orderCountPerDay = level == null ? null : level.getOrderCountPerDay();
        this.taskCountPerDay = level == null ? null : level.getTaskCountPerDay();
        this.todayRest = user.getTodayRest();
        this.totalRest = user.getTotalRest();
        this.workLimit = user.getWorkLimit();
        this.reputationScore = user.getReputationScore();
        this.signDays = user.getSignDays();
        this.todaySignCount = user.getTodaySignCount();
        this.totalSignDays = user.getTotalSignDays();
        this.isEnabled = user.getIsEnabled();
        this.allowInvite = user.getAllowInvite();
        this.isFrozen = user.getIsFrozen();
        this.isFake = user.getIsFake();
        this.isBanned = user.getIsBanned();
        this.productMatching = user.getProductMatching();
        this.accountStatus = user.getAccountStatus();
        this.transactionStatus = user.getTransactionStatus();
        this.withdrawalStatus = user.getWithdrawalStatus();
        this.assistWithdrawalStatus = user.getAssistWithdrawalStatus();
        this.depositBlockWithdrawal = user.getDepositBlockWithdrawal();
        this.isWithdrawalNotification = user.getIsWithdrawalNotification();
        this.web3AuthEnabled = user.getWeb3AuthEnabled();
        this.isInvalid = user.getIsInvalid();
        this.isActivity = user.getIsActivity();
        this.verifyIdentityBeforeTask = user.getVerifyIdentityBeforeTask();
        this.userContractEnabled = user.getUserContractEnabled();
        this.userContractSigned = user.getUserContractSigned();
        this.formalContractEnabled = user.getFormalContractEnabled();
        this.formalContractSigned = user.getFormalContractSigned();
        this.withdrawalBlockRemark = user.getWithdrawalBlockRemark();
        this.remarks = user.getRemarks();
    }

    private static BigDecimal zeroIfNull(BigDecimal amount)
    {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
