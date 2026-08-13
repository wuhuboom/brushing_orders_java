package com.brushing.web.controller.member;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.alibaba.fastjson2.JSONObject;
import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.constant.HttpStatus;
import com.brushing.common.core.page.PageDomain;
import com.brushing.common.core.page.TableSupport;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.MessageUtils;
import com.brushing.common.utils.CreditScoreUtils;
import com.brushing.member.domain.OrderFieldSetting;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.vo.TopLevelUserStatVo;
import com.brushing.member.service.IOrderFieldSettingService;
import com.brushing.member.service.IOrderMemberLevelService;
import com.brushing.member.service.IOrderTopupService;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.service.ISysUserService;
import com.brushing.web.controller.member.dto.ScopeUser;
import com.brushing.web.controller.member.dto.ChangeParentDto;
import com.brushing.web.controller.member.dto.TopupDto;
import com.brushing.web.controller.websocket.WebSocketServer;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.framework.init.GeoIpQueryQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageHelper;
import com.brushing.common.annotation.Log;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.enums.BusinessType;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.member.domain.vo.MemberHierarchyStatVo;

/**
 * 会员用户Controller
 * 
 * @author brushing
 * @date 2025-07-30
 */
@RestController
@RequestMapping("/member/member")
public class OrderMemberUserController extends BaseController
{
    private static final int CHANGE_PARENT_MAX_ATTEMPTS = 2;

    private static final String MAX_LEVEL_PRICE_SORT_EXPRESSION =
            "(SELECT MAX(member_sort_level.price) "
            + "FROM order_member_level member_sort_level "
            + "WHERE member_sort_level.price <= u.balance)";
    private static final String NEXT_LEVEL_PRICE_SORT_EXPRESSION =
            "(SELECT MIN(member_sort_next_level.price) "
            + "FROM order_member_level member_sort_next_level "
            + "WHERE member_sort_next_level.price > COALESCE("
            + "(SELECT MAX(member_sort_current_level.price) "
            + "FROM order_member_level member_sort_current_level "
            + "WHERE member_sort_current_level.price <= u.balance), -1))";

    private static final Set<String> MEMBER_LIST_AGGREGATE_SORT_KEYS = Set.of(
            "total_recharge",
            "total_withdraw",
            "diff_amount",
            "withdraw_frozen_amount",
            "today_withdraw_count",
            "direct_sub_count",
            "all_sub_count");
    private static final Set<String> MEMBER_LIST_FIXED_EXPRESSION_SORT_KEYS = Set.of(
            "max_level_price",
            "next_level_price");

