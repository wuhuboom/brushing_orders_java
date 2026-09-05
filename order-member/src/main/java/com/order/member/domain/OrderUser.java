package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 订单用户对象 order_user
 *
 * @author order
 * @date 2025-10-24
 */
public class OrderUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增长 */
    private Long id;

    /** 用户名 */
    @Excel(name = "用户名")
    private String username;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phoneNumber;

    /** 头像 */
    @Excel(name = "头像")
    private String avatar;

    /** VIPID */
    @Excel(name = "VIPID")
    private Long vipId;

    /** 上级ID */
    @Excel(name = "上级ID")
    private Long parentId;

    /** 余额 */
    @Excel(name = "余额")
    private BigDecimal balance;

    /** 冻结余额 */
    @Excel(name = "冻结余额")
    private BigDecimal frozenBalance;

    /** 底薪 */
    @Excel(name = "底薪")
    private BigDecimal baseSalary;

    /** 任务进度 */
    @Excel(name = "任务进度")
    private Long taskProgress;

    /** 信誉分 */
    @Excel(name = "信誉分")
    private Long reputationScore;

    /** 邀请码 */
    @Excel(name = "邀请码")
    private String inviteCode;


    private String ancestors;

    /** 性别 */
    @Excel(name = "性别")
    private String gender;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 生日 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    @Excel(name = "生日", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthday;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;

    /** 允许邀请 */
    @Excel(name = "允许邀请")
    private String allowInvite;

    /** 是否冻结 */
    @Excel(name = "是否冻结")
    private String isFrozen;

    /** 是否假人 */
    @Excel(name = "是否假人")
    private String isFake;

    /** 是否禁止工作 */
    @Excel(name = "是否禁止工作")
    private String isBanned;

    /** 工作限额 */
    @Excel(name = "工作限额")
    private BigDecimal workLimit;

    /** 关闭提现通知 */
    @Excel(name = "关闭提现通知")
    private String isWithdrawalNotification;

    /** 产品匹配 */
    @Excel(name = "产品匹配")
    private String productMatching;

    /** 账户状态 */
    @Excel(name = "账户状态")
    private String accountStatus;

    /** 交易状态 */
    @Excel(name = "交易状态")
    private String transactionStatus;

    /** 提现状态 */
    @Excel(name = "提现状态")
    private String withdrawalStatus;

    /** 充值后禁止提现 */
    @Excel(name = "充值后禁止提现")
    private String depositBlockWithdrawal;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 版本号 */
    @Excel(name = "版本号")
    private Long version;

    /** 登录密码 */
    @Excel(name = "登录密码")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 交易密码 */
    @Excel(name = "交易密码")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String tradePassword;

    /** 协助金提现状态 */
    @Excel(name = "协助金提现状态")
    private String assistWithdrawalStatus;

    /** 启用 Web3 授权 */
    private String web3AuthEnabled;

    /** 是否无效 */
    private String isInvalid;

    /** 是否活动 */
    private String isActivity;

    /** 禁止提现所需交易密码失败次数，0 表示不限制 */
    private Integer withdrawalPasswordFailLimit;

    /** 提现交易密码连续失败次数 */
    private Integer withdrawalPasswordFailCount;

    /** 单次最大提现金额，0 表示不限制 */
    private BigDecimal maxSingleWithdrawal;

    /** 任务开始前是否验证身份信息 */
    private String verifyIdentityBeforeTask;

    private String userContractEnabled;
    private String userContractSigned;
    private String formalContractEnabled;
    private String formalContractSigned;
    private String withdrawalBlockRemark;
    private Integer signDays;
    private Integer todaySignCount;
    private Integer totalSignDays;


    private Integer todayRest;

    private Integer totalRest;

    private GoodsMemberLevel memberLevel; // 新增的成员变量，用于存储 GoodsMemberLevel 信息

    private String parentUsername;
    private String parentInviteCode;
    private String scope;
    private String keyword;
    private String isOnline;
    private String usernameList;
    private BigDecimal balanceMin;
    private BigDecimal balanceMax;
    private Long reputationMin;
    private Long reputationMax;

    /** 会员身份信息 */
    private String identityType;
    private String identityName;
    private String identityNumber;
    private String identityFrontImage;
    private String identityBackImage;
    private String identityHandheldImage;
    private String identityStatus;
    private String identityRemarks;

    /** 会员专属合同内容 */
    private String userContractContent;
    private String formalContractContent;

    // 额外查询字段
    private java.math.BigDecimal todayCommission;
    private java.math.BigDecimal luckyBonus;
    private java.math.BigDecimal todayParentCommission;
    private String lastLoginIp;
    private String lastLoginAddress;
    private java.util.Date lastLoginTime;
    private Integer directChildrenCount;
    private Integer todayWithdrawalCount;
    private java.math.BigDecimal totalWithdrawalAmount;
    private java.math.BigDecimal totalRechargeAmount;

    public Integer getTodayRest() {
        return todayRest;
    }

    public void setTodayRest(Integer todayRest) {
        this.todayRest = todayRest;
    }

    public Integer getTotalRest() {
        return totalRest;
    }

    public void setTotalRest(Integer totalRest) {
        this.totalRest = totalRest;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getIsOnline() { return isOnline; }
    public void setIsOnline(String isOnline) { this.isOnline = isOnline; }
    public String getUsernameList() { return usernameList; }
    public void setUsernameList(String usernameList) { this.usernameList = usernameList; }
    public BigDecimal getBalanceMin() { return balanceMin; }
    public void setBalanceMin(BigDecimal balanceMin) { this.balanceMin = balanceMin; }
    public BigDecimal getBalanceMax() { return balanceMax; }
    public void setBalanceMax(BigDecimal balanceMax) { this.balanceMax = balanceMax; }
    public Long getReputationMin() { return reputationMin; }
    public void setReputationMin(Long reputationMin) { this.reputationMin = reputationMin; }
    public Long getReputationMax() { return reputationMax; }
    public void setReputationMax(Long reputationMax) { this.reputationMax = reputationMax; }

    public String getIdentityType() { return identityType; }
    public void setIdentityType(String identityType) { this.identityType = identityType; }
    public String getIdentityName() { return identityName; }
    public void setIdentityName(String identityName) { this.identityName = identityName; }
    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }
    public String getIdentityFrontImage() { return identityFrontImage; }
    public void setIdentityFrontImage(String identityFrontImage) { this.identityFrontImage = identityFrontImage; }
    public String getIdentityBackImage() { return identityBackImage; }
    public void setIdentityBackImage(String identityBackImage) { this.identityBackImage = identityBackImage; }
    public String getIdentityHandheldImage() { return identityHandheldImage; }
    public void setIdentityHandheldImage(String identityHandheldImage) { this.identityHandheldImage = identityHandheldImage; }
    public String getIdentityStatus() { return identityStatus; }
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }
    public String getIdentityRemarks() { return identityRemarks; }
    public void setIdentityRemarks(String identityRemarks) { this.identityRemarks = identityRemarks; }
    public String getUserContractContent() { return userContractContent; }
    public void setUserContractContent(String userContractContent) { this.userContractContent = userContractContent; }
    public String getFormalContractContent() { return formalContractContent; }
    public void setFormalContractContent(String formalContractContent) { this.formalContractContent = formalContractContent; }

    public String getParentUsername() {
        return parentUsername;
    }

    public void setParentUsername(String parentUsername) {
        this.parentUsername = parentUsername;
    }

    public String getParentInviteCode() {
        return parentInviteCode;
    }

    public void setParentInviteCode(String parentInviteCode) {
        this.parentInviteCode = parentInviteCode;
    }

    public java.math.BigDecimal getTodayCommission() {
        return todayCommission;
    }

    public void setTodayCommission(java.math.BigDecimal todayCommission) {
        this.todayCommission = todayCommission;
    }

    public java.math.BigDecimal getLuckyBonus() {
        return luckyBonus;
    }

    public void setLuckyBonus(java.math.BigDecimal luckyBonus) {
        this.luckyBonus = luckyBonus;
    }

    public java.math.BigDecimal getTodayParentCommission() {
        return todayParentCommission;
    }

    public void setTodayParentCommission(java.math.BigDecimal todayParentCommission) {
        this.todayParentCommission = todayParentCommission;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getLastLoginAddress() {
        return lastLoginAddress;
    }

    public void setLastLoginAddress(String lastLoginAddress) {
        this.lastLoginAddress = lastLoginAddress;
    }

    public java.util.Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(java.util.Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getDirectChildrenCount() {
        return directChildrenCount;
    }

    public void setDirectChildrenCount(Integer directChildrenCount) {
        this.directChildrenCount = directChildrenCount;
    }

    public Integer getTodayWithdrawalCount() {
        return todayWithdrawalCount;
    }

    public void setTodayWithdrawalCount(Integer todayWithdrawalCount) {
        this.todayWithdrawalCount = todayWithdrawalCount;
    }

    public java.math.BigDecimal getTotalWithdrawalAmount() {
        return totalWithdrawalAmount;
    }

    public void setTotalWithdrawalAmount(java.math.BigDecimal totalWithdrawalAmount) {
        this.totalWithdrawalAmount = totalWithdrawalAmount;
    }

    public java.math.BigDecimal getTotalRechargeAmount() {
        return totalRechargeAmount;
    }

    public void setTotalRechargeAmount(java.math.BigDecimal totalRechargeAmount) {
        this.totalRechargeAmount = totalRechargeAmount;
    }

    // getter 和 setter
    public GoodsMemberLevel getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(GoodsMemberLevel memberLevel) {
        this.memberLevel = memberLevel;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
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

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setVipId(Long vipId)
    {
        this.vipId = vipId;
    }

    public Long getVipId()
    {
        return vipId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getParentId()
    {
        return parentId;
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

    public void setBaseSalary(BigDecimal baseSalary)
    {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getBaseSalary()
    {
        return baseSalary;
    }

    public void setTaskProgress(Long taskProgress)
    {
        this.taskProgress = taskProgress;
    }

    public Long getTaskProgress()
    {
        return taskProgress;
    }

    public void setReputationScore(Long reputationScore)
    {
        this.reputationScore = reputationScore;
    }

    public Long getReputationScore()
    {
        return reputationScore;
    }

    public void setInviteCode(String inviteCode)
    {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode()
    {
        return inviteCode;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getGender()
    {
        return gender;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public void setBirthday(Date birthday)
    {
        this.birthday = birthday;
    }

    public Date getBirthday()
    {
        return birthday;
    }

    public void setIsEnabled(String isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled()
    {
        return isEnabled;
    }

    public void setAllowInvite(String allowInvite)
    {
        this.allowInvite = allowInvite;
    }

    public String getAllowInvite()
    {
        return allowInvite;
    }

    public void setIsFrozen(String isFrozen)
    {
        this.isFrozen = isFrozen;
    }

    public String getIsFrozen()
    {
        return isFrozen;
    }

    public void setIsFake(String isFake)
    {
        this.isFake = isFake;
    }

    public String getIsFake()
    {
        return isFake;
    }

    public void setIsBanned(String isBanned)
    {
        this.isBanned = isBanned;
    }

    public String getIsBanned()
    {
        return isBanned;
    }

    public void setWorkLimit(BigDecimal workLimit)
    {
        this.workLimit = workLimit;
    }

    public BigDecimal getWorkLimit()
    {
        return workLimit;
    }

    public void setIsWithdrawalNotification(String isWithdrawalNotification)
    {
        this.isWithdrawalNotification = isWithdrawalNotification;
    }

    public String getIsWithdrawalNotification()
    {
        return isWithdrawalNotification;
    }

    public void setProductMatching(String productMatching)
    {
        this.productMatching = productMatching;
    }

    public String getProductMatching()
    {
        return productMatching;
    }

    public void setAccountStatus(String accountStatus)
    {
        this.accountStatus = accountStatus;
    }

    public String getAccountStatus()
    {
        return accountStatus;
    }

    public void setTransactionStatus(String transactionStatus)
    {
        this.transactionStatus = transactionStatus;
    }

    public String getTransactionStatus()
    {
        return transactionStatus;
    }

    public void setWithdrawalStatus(String withdrawalStatus)
    {
        this.withdrawalStatus = withdrawalStatus;
    }

    public String getWithdrawalStatus()
    {
        return withdrawalStatus;
    }

    public void setDepositBlockWithdrawal(String depositBlockWithdrawal)
    {
        this.depositBlockWithdrawal = depositBlockWithdrawal;
    }

    public String getDepositBlockWithdrawal()
    {
        return depositBlockWithdrawal;
    }

    public void setRemarks(String remarks)
    {
        this.remarks = remarks;
    }

    public String getRemarks()
    {
        return remarks;
    }

    public void setVersion(Long version)
    {
        this.version = version;
    }

    public Long getVersion()
    {
        return version;
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

    public void setAssistWithdrawalStatus(String assistWithdrawalStatus)
    {
        this.assistWithdrawalStatus = assistWithdrawalStatus;
    }

    public String getAssistWithdrawalStatus()
    {
        return assistWithdrawalStatus;
    }

    public String getWeb3AuthEnabled() { return web3AuthEnabled; }
    public void setWeb3AuthEnabled(String web3AuthEnabled) { this.web3AuthEnabled = web3AuthEnabled; }
    public String getIsInvalid() { return isInvalid; }
    public void setIsInvalid(String isInvalid) { this.isInvalid = isInvalid; }
    public String getIsActivity() { return isActivity; }
    public void setIsActivity(String isActivity) { this.isActivity = isActivity; }
    public Integer getWithdrawalPasswordFailLimit() { return withdrawalPasswordFailLimit; }
    public void setWithdrawalPasswordFailLimit(Integer withdrawalPasswordFailLimit) { this.withdrawalPasswordFailLimit = withdrawalPasswordFailLimit; }
    public Integer getWithdrawalPasswordFailCount() { return withdrawalPasswordFailCount; }
    public void setWithdrawalPasswordFailCount(Integer withdrawalPasswordFailCount) { this.withdrawalPasswordFailCount = withdrawalPasswordFailCount; }
    public BigDecimal getMaxSingleWithdrawal() { return maxSingleWithdrawal; }
    public void setMaxSingleWithdrawal(BigDecimal maxSingleWithdrawal) { this.maxSingleWithdrawal = maxSingleWithdrawal; }
    public String getVerifyIdentityBeforeTask() { return verifyIdentityBeforeTask; }
    public void setVerifyIdentityBeforeTask(String verifyIdentityBeforeTask) { this.verifyIdentityBeforeTask = verifyIdentityBeforeTask; }
    public String getUserContractEnabled() { return userContractEnabled; }
    public void setUserContractEnabled(String userContractEnabled) { this.userContractEnabled = userContractEnabled; }
    public String getUserContractSigned() { return userContractSigned; }
    public void setUserContractSigned(String userContractSigned) { this.userContractSigned = userContractSigned; }
    public String getFormalContractEnabled() { return formalContractEnabled; }
    public void setFormalContractEnabled(String formalContractEnabled) { this.formalContractEnabled = formalContractEnabled; }
    public String getFormalContractSigned() { return formalContractSigned; }
    public void setFormalContractSigned(String formalContractSigned) { this.formalContractSigned = formalContractSigned; }
    public String getWithdrawalBlockRemark() { return withdrawalBlockRemark; }
    public void setWithdrawalBlockRemark(String withdrawalBlockRemark) { this.withdrawalBlockRemark = withdrawalBlockRemark; }
    public Integer getSignDays() { return signDays; }
    public void setSignDays(Integer signDays) { this.signDays = signDays; }
    public Integer getTodaySignCount() { return todaySignCount; }
    public void setTodaySignCount(Integer todaySignCount) { this.todaySignCount = todaySignCount; }
    public Integer getTotalSignDays() { return totalSignDays; }
    public void setTotalSignDays(Integer totalSignDays) { this.totalSignDays = totalSignDays; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("username", getUsername())
                .append("phoneNumber", getPhoneNumber())
                .append("avatar", getAvatar())
                .append("vipId", getVipId())
                .append("parentId", getParentId())
                .append("balance", getBalance())
                .append("frozenBalance", getFrozenBalance())
                .append("baseSalary", getBaseSalary())
                .append("taskProgress", getTaskProgress())
                .append("reputationScore", getReputationScore())
                .append("inviteCode", getInviteCode())
                .append("gender", getGender())
                .append("email", getEmail())
                .append("birthday", getBirthday())
                .append("isEnabled", getIsEnabled())
                .append("allowInvite", getAllowInvite())
                .append("isFrozen", getIsFrozen())
                .append("isFake", getIsFake())
                .append("isBanned", getIsBanned())
                .append("workLimit", getWorkLimit())
                .append("isWithdrawalNotification", getIsWithdrawalNotification())
                .append("productMatching", getProductMatching())
                .append("accountStatus", getAccountStatus())
                .append("transactionStatus", getTransactionStatus())
                .append("withdrawalStatus", getWithdrawalStatus())
                .append("depositBlockWithdrawal", getDepositBlockWithdrawal())
                .append("remarks", getRemarks())
                .append("version", getVersion())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("password", getPassword())
                .append("tradePassword", getTradePassword())
                .append("assistWithdrawalStatus", getAssistWithdrawalStatus())
                .append("web3AuthEnabled", getWeb3AuthEnabled())
                .append("isInvalid", getIsInvalid())
                .append("isActivity", getIsActivity())
                .append("withdrawalPasswordFailLimit", getWithdrawalPasswordFailLimit())
                .append("withdrawalPasswordFailCount", getWithdrawalPasswordFailCount())
                .append("maxSingleWithdrawal", getMaxSingleWithdrawal())
                .append("verifyIdentityBeforeTask", getVerifyIdentityBeforeTask())
                .append("userContractEnabled", getUserContractEnabled())
                .append("userContractSigned", getUserContractSigned())
                .append("formalContractEnabled", getFormalContractEnabled())
                .append("formalContractSigned", getFormalContractSigned())
                .append("withdrawalBlockRemark", getWithdrawalBlockRemark())
                .append("signDays", getSignDays())
                .append("todaySignCount", getTodaySignCount())
                .append("totalSignDays", getTotalSignDays())
                .append("todayCommission", getTodayCommission())
                .append("luckyBonus", getLuckyBonus())
                .append("todayParentCommission", getTodayParentCommission())
                .append("lastLoginIp", getLastLoginIp())
                .append("lastLoginAddress", getLastLoginAddress())
                .append("lastLoginTime", getLastLoginTime())
                .append("directChildrenCount", getDirectChildrenCount())
                .append("todayWithdrawalCount", getTodayWithdrawalCount())
                .append("totalWithdrawalAmount", getTotalWithdrawalAmount())
                .append("totalRechargeAmount", getTotalRechargeAmount())
                .toString();
    }
}
