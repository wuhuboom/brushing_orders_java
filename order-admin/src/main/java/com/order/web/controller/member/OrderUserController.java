package com.order.web.controller.member;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.order.common.utils.StringUtils;
import com.order.api.service.AdminMemberCredentialService;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.ITransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.redis.RedisCache;
import com.order.common.enums.BusinessType;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;
import com.order.member.domain.dto.TransactionDto;

/**
 * 订单用户Controller
 * 
 * @author order
 * @date 2025-10-21
 */
@RestController
@RequestMapping("/member/orderuser")
public class OrderUserController extends BaseController
{
    @Autowired
    private IOrderUserService orderUserService;

    @Autowired
    private IGoodsMemberLevelService goodsMemberLevelService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private AdminMemberCredentialService adminMemberCredentialService;

    private static final String MEMBER_LOGIN_FAIL_PREFIX = "member_login_fail:";
    private static final String FRONT_USER_TOKEN_PREFIX = "front:user_tokens:";
    private static final String LEGACY_USER_TOKEN_PREFIX = "user:tokens:";
    private static final String EXCLUDE_USERNAMES_PREFIX = "__exclude__:";

    /**
     * 查询订单用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderUser orderUser, HttpServletRequest request)
    {
        Set<Long> onlineUserIds = new HashSet<>();
        Set<String> onlineUsernames = new HashSet<>();
        collectOnlineMemberIds(redisCache.keys(FRONT_USER_TOKEN_PREFIX + "*"), onlineUserIds);
        collectOnlineMemberNames(redisCache.keys(LEGACY_USER_TOKEN_PREFIX + "*"), onlineUsernames);
        collectOnlineMemberNames(onlineUserIds, onlineUsernames);

        if (!applyOnlineUsernameFilter(
                orderUser,
                request.getParameter("isOnline"),
                onlineUsernames)) {
            return getDataTable(new ArrayList<>());
        }

        startPage();
        List<OrderUser> list = orderUserService.selectOrderUserList(orderUser);
        for (OrderUser user : list) {
            boolean online = onlineUserIds.contains(user.getId())
                    || onlineUsernames.contains(user.getUsername());
            user.setIsOnline(online ? "1" : "0");
        }
        return getDataTable(list);
    }

    private void collectOnlineMemberIds(Collection<String> keys, Set<Long> onlineUserIds)
    {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            String value = StringUtils.substringAfter(key, FRONT_USER_TOKEN_PREFIX);
            try {
                onlineUserIds.add(Long.valueOf(value));
            } catch (NumberFormatException ignored) {
                // Ignore stale or malformed keys instead of breaking the member list.
            }
        }
    }

    private void collectOnlineMemberNames(Collection<String> keys, Set<String> onlineUsernames)
    {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            String username = StringUtils.substringAfter(key, LEGACY_USER_TOKEN_PREFIX);
            if (StringUtils.isNotEmpty(username)) {
                onlineUsernames.add(username);
            }
        }
    }

    private void collectOnlineMemberNames(Set<Long> onlineUserIds, Set<String> onlineUsernames)
    {
        for (Long userId : onlineUserIds) {
            OrderUser user = orderUserService.selectOrderUserById(userId);
            if (user != null && StringUtils.isNotEmpty(user.getUsername())) {
                onlineUsernames.add(user.getUsername());
            }
        }
    }

    private boolean applyOnlineUsernameFilter(
            OrderUser orderUser,
            String onlineStatus,
            Set<String> onlineUsernames)
    {
        if (StringUtils.isEmpty(onlineStatus)) {
            return true;
        }

        Set<String> requestedUsernames = splitUsernames(orderUser.getUsernameList());
        if ("1".equals(onlineStatus)) {
            Set<String> matchingUsernames = new HashSet<>(onlineUsernames);
            if (!requestedUsernames.isEmpty()) {
                matchingUsernames.retainAll(requestedUsernames);
            }
            if (matchingUsernames.isEmpty()) {
                return false;
            }
            orderUser.setUsernameList(String.join(",", matchingUsernames));
            return true;
        }

        if (!requestedUsernames.isEmpty()) {
            requestedUsernames.removeAll(onlineUsernames);
            if (requestedUsernames.isEmpty()) {
                return false;
            }
            orderUser.setUsernameList(String.join(",", requestedUsernames));
        } else if (!onlineUsernames.isEmpty()) {
            orderUser.setUsernameList(EXCLUDE_USERNAMES_PREFIX + String.join(",", onlineUsernames));
        }
        return true;
    }

    private Set<String> splitUsernames(String usernameList)
    {
        Set<String> usernames = new HashSet<>();
        if (StringUtils.isEmpty(usernameList)) {
            return usernames;
        }
        String[] values = StringUtils.split(usernameList, ", \r\n\t");
        if (values != null) {
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    usernames.add(value);
                }
            }
        }
        return usernames;
    }

    @PreAuthorize("@ss.hasAnyPermi('member:orderuser:query,member:orderuser:add,member:orderuser:edit')")
    @GetMapping("/getLevel")
    public AjaxResult getLevel(){
        List<GoodsMemberLevel> list = goodsMemberLevelService.selectGoodsMemberLevelList(null);
        return success(list);
    }

    @PreAuthorize("@ss.hasAnyPermi('member:orderuser:query,member:orderuser:edit,member:bonus:add,member:bonus:edit')")
    @GetMapping("/allUser")
    public AjaxResult getAllUser(){
        List<OrderUser> list = orderUserService.selectOrderUserList(null);
        return success(list);
    }

    /**
     * 导出订单用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:export')")
    @Log(title = "订单用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderUser orderUser)
    {
        List<OrderUser> list = orderUserService.selectOrderUserList(orderUser);
        ExcelUtil<OrderUser> util = new ExcelUtil<OrderUser>(OrderUser.class);
        util.exportExcel(response, list, "订单用户数据");
    }

    /**
     * 获取订单用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderUserService.selectOrderUserById(id));
    }

    /**
     * 新增订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:add')")
    @Log(title = "订单用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderUser orderUser)
    {
        String validationError = validateMemberControls(orderUser);
        if (validationError != null) {
            return AjaxResult.error(validationError);
        }
        OrderUser orderUser1 = orderUserService.selectOrderUserByName(orderUser.getUsername());
        if (StringUtils.isNotNull(orderUser1)){
            return AjaxResult.error("用户名重复");
        }
        if (StringUtils.isNotEmpty(orderUser.getPhoneNumber())
                && orderUserService.existsPhone(orderUser.getPhoneNumber())) {
            return AjaxResult.error("Phone number already exists");
        }
        if (StringUtils.isNotEmpty(orderUser.getParentInviteCode())) {
            OrderUser parent = orderUserService.selectOrderUserByInviteCode(orderUser.getParentInviteCode());
            if (parent == null) {
                return AjaxResult.error("上级邀请码不存在");
            }
            orderUser.setParentId(parent.getId());
        } else if (orderUser.getParentId() == null) {
            orderUser.setParentId(0L);
        }
        Long parentId = orderUser.getParentId();
        if (!parentId.equals(0L))
        {
            OrderUser orderUser2 = orderUserService.selectOrderUserById(parentId);
            if (StringUtils.isNull(orderUser2)){
                return AjaxResult.error("上级用户不存在");
            }
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderUser.setPassword(encoder.encode(orderUser.getPassword()));
        orderUser.setTradePassword(encoder.encode(orderUser.getTradePassword()));
        return toAjax(orderUserService.insertOrderUser(orderUser));
    }

    /**
     * 修改订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @Log(title = "订单用户", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult edit(@RequestBody OrderUser orderUser)
    {
        OrderUser existing = orderUserService.selectOrderUserById(orderUser.getId());
        if (existing == null) {
            return AjaxResult.error("会员不存在");
        }
        String validationError = validateMemberControls(orderUser);
        if (validationError != null) {
            return AjaxResult.error(validationError);
        }
        if (StringUtils.isNotEmpty(orderUser.getParentInviteCode())) {
            OrderUser parent = orderUserService.selectOrderUserByInviteCode(orderUser.getParentInviteCode());
            if (parent == null || parent.getId().equals(orderUser.getId())) {
                return AjaxResult.error("上级邀请码无效");
            }
            orderUser.setParentId(parent.getId());
        }
        Long requestedParentId = orderUser.getParentId() == null
                ? existing.getParentId()
                : orderUser.getParentId();
        String parentError = validateParentChange(existing, requestedParentId);
        if (parentError != null) {
            return AjaxResult.error(parentError);
        }
        if (StringUtils.isNotEmpty(orderUser.getPhoneNumber())
                && !orderUser.getPhoneNumber().equals(existing.getPhoneNumber())
                && orderUserService.existsPhone(orderUser.getPhoneNumber())) {
            return AjaxResult.error("手机号已存在");
        }
        boolean parentChanged = !Objects.equals(existing.getParentId(), requestedParentId);
        orderUser.setParentId(parentChanged ? null : requestedParentId);
        orderUser.setVersion(existing.getVersion());
        orderUser.setTradePassword(null);
        orderUser.setPassword(null);
        orderUser.setUsername(null);
        int updated = orderUserService.updateOrderUser(orderUser);
        if (updated == 0) {
            return AjaxResult.error("会员资料已变化，请刷新后重试");
        }
        if (parentChanged) {
            OrderUser refreshed = orderUserService.selectOrderUserById(orderUser.getId());
            return toAjax(orderUserService.updateParentIdAndAncestors(
                    orderUser.getId(), requestedParentId, refreshed.getVersion()));
        }
        return toAjax(updated);
    }

    private String validateMemberControls(OrderUser orderUser)
    {
        if (orderUser.getWithdrawalPasswordFailLimit() != null && orderUser.getWithdrawalPasswordFailLimit() < 0) {
            return "交易密码失败限制不能小于 0";
        }
        if (orderUser.getWithdrawalPasswordFailCount() != null && orderUser.getWithdrawalPasswordFailCount() < 0) {
            return "交易密码连续失败次数不能小于 0";
        }
        if (orderUser.getMaxSingleWithdrawal() != null && orderUser.getMaxSingleWithdrawal().compareTo(BigDecimal.ZERO) < 0) {
            return "单次最大提现金额不能小于 0";
        }
        return null;
    }

    @PreAuthorize("@ss.hasPermi('member:orderuser:unlock')")
    @Log(title = "会员登录解冻", businessType = BusinessType.OTHER)
    @PutMapping("/unlock")
    public AjaxResult unlock(@RequestBody Long[] ids)
    {
        if (ids == null || ids.length == 0) {
            return AjaxResult.error("请选择需要登录解冻的会员");
        }
        int unlocked = 0;
        for (Long id : ids) {
            OrderUser user = orderUserService.selectOrderUserById(id);
            if (user != null && StringUtils.isNotEmpty(user.getUsername())) {
                redisCache.deleteObject(MEMBER_LOGIN_FAIL_PREFIX + user.getUsername());
                unlocked++;
            }
        }
        return AjaxResult.success("已解冻 " + unlocked + " 个会员");
    }

    //修改密码
    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @PutMapping("/editPassword")
    public AjaxResult editPassword(@RequestBody OrderUser orderUser)
    {
        OrderUser orderUser1 = orderUserService.selectOrderUserById(orderUser.getId());
        orderUser.setVersion(orderUser1.getVersion());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderUser.setPassword(encoder.encode(orderUser.getPassword()));
        return toAjax(orderUserService.updateOrderUser(orderUser));
    }

    //修改交易密码
    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @PutMapping("/editTradePassword")
    public AjaxResult editTradePassword(@RequestBody OrderUser orderUser)
    {
        adminMemberCredentialService.resetTradePassword(
                orderUser.getId(), orderUser.getTradePassword());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @PutMapping("/editParentId")
    public AjaxResult editParentId(@RequestBody OrderUser orderUser)
    {
        OrderUser old = orderUserService.selectOrderUserById(orderUser.getId());
        if (old == null) {
            return error("会员不存在");
        }
        Long newParentId = orderUser.getParentId();
        if (StringUtils.isNotEmpty(orderUser.getParentInviteCode())) {
            OrderUser parent = orderUserService.selectOrderUserByInviteCode(orderUser.getParentInviteCode());
            if (parent == null) {
                return error("上级邀请码不存在");
            }
            newParentId = parent.getId();
        }
        if (newParentId == null) {
            return error("请选择顶级用户或填写上级邀请码");
        }
        String parentError = validateParentChange(old, newParentId);
        if (parentError != null) {
            return error(parentError);
        }
        if (newParentId.equals(old.getParentId())) {
            return error("上级用户未修改");
        }
        if (newParentId.equals(0L)) {
            return toAjax(  orderUserService.updateParentIdAndAncestors(old.getId(), 0L, old.getVersion()));
        }
        return toAjax(orderUserService.updateParentIdAndAncestors(old.getId(), newParentId, old.getVersion()));
    }

    private String validateParentChange(OrderUser member, Long parentId)
    {
        if (parentId == null || parentId.equals(0L)) {
            return null;
        }
        if (member.getId().equals(parentId)) {
            return "不能设置自己为上级用户";
        }
        OrderUser parent = orderUserService.selectOrderUserById(parentId);
        if (parent == null) {
            return "上级用户不存在";
        }
        String ancestors = "," + StringUtils.defaultString(parent.getAncestors()) + ",";
        if (ancestors.contains("," + member.getId() + ",")) {
            return "不能将自己的下级设置为上级用户";
        }
        return null;
    }

    /**
     * 删除订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:remove')")
    @Log(title = "订单用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderUserService.deleteOrderUserByIds(ids));
    }

    @Log(title = "订单用户交易", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @PostMapping("/transaction")
    public AjaxResult transaction(@RequestBody TransactionDto dto)
    {
        // 简单校验
        if (dto == null) {
            return AjaxResult.error("请求体不能为空");
        }
        if (dto.getAmount() == null) {
            return AjaxResult.error("金额不能为空");
        }
        if (dto.getUserId() == null) {
            return AjaxResult.error("userId 不能为空");
        }
        try {
            // 始终通过服务的统一入口处理，服务内部会根据 operationType 调用 recordRecharge 或 recordFlow 等通用方法
            boolean ok = transactionService.processTransaction(dto);
            if (ok) {
                return AjaxResult.success("处理成功");
            } else {
                return AjaxResult.error("处理失败");
            }
        } catch (Exception ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }


    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @GetMapping("/resetOrder/{id}")
    public AjaxResult resetOrder(@PathVariable("id") Long id){
        OrderUser orderUser = orderUserService.selectOrderUserById(id);
        GoodsMemberLevel memberLevel = orderUser.getMemberLevel();
        if (!orderUser.getTaskProgress().equals(memberLevel.getOrderCountPerDay())){
            return AjaxResult.error("需要完成全部任务");
        }
        orderUser.setTaskProgress(0L);
        orderUser.setTodayRest(StringUtils.isNull(orderUser.getTodayRest()) ? 0 : orderUser.getTodayRest()+1);
        orderUser.setTotalRest(StringUtils.isNull(orderUser.getTotalRest()) ? 0 : orderUser.getTotalRest()+1);
        orderUserService.updateOrderUser(orderUser);
        return AjaxResult.success("重置成功");
    }

    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @PostMapping("/giftAmount")
    public AjaxResult giftAmount(@RequestBody TransactionDto dto) {
        OrderUser orderUser = orderUserService.selectOrderUserById(dto.getUserId());
        if (orderUser == null) {
            return AjaxResult.error("用户不存在");
        }
        try {
            transactionService.recordRecharge(
                    dto.getUserId(),
                    dto.getAmount(),
                    BigDecimal.ZERO,
                    "zs",
                    dto.getRemark());
            return AjaxResult.success("赠送成功");
        } catch (Exception ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }


    @PreAuthorize("@ss.hasPermi('member:orderuser:query')")
    @GetMapping("/selectChildrenById")
    public TableDataInfo selectChildrenById(OrderUser orderUser)
    {
        startPage();
        List<OrderUser> list = orderUserService.selectChildrenById(orderUser.getId(), orderUser.getScope());
        return getDataTable(list);
    }


}