    /**
     * Public member-list sort keys mapped to fixed SQL expressions.  Request
     * values are never copied into ORDER BY.  Both camelCase and snake_case
     * inputs normalize to these keys.
     */
    private static final Map<String, String> MEMBER_LIST_SQL_SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "u.id"),
            Map.entry("username", "u.username"),
            Map.entry("phone", "u.phone"),
            Map.entry("password", "u.password"),
            Map.entry("trade_password", "u.trade_password"),
            Map.entry("parent_id", "u.parent_id"),
            Map.entry("avatar", "u.avatar"),
            Map.entry("ancestors", "u.ancestors"),
            Map.entry("sex", "u.sex"),
            Map.entry("email", "u.email"),
            Map.entry("credit_score", "u.credit_score"),
            Map.entry("balance", "u.balance"),
            Map.entry("frozen_balance", "u.frozen_balance"),
            Map.entry("total_balance", "total_balance"),
            Map.entry("invite_code", "u.invite_code"),
            Map.entry("register_ip", "u.register_ip"),
            Map.entry("last_login_time", "u.last_login_time"),
            Map.entry("account_status", "u.account_status"),
            Map.entry("trade_status", "u.trade_status"),
            Map.entry("withdraw_status", "u.withdraw_status"),
            Map.entry("real_name_status", "u.real_name_status"),
            Map.entry("real_name", "u.real_name"),
            Map.entry("id_card_number", "u.id_card_number"),
            Map.entry("id_card_front", "u.id_card_front"),
            Map.entry("id_card_back", "u.id_card_back"),
            Map.entry("withdraw_name", "u.withdraw_name"),
            Map.entry("withdraw_address", "u.withdraw_address"),
            Map.entry("withdraw_type", "u.withdraw_type"),
            Map.entry("create_time", "u.create_time"),
            Map.entry("remark", "u.remark"),
            Map.entry("is_real", "u.is_real"),
            Map.entry("level_id", "u.level_id"),
            Map.entry("commission", "u.commission"),
            Map.entry("all_commission", "u.all_commission"),
            Map.entry("deal_count", "u.deal_count"),
            Map.entry("card_number", "u.card_number"),
            Map.entry("card_amount", "u.card_amount"),
            Map.entry("task_status", "u.task_status"),
            Map.entry("version", "u.version"),
            Map.entry("total_withdraw_count", "u.total_withdraw_count"),
            Map.entry("today_reset_count", "u.today_reset_count"),
            Map.entry("total_reset_count", "u.total_reset_count"),
            Map.entry("withdraw_tip", "u.withdraw_tip"),
            Map.entry("parent_username", "parent_user.username"),
            Map.entry("parent_phone", "parent_user.phone"),
            Map.entry("icon", "l.icon"),
            Map.entry("name_zh", "l.name_zh"),
            Map.entry("name_en", "l.name_en"),
            Map.entry("price", "l.price"),
            Map.entry("auto_upgrade_invite_count", "l.auto_upgrade_invite_count"),
            Map.entry("commission_ratio", "l.commission_ratio"),
            Map.entry("streak_commission_ratio", "l.streak_commission_ratio"),
            Map.entry("min_balance", "l.min_balance"),
            Map.entry("order_count", "l.order_count"),
            Map.entry("withdraw_count", "l.withdraw_count"),
            Map.entry("withdraw_limit", "l.withdraw_limit"),
            Map.entry("min_withdraw_amount", "l.min_withdraw_amount"),
            Map.entry("max_withdraw_amount", "l.max_withdraw_amount"),
            Map.entry("withdraw_fee", "l.withdraw_fee"),
            Map.entry("withdraw_order_per_day", "l.withdraw_order_per_day"),
            Map.entry("description_zh", "l.description_zh"),
            Map.entry("description_en", "l.description_en"),
            Map.entry("level_time", "l.create_time"),
            Map.entry("max_level_price", MAX_LEVEL_PRICE_SORT_EXPRESSION),
            Map.entry("next_level_price", NEXT_LEVEL_PRICE_SORT_EXPRESSION),
            Map.entry("recharge_needed_for_next_level", "recharge_needed_for_next_level"));

    @Autowired
    private IOrderMemberUserService orderMemberUserService;

    @Autowired
    private IOrderMemberLevelService orderMemberLevelService;

    @Autowired
    private IOrderTopupService orderTopupService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    @Autowired
    private IOrderFieldSettingService orderFieldSettingService;

    @Autowired
    private GeoIpQueryQueryService queryService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查询会员用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderMemberUser orderMemberUser)
    {
        PageDomain pageRequest = TableSupport.buildPageRequest();
        MemberListSort sort = resolveMemberListSort(pageRequest);
        Long userId = getUserId();
        SysUser sysUser = userService.selectUserById(userId);
        if (!sysUser.getUserName().equals("admin")) {
            String agentUser = sysUser.getAgentUser();
            if (StringUtils.isNotEmpty(agentUser)) {
                OrderMemberUser byUsername = orderMemberUserService.findByUsername(agentUser);
                if (StringUtils.isNotNull(byUsername)) {
                    orderMemberUser.setAgentUserId(byUsername.getId());
                }
            }
        }

        startMemberListPage(pageRequest, sort);
        List<OrderMemberUser> list = sort.aggregate()
                ? orderMemberUserService.selectOrderMemberUserListByAggregateSort(
                        orderMemberUser, sort.key(), sort.direction())
                : orderMemberUserService.selectOrderMemberUserList(orderMemberUser);
        return getDataTable(list);
    }

    private void startMemberListPage(PageDomain pageRequest, MemberListSort sort)
    {
        if (sort.isDefault() || sort.aggregate()) {
            PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize())
                    .setReasonable(pageRequest.getReasonable());
            return;
        }
        if (sort.fixedExpression()) {
            // PageHelper's public ORDER BY validator rejects every expression
            // containing parentheses.  This bypass is limited to the two
            // compile-time SQL expressions above; no request text reaches it.
            PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize())
                    .setReasonable(pageRequest.getReasonable())
                    .setUnsafeOrderBy(sort.sqlOrderBy());
            return;
        }
        PageHelper.startPage(
                        pageRequest.getPageNum(),
                        pageRequest.getPageSize(),
                        sort.sqlOrderBy())
                .setReasonable(pageRequest.getReasonable());
    }

    static MemberListSort resolveMemberListSort(PageDomain pageRequest)
    {
        String requestedColumn = StringUtils.trim(pageRequest.getOrderByColumn());
        if (StringUtils.isEmpty(requestedColumn)) {
            return MemberListSort.defaultSort();
        }
        // Reject multi-column input, table qualification and punctuation before
        // normalization.  A fixed ID tie-breaker is added internally instead.
        if (!requestedColumn.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw invalidMemberListSort("Unsupported member list sort field");
        }

        String key = StringUtils.toUnderScoreCase(requestedColumn)
                .toLowerCase(Locale.ROOT);
        String requestedDirection = StringUtils.trim(pageRequest.getIsAsc());
        String direction = StringUtils.isEmpty(requestedDirection)
                ? "asc" : requestedDirection.toLowerCase(Locale.ROOT);
        if ("ascending".equals(direction)) {
            direction = "asc";
        } else if ("descending".equals(direction)) {
            direction = "desc";
        }
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw invalidMemberListSort("Unsupported member list sort direction");
        }

        if (MEMBER_LIST_AGGREGATE_SORT_KEYS.contains(key)) {
            return new MemberListSort(key, null, direction, true, false);
        }
        String sqlColumn = MEMBER_LIST_SQL_SORT_COLUMNS.get(key);
        if (sqlColumn == null) {
            throw invalidMemberListSort("Unsupported member list sort field");
        }
        String tieBreaker = "u.id".equals(sqlColumn) ? "" : ", u.id desc";
        return new MemberListSort(
                key,
                sqlColumn + " " + direction + tieBreaker,
                direction,
                false,
                MEMBER_LIST_FIXED_EXPRESSION_SORT_KEYS.contains(key));
    }

    private static ServiceException invalidMemberListSort(String message)
    {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    static record MemberListSort(String key,
                                 String sqlOrderBy,
                                 String direction,
                                 boolean aggregate,
                                 boolean fixedExpression)
    {
        static MemberListSort defaultSort()
        {
            return new MemberListSort(null, null, "desc", false, false);
        }

        boolean isDefault()
        {
            return key == null;
        }
    }

    @GetMapping("/scopeList")
    public TableDataInfo scopeList(ScopeUser user)
    {
        startPage();
        List<OrderMemberUser> list = orderMemberUserService.selectMembersByScope(user.getUserId(),user.getScope(), user.getSubUsername(), user.getSubPhone());
        return getDataTable(list);
    }

    /**
     * 导出会员用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:member:export')")
    @Log(title = "会员用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderMemberUser orderMemberUser)
    {
        List<OrderMemberUser> list = orderMemberUserService.selectOrderMemberUserList(orderMemberUser);
        ExcelUtil<OrderMemberUser> util = new ExcelUtil<OrderMemberUser>(OrderMemberUser.class);
        util.exportExcel(response, list, "会员用户数据");
    }

    /**
     * 获取会员用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:member:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(id);
        return success(orderMemberUser);
    }

    /**
     * 新增会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:add')")
    @Log(title = "会员用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderMemberUser orderMemberUser)
    {
        OrderMemberUser byUsername = orderMemberUserService.findByUsername(orderMemberUser.getUsername());
        if (StringUtils.isNotNull(byUsername)){
            return error(MessageUtils.message("member.user.username.duplicate"));
        }
        Boolean b = orderMemberUserService.existsPhone(orderMemberUser.getPhone());
        if (b){
            return error("Phone number already exists");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderMemberUser.setPassword(encoder.encode(orderMemberUser.getPassword()));
        OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (orderSiteConfig != null||orderSiteConfig.getNewUserCanTask().equals("0")){
            orderMemberUser.setTaskStatus("0");
        }else {
            orderMemberUser.setTaskStatus("1");
        }
        if(StringUtils.isNotEmpty(orderMemberUser.getTradePassword())){
            orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        }
        return toAjax(orderMemberUserService.insertOrderMemberUser(orderMemberUser));
    }

    /**
     * 修改会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:edit')")
    @Log(title = "会员用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderMemberUser orderMemberUser)
    {
        if (orderMemberUser.getId() == null) {
            return error("User ID cannot be empty");
        }
        if (orderMemberUser.getCreditScore() != null
                && !CreditScoreUtils.isValidScore(orderMemberUser.getCreditScore())) {
            return error("Credit score must be an integer between 1 and 100");
        }
        OrderMemberUser orderMemberUser2 = orderMemberUserService.selectOrderMemberUserById(orderMemberUser.getId());
        if (orderMemberUser2 == null) {
            return error("The user does not exist");
        }
        orderMemberUser.setVersion(orderMemberUser2.getVersion());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (StringUtils.isNotEmpty(orderMemberUser.getPassword())){
            orderMemberUser.setPassword(encoder.encode(orderMemberUser.getPassword()));
        }
        if(StringUtils.isNotEmpty(orderMemberUser.getTradePassword())){
            orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        }
        int i = orderMemberUserService.updateOrderMemberUser(orderMemberUser);
        JSONObject jsonObject= new JSONObject();
        jsonObject.put("type","1");
        jsonObject.put("data",orderMemberUser);
        WebSocketServer.sendMessageToUser(orderMemberUser.getId(),jsonObject.toJSONString());
        return toAjax(i);
    }

    /**
     * Move a member and their complete descendant branch to another parent.
     */
    @PreAuthorize("@ss.hasPermi('member:member:changeParent')")
    @Log(title = "修改会员上级", businessType = BusinessType.UPDATE)
    @PutMapping("/changeParent")
    public AjaxResult changeParent(@Validated @RequestBody ChangeParentDto dto)
    {
        int rows = changeParentWithOneTransientLockRetry(dto);
        sendUserInfoMessage(dto.getMemberId(), "1");
        return toAjax(rows);
    }

    /**
     * The service call crosses a Spring transactional proxy.  A lock failure is
     * therefore observed here only after the failed transaction has rolled
     * back, so the second invocation starts in a fresh transaction.
     */
    private int changeParentWithOneTransientLockRetry(ChangeParentDto dto)
    {
        for (int attempt = 1; attempt <= CHANGE_PARENT_MAX_ATTEMPTS; attempt++) {
            try {
                return orderMemberUserService.changeParent(
                        dto.getMemberId(), dto.getParentIdentifier(), dto.getVersion());
            } catch (PessimisticLockingFailureException exception) {
                if (attempt == CHANGE_PARENT_MAX_ATTEMPTS) {
                    logger.warn("Concurrent hierarchy update failed twice for member {}",
                            dto.getMemberId(), exception);
                    throw new ServiceException(
                            MessageUtils.message("member.user.parent.concurrent_conflict"),
                            HttpStatus.CONFLICT);
                }
                logger.warn("Transient hierarchy lock conflict for member {}; retrying once",
                        dto.getMemberId(), exception);
            }
        }
        throw new IllegalStateException("Unreachable change-parent retry state");
    }

    /**
     * 删除会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:remove')")
    @Log(title = "会员用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderMemberUserService.deleteOrderMemberUserByIds(ids));
    }

    /**
     * 获取等级列表
     * @return
     */
    @GetMapping("/levelList")
    public AjaxResult levelList()
    {
        List<OrderMemberLevel> list = orderMemberLevelService.selectOrderMemberLevelList(new OrderMemberLevel());
        return success(list);
    }

    /**
     * 上下分操作
     * @param dto
     * @return
     */
    @PostMapping("/topupAmount")
    public AjaxResult topupAmount(@RequestBody TopupDto dto, HttpServletRequest request) {
        String ipAddr = IpUtils.getIpAddress(request);
        String ipAddress = null;
        try {
            ipAddress = queryService.queryByIp(ipAddr);
        } catch (Exception e) {
            // IGNORE
        }
        int i = orderTopupService.upOrDown(dto.getUserId(), new BigDecimal(dto.getAmount()), dto.getType(), ipAddr, ipAddress);
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    @PostMapping("/upAmount")
    public AjaxResult upAmount(@RequestBody TopupDto dto){
        int i = orderTopupService.uPamount(dto.getUserId(), new BigDecimal(dto.getAmount()), getUserId(), getUsername());
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    @PostMapping("/updateAmount")
    public AjaxResult updateAmount(@RequestBody TopupDto dto){
        int i = orderTopupService.updateAmount(dto.getUserId(), new BigDecimal(dto.getAmount()), getUserId(), getUsername());
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    /**
     * 重置单数
     * @return
     */
    @GetMapping("/restDealCount/{id}")
    public AjaxResult restDealCount(@PathVariable("id") Long userId){
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(userId);
        if (orderMemberUser.getDealCount() < orderMemberUser.getUserLevel().getOrderCount() ){
            return error(MessageUtils.message("member.user.reset.not_reached"));
        }
        orderMemberUser.setDealCount(0);
        orderMemberUser.setTotalResetCount(orderMemberUser.getTotalResetCount()+1);
        orderMemberUser.setTodayResetCount(orderMemberUser.getTodayResetCount()+1);
        int i = orderMemberUserService.updateOrderMemberUser(orderMemberUser);
        sendUserInfoMessage(userId,"1");
        return toAjax(i);
    }


    public void sendUserInfoMessage(Long userId, String type) {
        // 获取用户信息
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(userId);

        if (orderMemberUser != null) {
            // 创建 JSON 对象，构建消息
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);  // 可以根据传入的 type 动态改变消息类型
            jsonObject.put("data", orderMemberUser);  // 包含用户信息

            // 发送消息
            WebSocketServer.sendMessageToUser(orderMemberUser.getId(), jsonObject.toJSONString());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }

    /**
     * 获取统计信息
     * @return
     */
    @GetMapping("/getDashboardData")
    public AjaxResult getDashboardData(){

        return success(orderMemberUserService.getDashboardData());
    }

    /**
     * 会员层级统计
     * @param username
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/hierarchy/stats")
    public AjaxResult hierarchyStats(String username, String beginTime, String endTime) {
        LocalDateTime start = LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
        List<MemberHierarchyStatVo> stats = orderMemberUserService.getHierarchyStats(username, start, end);
        return success(stats);
    }

    /**
     * 查询顶级节点统计（parent_id = 0）
     * @param username 可选，按用户名过滤
     * @param beginTime 时间范围开始，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime 时间范围结束，格式 yyyy-MM-dd HH:mm:ss
     * @return
     */
    @GetMapping("/hierarchy/topLevelStats")
    public TableDataInfo topLevelStats(String username, String beginTime, String endTime) {
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (beginTime != null && !beginTime.isEmpty()) {
            start = java.time.LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER);
        }
        if (endTime != null && !endTime.isEmpty()) {
            end = java.time.LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
        }
        startPage();
        List<TopLevelUserStatVo> stats = orderMemberUserService.getTopLevelStats(username, start, end);
        return getDataTable(stats);
    }

    /**
     * 查询指定用户的下级节点统计信息
     * @param userId 指定的用户ID
     * @param beginTime 时间范围开始，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime 时间范围结束，格式 yyyy-MM-dd HH:mm:ss
     * @return
     */
    @GetMapping("/hierarchy/subStats")
    public TableDataInfo subStats(Long userId, String beginTime, String endTime) {
        if (userId == null) {
            return new TableDataInfo(); // or throw exception/return empty
        }
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (beginTime != null && !beginTime.isEmpty()) {
            start = java.time.LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER);
        }
        if (endTime != null && !endTime.isEmpty()) {
            end = java.time.LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
        }
        startPage();
        List<TopLevelUserStatVo> stats = orderMemberUserService.getSubStatsByUserId(userId, start, end);
        return getDataTable(stats);
    }

    /**
     * 返点统计
     * @param username
     * @return
     */
    @GetMapping("/rebate/stats")
    public AjaxResult rebateStats(String username) {
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(400, "username is required");
        }
        com.brushing.member.domain.vo.RebateStatVo vo = orderMemberUserService.getRebateStats(username);
        return success(vo);
    }

    @GetMapping("/getPhoneFieldSet")
    public AjaxResult getFieldSettings(){
      try{
          OrderFieldSetting orderFieldSetting = orderFieldSettingService.selectOrderFieldSettingByType("user-phone");
          if (StringUtils.isNotNull(orderFieldSetting)){
               boolean res = orderFieldSetting.getStatus().equals("0");
              return success(res);
          }
          return success(true);
      }catch (Exception e){
          return success(true);
      }
    }
}
