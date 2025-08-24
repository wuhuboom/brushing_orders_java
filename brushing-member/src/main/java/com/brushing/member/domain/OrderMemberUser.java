package com.brushing.member.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.brushing.member.domain.vo.UserLevel;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 会员用户对象 order_member_user
 * 
 * @author brushing
 * @date 2025-07-30
 */
public class OrderMemberUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户名 */
    @Excel(name = "用户名")
    private String username;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 登录密码 */
    @Excel(name = "登录密码")
    private String password;

    /** 交易密码 */
    @Excel(name = "交易密码")
    private String tradePassword;

    /** 上级用户ID */
    @Excel(name = "上级用户ID")
    private Long parentId;

    /** 祖级 */
    @Excel(name = "祖级")
    private String ancestors;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 信誉分 */
    @Excel(name = "信誉分")
    private Long creditScore;

    /** 可用余额 */
    @Excel(name = "可用余额")
    private BigDecimal balance;

    /** 冻结余额 */
    @Excel(name = "冻结余额")
    private BigDecimal frozenBalance;

    /** 总余额 */
    @Excel(name = "总余额")
    private BigDecimal totalBalance;


    private BigDecimal rechargeNeededForNextLevel;

    /** 邀请码 */
    @Excel(name = "邀请码")
    private String inviteCode;

    /** 最近登录 */
    @Excel(name = "最近登录")
    private String registerIp;

    /** 登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 账户状态 */
    @Excel(name = "账户状态")
    private String accountStatus;

    /** 交易状态 */
    @Excel(name = "交易状态")
    private String tradeStatus;

    /** 提现状态 */
    @Excel(name = "提现状态")
    private String withdrawStatus;

    /** 实名状态 */
    @Excel(name = "实名状态")
    private String realNameStatus;

    /** 真实姓名 */
    @Excel(name = "真实姓名")
    private String realName;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idCardNumber;

    /** 身份证正面 */
    @Excel(name = "身份证正面")
    private String idCardFront;

    /** 身份证反面 */
    @Excel(name = "身份证反面")
    private String idCardBack;

    /** 提现姓名 */
    @Excel(name = "提现姓名")
    private String withdrawName;

    /** 提现地址 */
    @Excel(name = "提现地址")
    private String withdrawAddress;

    /** 提现类型 */
    @Excel(name = "提现类型")
    private String withdrawType;

    /** 是否假人 */
    @Excel(name = "是否假人")
    private String isReal;

    /**
     *性别 0男 1女
     */
    private String sex;

    private Long levelId;


    private OrderMemberLevel userLevel;

    /**
     * 当日佣金
     */
    private BigDecimal commission;

    /** 累计佣金 */
    private BigDecimal allCommission;

    /** 单数 */
    private Integer dealCount;


    // 直属下级人数
    private Integer directSubCount;
    // 所有下级人数
    private Integer allSubCount;


    /** 今日提现次数 */
    private Integer todayWithdrawCount;

    /** 历史提现次数 */
    private Integer totalWithdrawCount;

    /** 今日重置次数 */
    private Integer todayResetCount;

    /** 总重置次数 */
    private Integer totalResetCount;


    /** 卡单数量 */
    private Integer cardNumber;

    /** 提现提示 */
    private String withdrawTip;

    /**
     * 用户头像
     */
    private String avatar;

    private String parentUsername;

    private String parentPhone;


    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getRechargeNeededForNextLevel() {
        return rechargeNeededForNextLevel;
    }

    public void setRechargeNeededForNextLevel(BigDecimal rechargeNeededForNextLevel) {
        this.rechargeNeededForNextLevel = rechargeNeededForNextLevel;
    }

    public String getParentUsername() {
        return parentUsername;
    }

    public void setParentUsername(String parentUsername) {
        this.parentUsername = parentUsername;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getTodayWithdrawCount() {
        return todayWithdrawCount;
    }

    public void setTodayWithdrawCount(Integer todayWithdrawCount) {
        this.todayWithdrawCount = todayWithdrawCount;
    }

    public Integer getTotalWithdrawCount() {
        return totalWithdrawCount;
    }

    public void setTotalWithdrawCount(Integer totalWithdrawCount) {
        this.totalWithdrawCount = totalWithdrawCount;
    }

    public Integer getTodayResetCount() {
        return todayResetCount;
    }

    public void setTodayResetCount(Integer todayResetCount) {
        this.todayResetCount = todayResetCount;
    }

    public Integer getTotalResetCount() {
        return totalResetCount;
    }

    public void setTotalResetCount(Integer totalResetCount) {
        this.totalResetCount = totalResetCount;
    }

    public String getWithdrawTip() {
        return withdrawTip;
    }

    public void setWithdrawTip(String withdrawTip) {
        this.withdrawTip = withdrawTip;
    }

    public Integer getDirectSubCount() {
        return directSubCount;
    }

    public void setDirectSubCount(Integer directSubCount) {
        this.directSubCount = directSubCount;
    }

    public Integer getAllSubCount() {
        return allSubCount;
    }

    public void setAllSubCount(Integer allSubCount) {
        this.allSubCount = allSubCount;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public BigDecimal getAllCommission() {
        return allCommission;
    }

    public void setAllCommission(BigDecimal allCommission) {
        this.allCommission = allCommission;
    }

    public Integer getDealCount() {
        return dealCount;
    }

    public void setDealCount(Integer dealCount) {
        this.dealCount = dealCount;
    }

    public Long getLevelId() {
        return levelId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public OrderMemberLevel getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(OrderMemberLevel userLevel) {
        this.userLevel = userLevel;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUsername(String username) 
    {
        this.username = username;
    }

    public String getUsername() 
    {
        return username;
    }

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }

    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }

    public void setTradePassword(String tradePassword) 
    {
        this.tradePassword = tradePassword;
    }

    public String getTradePassword() 
    {
        return tradePassword;
    }

    public void setParentId(Long parentId) 
    {
        this.parentId = parentId;
    }

    public Long getParentId() 
    {
        return parentId;
    }

    public void setAncestors(String ancestors) 
    {
        this.ancestors = ancestors;
    }

    public String getAncestors() 
    {
        return ancestors;
    }

    public void setEmail(String email) 
    {
        this.email = email;
    }

    public String getEmail() 
    {
        return email;
    }

    public void setCreditScore(Long creditScore) 
    {
        this.creditScore = creditScore;
    }

    public Long getCreditScore() 
    {
        return creditScore;
    }

    public void setBalance(BigDecimal balance) 
    {
        this.balance = balance;
    }

    public BigDecimal getBalance() 
    {
        return balance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance) 
    {
        this.frozenBalance = frozenBalance;
    }

    public BigDecimal getFrozenBalance() 
    {
        return frozenBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) 
    {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getTotalBalance() 
    {
        return totalBalance;
    }

    public void setInviteCode(String inviteCode) 
    {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode() 
    {
        return inviteCode;
    }

    public void setRegisterIp(String registerIp) 
    {
        this.registerIp = registerIp;
    }

    public String getRegisterIp() 
    {
        return registerIp;
    }

    public void setLastLoginTime(Date lastLoginTime) 
    {
        this.lastLoginTime = lastLoginTime;
    }

    public Date getLastLoginTime() 
    {
        return lastLoginTime;
    }

    public void setAccountStatus(String accountStatus) 
    {
        this.accountStatus = accountStatus;
    }

    public String getAccountStatus() 
    {
        return accountStatus;
    }

    public void setTradeStatus(String tradeStatus) 
    {
        this.tradeStatus = tradeStatus;
    }

    public String getTradeStatus() 
    {
        return tradeStatus;
    }

    public void setWithdrawStatus(String withdrawStatus) 
    {
        this.withdrawStatus = withdrawStatus;
    }

    public String getWithdrawStatus() 
    {
        return withdrawStatus;
    }

    public void setRealNameStatus(String realNameStatus) 
    {
        this.realNameStatus = realNameStatus;
    }

    public String getRealNameStatus() 
    {
        return realNameStatus;
    }

    public void setRealName(String realName) 
    {
        this.realName = realName;
    }

    public String getRealName() 
    {
        return realName;
    }

    public void setIdCardNumber(String idCardNumber) 
    {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardNumber() 
    {
        return idCardNumber;
    }

    public void setIdCardFront(String idCardFront) 
    {
        this.idCardFront = idCardFront;
    }

    public String getIdCardFront() 
    {
        return idCardFront;
    }

    public void setIdCardBack(String idCardBack) 
    {
        this.idCardBack = idCardBack;
    }

    public String getIdCardBack() 
    {
        return idCardBack;
    }

    public void setWithdrawName(String withdrawName) 
    {
        this.withdrawName = withdrawName;
    }

    public String getWithdrawName() 
    {
        return withdrawName;
    }

    public void setWithdrawAddress(String withdrawAddress) 
    {
        this.withdrawAddress = withdrawAddress;
    }

    public String getWithdrawAddress() 
    {
        return withdrawAddress;
    }

    public void setWithdrawType(String withdrawType) 
    {
        this.withdrawType = withdrawType;
    }

    public String getWithdrawType() 
    {
        return withdrawType;
    }

    public void setIsReal(String isReal) 
    {
        this.isReal = isReal;
    }

    public String getIsReal() 
    {
        return isReal;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("username", getUsername())
            .append("phone", getPhone())
            .append("password", getPassword())
            .append("tradePassword", getTradePassword())
            .append("parentId", getParentId())
            .append("ancestors", getAncestors())
            .append("email", getEmail())
            .append("creditScore", getCreditScore())
            .append("balance", getBalance())
            .append("frozenBalance", getFrozenBalance())
            .append("totalBalance", getTotalBalance())
            .append("inviteCode", getInviteCode())
            .append("registerIp", getRegisterIp())
            .append("lastLoginTime", getLastLoginTime())
            .append("accountStatus", getAccountStatus())
            .append("tradeStatus", getTradeStatus())
            .append("withdrawStatus", getWithdrawStatus())
            .append("realNameStatus", getRealNameStatus())
            .append("realName", getRealName())
            .append("idCardNumber", getIdCardNumber())
            .append("idCardFront", getIdCardFront())
            .append("idCardBack", getIdCardBack())
            .append("withdrawName", getWithdrawName())
            .append("withdrawAddress", getWithdrawAddress())
            .append("withdrawType", getWithdrawType())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .append("isReal", getIsReal())
            .toString();
    }
}
