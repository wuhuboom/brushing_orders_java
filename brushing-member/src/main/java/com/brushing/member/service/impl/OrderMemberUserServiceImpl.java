package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.InviteCodeGenerator;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.MessageUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.domain.vo.MemberLevelAmountVo;
import com.brushing.member.domain.vo.RebateStatVo;
import com.brushing.member.domain.vo.TopLevelUserStatVo;
import com.brushing.member.mapper.*;
import com.brushing.member.service.IOrderShopService;
import com.github.pagehelper.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.member.domain.vo.MemberHierarchyStatVo;

/**
 * 会员用户Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-30
 */
@Service
public class OrderMemberUserServiceImpl implements IOrderMemberUserService 
{
    private static final int ANCESTOR_UPDATE_BATCH_SIZE = 500;
    private static final int HIERARCHY_LOCK_BATCH_SIZE = 500;
    private static final int MEMBER_LIST_AGGREGATE_BATCH_SIZE = 500;
    private static final String MEMBER_LIST_SORT_KEY_PARAM = "memberListSortKey";
    private static final String MEMBER_LIST_SORT_DIRECTION_PARAM = "memberListSortDirection";
    private static final Set<String> MEMBER_LIST_AGGREGATE_SORT_KEYS = Set.of(
            "total_recharge",
            "total_withdraw",
            "diff_amount",
            "withdraw_frozen_amount",
            "today_withdraw_count",
            "direct_sub_count",
            "all_sub_count");

    @Autowired
    private OrderMemberUserMapper orderMemberUserMapper;

    @Autowired
    private OrderMemberLevelMapper levelMapper;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private WelfareConfigMapper welfareConfigMapper;

    @Autowired
    private OrderShopMapper shopMapper;

    /**
     * 查询会员用户
     * 
     * @param id 会员用户主键
     * @return 会员用户
     */
    @Override
    public OrderMemberUser selectOrderMemberUserById(Long id)
    {
        return orderMemberUserMapper.selectOrderMemberUserById(id);
    }

    @Override
    public OrderMemberUser findByUsername(String username) {
        return orderMemberUserMapper.findByUsername(username);
    }

    @Override
    @Transactional
    public String register(OrderMemberUser user) {
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserByInCode(user.getInviteCode());
        if (StringUtils.isNull(orderMemberUser)){
            return "500";
        }

        OrderMemberLevel orderMemberLevel = levelMapper.selectLowestPriceLevel();
        user.setLevelId(orderMemberLevel.getId());
        user.setParentId(orderMemberUser.getId());
        WelfareConfig welfareConfig = welfareConfigMapper.selectWelfareConfigByType("1");
        if (StringUtils.isNotNull(welfareConfig)&&welfareConfig.getStatus().equals("0")){
            user.setBalance(welfareConfig.getAmount());
        }
        user.setInviteCode(setCode());
        user.setAncestors(lockParentAndBuildAncestors(orderMemberUser.getId()));
        orderMemberUserMapper.insertOrderMemberUser(user);
        return "200";
    }

    @Override
    public Boolean existsPhone(String phone) {
        return orderMemberUserMapper.existsPhone(phone);
    }


    /**
     * 查询会员用户列表
     * 
     * @param orderMemberUser 会员用户
     * @return 会员用户
    */
    @Override
    @Transactional(readOnly = true)
    public List<OrderMemberUser> selectOrderMemberUserList(OrderMemberUser orderMemberUser)
    {
        // Keep the exact Page returned by PageHelper.  Copying the rows into a
        // normal ArrayList here would discard Page.total and break getDataTable.
        List<OrderMemberUser> members = orderMemberUserMapper.selectOrderMemberUserList(orderMemberUser);
        return enrichMemberList(members);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderMemberUser> selectOrderMemberUserListByAggregateSort(
            OrderMemberUser orderMemberUser, String sortKey, String sortDirection)
    {
        validateMemberListAggregateSort(sortKey, sortDirection);

        Map<String, Object> params = orderMemberUser.getParams();
        boolean hadSortKey = params.containsKey(MEMBER_LIST_SORT_KEY_PARAM);
        boolean hadSortDirection = params.containsKey(MEMBER_LIST_SORT_DIRECTION_PARAM);
        Object previousSortKey = params.put(MEMBER_LIST_SORT_KEY_PARAM, sortKey);
        Object previousSortDirection = params.put(
                MEMBER_LIST_SORT_DIRECTION_PARAM, sortDirection);
        try {
            List<Long> sortedIds = orderMemberUserMapper
                    .selectOrderMemberUserAggregateSortedIds(orderMemberUser);
            if (!(sortedIds instanceof Page<?>)) {
                throw new ServiceException(
                        "Aggregate member-list sorting requires an active page request",
                        com.brushing.common.constant.HttpStatus.BAD_REQUEST);
            }

            @SuppressWarnings("unchecked")
            Page<Long> idPage = (Page<Long>) sortedIds;
            Page<OrderMemberUser> memberPage = copyMemberPageMetadata(idPage);
            if (idPage.isEmpty()) {
                return memberPage;
            }

            Map<Long, OrderMemberUser> rowsById = new HashMap<>();
            for (int start = 0; start < idPage.size(); start += MEMBER_LIST_AGGREGATE_BATCH_SIZE) {
                int end = Math.min(start + MEMBER_LIST_AGGREGATE_BATCH_SIZE, idPage.size());
                List<Long> batch = new ArrayList<>(idPage.subList(start, end));
                indexMemberAggregates(
                        rowsById,
                        orderMemberUserMapper.selectOrderMemberUserListByIds(batch));
            }
            for (Long id : idPage) {
                OrderMemberUser member = rowsById.get(id);
                if (member == null) {
                    throw new ServiceException(
                            "Member list changed while the sorted page was being loaded");
                }
                memberPage.add(member);
            }
            return enrichMemberList(memberPage);
        } finally {
            restoreInternalListParam(
                    params, MEMBER_LIST_SORT_KEY_PARAM, hadSortKey, previousSortKey);
            restoreInternalListParam(
                    params,
                    MEMBER_LIST_SORT_DIRECTION_PARAM,
                    hadSortDirection,
                    previousSortDirection);
        }
    }

    private List<OrderMemberUser> enrichMemberList(List<OrderMemberUser> members)
    {
        if (members == null || members.isEmpty()) {
            return members;
        }

        List<Long> userIds = new ArrayList<>(members.size());
        for (OrderMemberUser member : members) {
            if (member != null && member.getId() != null) {
                userIds.add(member.getId());
            }
        }
        if (userIds.isEmpty()) {
            return members;
        }

        Map<Long, OrderMemberUser> financialByUserId = new HashMap<>();
        Map<Long, OrderMemberUser> subordinateByUserId = new HashMap<>();
        // The normal list is one small page, while export intentionally calls
        // this service without PageHelper.  Bounded batches avoid an unbounded
        // IN clause for that export path.
        for (int start = 0; start < userIds.size(); start += MEMBER_LIST_AGGREGATE_BATCH_SIZE) {
            int end = Math.min(start + MEMBER_LIST_AGGREGATE_BATCH_SIZE, userIds.size());
            List<Long> batch = new ArrayList<>(userIds.subList(start, end));
            indexMemberAggregates(
                    financialByUserId,
                    orderMemberUserMapper.selectMemberListFinancialAggregates(batch));
            indexMemberAggregates(
                    subordinateByUserId,
                    orderMemberUserMapper.selectMemberListSubordinateAggregates(batch));
        }

        for (OrderMemberUser member : members) {
            if (member == null || member.getId() == null) {
                continue;
            }
            OrderMemberUser financial = financialByUserId.get(member.getId());
            BigDecimal totalRecharge = amountOrZero(
                    financial == null ? null : financial.getTotalRecharge());
            BigDecimal totalWithdraw = amountOrZero(
                    financial == null ? null : financial.getTotalWithdraw());
            member.setTotalRecharge(totalRecharge);
            member.setTotalWithdraw(totalWithdraw);
            member.setWithdrawFrozenAmount(amountOrZero(
                    financial == null ? null : financial.getWithdrawFrozenAmount()));
            member.setTodayWithdrawCount(financial == null
                    || financial.getTodayWithdrawCount() == null
                    ? 0 : financial.getTodayWithdrawCount());
            member.setDiffAmount(totalRecharge.subtract(totalWithdraw));

            OrderMemberUser subordinate = subordinateByUserId.get(member.getId());
            member.setDirectSubCount(subordinate == null
                    || subordinate.getDirectSubCount() == null
                    ? 0 : subordinate.getDirectSubCount());
            member.setAllSubCount(subordinate == null
                    || subordinate.getAllSubCount() == null
                    ? 0 : subordinate.getAllSubCount());
        }
        return members;
    }

    private void validateMemberListAggregateSort(String sortKey, String sortDirection)
    {
        if (!MEMBER_LIST_AGGREGATE_SORT_KEYS.contains(sortKey)
                || (!("asc".equals(sortDirection)) && !("desc".equals(sortDirection)))) {
            throw new ServiceException(
                    "Unsupported member list aggregate sort",
                    com.brushing.common.constant.HttpStatus.BAD_REQUEST);
        }
    }

    private Page<OrderMemberUser> copyMemberPageMetadata(Page<Long> source)
    {
        Page<OrderMemberUser> copy = new Page<>(source.getPageNum(), source.getPageSize());
        copy.setReasonable(source.getReasonable());
        copy.setPageSizeZero(source.getPageSizeZero());
        copy.setTotal(source.getTotal());
        return copy;
    }

    private void restoreInternalListParam(Map<String, Object> params,
                                          String key,
                                          boolean previouslyPresent,
                                          Object previousValue)
    {
        if (previouslyPresent) {
            params.put(key, previousValue);
        } else {
            params.remove(key);
        }
    }

    private void indexMemberAggregates(Map<Long, OrderMemberUser> destination,
                                       List<OrderMemberUser> aggregates) {
        if (aggregates == null) {
            return;
        }
        for (OrderMemberUser aggregate : aggregates) {
            if (aggregate != null && aggregate.getId() != null) {
                destination.put(aggregate.getId(), aggregate);
            }
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    @Override
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope) {
        return orderMemberUserMapper.selectMembersByScope(userId, scope, null, null);
    }

    // 新增：支持按下级用户名模糊查询的重载
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope, String subUsername) {
        return orderMemberUserMapper.selectMembersByScope(userId, scope, subUsername, null);
    }

    @Override
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope, String subUsername, String subPhone) {
        return orderMemberUserMapper.selectMembersByScope(userId, scope, subUsername, subPhone);
    }

    /**
     * 新增会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    @Override
    @Transactional
    public int insertOrderMemberUser(OrderMemberUser orderMemberUser)
    {
        resolveAndValidateParent(orderMemberUser);
        orderMemberUser.setCreateTime(DateUtils.getNowDate());
        orderMemberUser.setInviteCode(setCode());
        orderMemberUser.setAncestors(lockParentAndBuildAncestors(orderMemberUser.getParentId()));
       if (StringUtils.isNull(orderMemberUser.getLevelId())){
           OrderMemberLevel orderMemberLevel = levelMapper.selectLowestPriceLevel();
           orderMemberUser.setLevelId(orderMemberLevel.getId());
       }
        return orderMemberUserMapper.insertOrderMemberUser(orderMemberUser);
    }

    public String setCode(){
        String code = InviteCodeGenerator.generateInviteCode();
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserByInCode(code);
        if (StringUtils.isNotNull(orderMemberUser)){
            return setCode();
        }
        return code;
    }

    //设置祖级
    public String setAncestors(OrderMemberUser orderMemberUser){
        Long parentId = orderMemberUser.getParentId();
        if (StringUtils.isNull(parentId)){
            return "0";
        }
        if (parentId.equals(0L)){
            return "0";
        }
        OrderMemberUser orderMemberUser1 = orderMemberUserMapper.selectOrderMemberUserById(parentId);
        if (StringUtils.isNull(orderMemberUser1)){
            return "0";
        }
        String parentAncestors = StringUtils.isEmpty(orderMemberUser1.getAncestors())
                ? "0" : orderMemberUser1.getAncestors();
        return parentAncestors + "," + orderMemberUser1.getId();
    }

    /**
     * 修改会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    @Override
    @Transactional
    public int updateOrderMemberUser(OrderMemberUser orderMemberUser)
    {
        validateGenericHierarchyFields(orderMemberUser);
        return orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
    }

    /**
     * Parent changes are intentionally isolated from the generic member update.
     * The target and parent rows are locked in ID order, then the complete
     * branch is locked and checked through the real parent_id graph. A failed
     * optimistic update raises an exception so Spring rolls the complete
     * transaction back.
     */
    @Override
    @Transactional
    public int changeParent(Long memberId, String parentIdentifier, Long version)
    {
        if (memberId == null) {
            throw new ServiceException(MessageUtils.message("member.user.id.required"));
        }
        if (version == null) {
            throw new ServiceException(MessageUtils.message("member.user.version.required"));
        }

        String identifier = StringUtils.trim(parentIdentifier);
        if (StringUtils.isEmpty(identifier)) {
            throw new ServiceException(MessageUtils.message("member.user.parent_identifier.required"));
        }

        OrderMemberUser targetSnapshot = orderMemberUserMapper.selectMemberHierarchyById(memberId);
        if (targetSnapshot == null) {
            throw new ServiceException(MessageUtils.message("member.user.not.exists"));
        }

        OrderMemberUser parentSnapshot = null;
        Long newParentId = 0L;
        if (!"0".equals(identifier)) {
            parentSnapshot = resolveParentHierarchy(identifier);
            if (parentSnapshot == null) {
                throw new ServiceException(MessageUtils.message("member.user.parent_id.invalid"));
            }
            newParentId = parentSnapshot.getId();
        }

        List<Long> lockIds = new ArrayList<>();
        lockIds.add(memberId);
        if (!Objects.equals(memberId, newParentId) && !Objects.equals(0L, newParentId)) {
            lockIds.add(newParentId);
        }
        lockIds.sort(Long::compareTo);

        List<OrderMemberUser> lockedMembers = orderMemberUserMapper.selectMemberHierarchiesForUpdate(lockIds);
        OrderMemberUser target = findHierarchyMember(lockedMembers, memberId);
        if (target == null) {
            throw new ServiceException(MessageUtils.message("member.user.not.exists"));
        }
        if (!Objects.equals(target.getVersion(), version)) {
            throw new ServiceException(MessageUtils.message("member.user.version.conflict"));
        }
        if (Objects.equals(memberId, newParentId)) {
            throw new ServiceException(MessageUtils.message("member.user.parent.self"));
        }

        OrderMemberUser parent = null;
        if (!Objects.equals(0L, newParentId)) {
            parent = findHierarchyMember(lockedMembers, newParentId);
            if (parent == null) {
                throw new ServiceException(MessageUtils.message("member.user.parent_id.invalid"));
            }
        }

        // Lock the complete branch before either checking for a cycle or
        // changing paths.  Every supported insert/move also locks its parent,
        // so a concurrent descendant move or child insert cannot interleave
        // with this rebuild and leave a stale ancestors value behind.
        List<OrderMemberUser> lockedSubtree = lockSubtree(target);
        if (findHierarchyMember(lockedSubtree, newParentId) != null) {
            throw new ServiceException(MessageUtils.message("member.user.parent.descendant"));
        }

        String newAncestors = buildAncestors(parent);
        int changed = orderMemberUserMapper.changeMemberParent(
                memberId, newParentId, newAncestors, version);
        if (changed != 1) {
            throw new ServiceException(MessageUtils.message("member.user.version.conflict"));
        }

        List<OrderMemberUser> descendants = rebuildDescendantPaths(
                lockedSubtree,
                memberId,
                newAncestors);
        // changeMemberParent already updated the root path and incremented its
        // version exactly once. Only descendants are updated here; incrementing
        // every descendant version also makes the affected-row count stable
        // regardless of the JDBC useAffectedRows setting.
        for (int start = 0; start < descendants.size(); start += ANCESTOR_UPDATE_BATCH_SIZE) {
            int end = Math.min(start + ANCESTOR_UPDATE_BATCH_SIZE, descendants.size());
            List<OrderMemberUser> batch = new ArrayList<>(descendants.subList(start, end));
            int rebuilt = orderMemberUserMapper.updateMemberAncestorsBatch(batch);
            if (rebuilt != batch.size()) {
                throw hierarchyRebuildFailure();
            }
        }
        return changed;
    }

    /**
     * Lock a complete branch with current reads against the base table. The
     * target is already locked by changeParent. Each returned child is locked
     * before it becomes a parent in the next frontier, matching the parent-lock
     * protocol used by register, insertOrderMemberUser and changeParent.
     */
    private List<OrderMemberUser> lockSubtree(OrderMemberUser target) {
        if (target == null || target.getId() == null) {
            throw hierarchyRebuildFailure();
        }

        List<OrderMemberUser> locked = new ArrayList<>();
        locked.add(target);
        Set<Long> visited = new HashSet<>();
        visited.add(target.getId());
        List<Long> frontier = new ArrayList<>();
        frontier.add(target.getId());

        while (!frontier.isEmpty()) {
            List<Long> nextFrontier = new ArrayList<>();
            for (int start = 0; start < frontier.size(); start += HIERARCHY_LOCK_BATCH_SIZE) {
                int end = Math.min(start + HIERARCHY_LOCK_BATCH_SIZE, frontier.size());
                List<Long> parentBatch = new ArrayList<>(frontier.subList(start, end));
                Set<Long> batchParents = new HashSet<>(parentBatch);
                List<OrderMemberUser> children =
                        orderMemberUserMapper.selectMemberChildrenForUpdate(parentBatch);
                if (children == null) {
                    continue;
                }
                for (OrderMemberUser child : children) {
                    if (child == null || child.getId() == null || child.getParentId() == null
                            || !batchParents.contains(child.getParentId())
                            || !visited.add(child.getId())) {
                        throw hierarchyRebuildFailure();
                    }
                    locked.add(child);
                    nextFrontier.add(child.getId());
                }
            }
            nextFrontier.sort(Long::compareTo);
            frontier = nextFrontier;
        }
        return locked;
    }

    private List<OrderMemberUser> rebuildDescendantPaths(List<OrderMemberUser> nodes,
                                                          Long memberId,
                                                          String rootAncestors) {
        if (nodes == null || nodes.isEmpty()) {
            throw hierarchyRebuildFailure();
        }

        Map<Long, OrderMemberUser> byId = new HashMap<>();
        Map<Long, List<OrderMemberUser>> childrenByParent = new HashMap<>();
        for (OrderMemberUser node : nodes) {
            if (node == null || node.getId() == null || byId.put(node.getId(), node) != null) {
                throw hierarchyRebuildFailure();
            }
            if (!Objects.equals(node.getId(), memberId)) {
                childrenByParent.computeIfAbsent(node.getParentId(), ignored -> new ArrayList<>())
                        .add(node);
            }
        }

        OrderMemberUser root = byId.get(memberId);
        if (root == null) {
            throw hierarchyRebuildFailure();
        }
        root.setAncestors(rootAncestors);

        ArrayDeque<OrderMemberUser> queue = new ArrayDeque<>();
        List<OrderMemberUser> rebuilt = new ArrayList<>(Math.max(0, nodes.size() - 1));
        Set<Long> visited = new HashSet<>();
        queue.add(root);
        visited.add(root.getId());
        while (!queue.isEmpty()) {
            OrderMemberUser current = queue.removeFirst();
            if (!Objects.equals(current.getId(), memberId)) {
                rebuilt.add(current);
            }
            List<OrderMemberUser> children = childrenByParent.get(current.getId());
            if (children == null) {
                continue;
            }
            for (OrderMemberUser child : children) {
                if (!visited.add(child.getId())) {
                    throw hierarchyRebuildFailure();
                }
                child.setAncestors(current.getAncestors() + "," + current.getId());
                queue.addLast(child);
            }
        }
        if (visited.size() != nodes.size()) {
            throw hierarchyRebuildFailure();
        }
        return rebuilt;
    }

    private ServiceException hierarchyRebuildFailure() {
        return new ServiceException(MessageUtils.message("member.user.hierarchy.rebuild_failed"));
    }

    private void validateGenericHierarchyFields(OrderMemberUser update) {
        if (update == null || update.getId() == null) {
            throw new ServiceException(MessageUtils.message("member.user.id.required"));
        }
        OrderMemberUser existing = orderMemberUserMapper.selectMemberHierarchyById(update.getId());
        if (existing == null) {
            throw new ServiceException(MessageUtils.message("member.user.not.exists"));
        }
        if (StringUtils.isNotEmpty(StringUtils.trim(update.getParentIdentifier()))
                || (update.getParentId() != null
                    && !Objects.equals(update.getParentId(), existing.getParentId()))
                || (update.getAncestors() != null
                    && !Objects.equals(update.getAncestors(), existing.getAncestors()))) {
            throw new ServiceException(MessageUtils.message("member.user.parent.change_requires_endpoint"));
        }

        // Do not mutate the caller's object. The generic mapper deliberately
        // has no parent_id/ancestors assignments, while retaining these fields
        // keeps the controller's WebSocket payload and other callers intact.
    }

    private OrderMemberUser resolveParentHierarchy(String identifier) {
        OrderMemberUser parent = null;
        try {
            parent = orderMemberUserMapper.selectMemberHierarchyById(Long.valueOf(identifier));
        } catch (NumberFormatException ignored) {
            // Non-numeric identifiers are invite codes.
        }
        if (parent == null) {
            parent = orderMemberUserMapper.selectMemberHierarchyByInviteCode(
                    identifier.toUpperCase(Locale.ROOT));
        }
        return parent;
    }

    private OrderMemberUser findHierarchyMember(List<OrderMemberUser> members, Long id) {
        if (members == null) {
            return null;
        }
        for (OrderMemberUser member : members) {
            if (Objects.equals(member.getId(), id)) {
                return member;
            }
        }
        return null;
    }

    private String buildAncestors(OrderMemberUser parent) {
        if (parent == null || Objects.equals(0L, parent.getId())) {
            return "0";
        }
        String parentAncestors = StringUtils.isEmpty(parent.getAncestors())
                ? "0" : parent.getAncestors();
        return parentAncestors + "," + parent.getId();
    }

    private String lockParentAndBuildAncestors(Long parentId) {
        if (parentId == null || Objects.equals(0L, parentId)) {
            return "0";
        }
        List<OrderMemberUser> locked = orderMemberUserMapper.selectMemberHierarchiesForUpdate(
                List.of(parentId));
        OrderMemberUser parent = findHierarchyMember(locked, parentId);
        if (parent == null) {
            throw new ServiceException(MessageUtils.message("member.user.parent_id.invalid"));
        }
        return buildAncestors(parent);
    }

    /**
     * 将管理端输入的上级用户ID或邀请码解析成真实 parentId，并校验层级关系。
     * 纯数字优先按用户ID查找；用户ID不存在时再按邀请码查找。
     */
    private void resolveAndValidateParent(OrderMemberUser user) {
        String identifier = StringUtils.trim(user.getParentIdentifier());
        OrderMemberUser parent = null;

        if (StringUtils.isNotEmpty(identifier)) {
            if ("0".equals(identifier)) {
                user.setParentId(0L);
            } else {
                try {
                    parent = orderMemberUserMapper.selectOrderMemberUserById(Long.valueOf(identifier));
                } catch (NumberFormatException ignored) {
                    // 非数字输入按邀请码查询。
                }
                if (parent == null) {
                    parent = orderMemberUserMapper.selectOrderMemberUserByInCode(identifier.toUpperCase(java.util.Locale.ROOT));
                }
                if (parent == null) {
                    throw new ServiceException(MessageUtils.message("member.user.parent_id.invalid"));
                }
                user.setParentId(parent.getId());
            }
        } else if (StringUtils.isNotNull(user.getParentId()) && !user.getParentId().equals(0L)) {
            parent = orderMemberUserMapper.selectOrderMemberUserById(user.getParentId());
            if (parent == null) {
                throw new ServiceException(MessageUtils.message("member.user.parent_id.invalid"));
            }
        }

        if (user.getParentId() == null || user.getParentId().equals(0L) || user.getId() == null) {
            return;
        }
        if (user.getId().equals(user.getParentId())) {
            throw new ServiceException(MessageUtils.message("member.user.parent.self"));
        }
        if (parent == null) {
            parent = orderMemberUserMapper.selectOrderMemberUserById(user.getParentId());
        }
        String parentAncestors = StringUtils.isEmpty(parent.getAncestors()) ? "0" : parent.getAncestors();
        String parentPath = "," + parentAncestors + "," + parent.getId() + ",";
        if (parentPath.contains("," + user.getId() + ",")) {
            throw new ServiceException(MessageUtils.message("member.user.parent.descendant"));
        }
    }

    /**
     * 批量删除会员用户
     * 
     * @param ids 需要删除的会员用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberUserByIds(Long[] ids)
    {
        return orderMemberUserMapper.deleteOrderMemberUserByIds(ids);
    }

    /**
     * 删除会员用户信息
     * 
     * @param id 会员用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberUserById(Long id)
    {
        return orderMemberUserMapper.deleteOrderMemberUserById(id);
    }

    @Override
    public DashboardData getDashboardData() {
        return dashboardMapper.getDashboardData();
    }

    @Override
    public void updateUserLevel(Long userId, BigDecimal amount) {
        OrderMemberUser user = orderMemberUserMapper.selectOrderMemberUserById(userId);
        try {
                // 获取用户当前的余额对应的等级
                OrderMemberLevel levelByBalance = levelMapper.findLevelByBalance(amount);
                if (levelByBalance == null) {
                    return;
                }
                // 比较查询到的等级与用户当前等级
                Long currentLevelId = user.getLevelId();
                Long newLevelId = levelByBalance.getId();
                if (currentLevelId != null && currentLevelId.equals(newLevelId)) {
                    return;
                }
                user.setLevelId(newLevelId);
                // 更新用户等级
                orderMemberUserMapper.updateOrderMemberUser(user);
        } catch (Exception e) {
                // 记录错误，继续处理下一个用户
        }
    }

    @Override
    public Boolean checkUserBalance(OrderMemberUser user) {
        Integer dealCount = user.getDealCount() + 1;
        //当前等级
        int level = levelMapper.selectLevelById(user.getLevelId());
        //查询当前用户的
        OrderShop orderShop = shopMapper.selectOrderShopByAutoVip(dealCount);
        if (StringUtils.isNull(orderShop)){
            return false;
        }
        //对应的等级
        Integer vipLevel = orderShop.getVipLevel();
        OrderMemberLevel orderMemberLevel = levelMapper.selectLevelByRank(vipLevel);
        if (StringUtils.isNull(orderMemberLevel)){
            return false;
        }
        if (level == vipLevel || level>vipLevel){
            return true;
        }
        BigDecimal balance = user.getBalance();
        BigDecimal price = orderMemberLevel.getPrice();
        return balance.compareTo(price) >= 0;
    }

    @Override
    public List<MemberHierarchyStatVo> getHierarchyStats(String username, LocalDateTime startTime, LocalDateTime endTime) {
        if (StringUtils.isEmpty(username)) {
            throw new ServiceException("用户名不能为空");
        }
        if (startTime == null || endTime == null) {
            throw new ServiceException("时间范围不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new ServiceException("结束时间需大于开始时间");
        }
        List<MemberHierarchyStatVo> stats = orderMemberUserMapper.selectHierarchyStats(username, startTime, endTime);
        for (MemberHierarchyStatVo stat : stats) {
            stat.setDepositWithdrawDiff(stat.getRechargeAmount().subtract(stat.getWithdrawAmount()));
        }
        return stats;
    }

    @Override
    public RebateStatVo getRebateStats(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new ServiceException("用户名不能为空");
        }
        RebateStatVo vo = orderMemberUserMapper.selectRebateStats(username);
        if (vo == null) {
            vo = new RebateStatVo();
            vo.setUsername(username);
            vo.setInviteCode(null);
            vo.setRebateToday(java.math.BigDecimal.ZERO);
            vo.setRebateYesterday(java.math.BigDecimal.ZERO);
            vo.setRebateTotal(java.math.BigDecimal.ZERO);
            vo.setSubordinatesCount(0);
            vo.setTotalTradingUsers(0);
            vo.setTodayTradingUsers(0);
            return vo;
        }
        if (vo.getRebateToday() == null) vo.setRebateToday(java.math.BigDecimal.ZERO);
        if (vo.getRebateYesterday() == null) vo.setRebateYesterday(java.math.BigDecimal.ZERO);
        if (vo.getRebateTotal() == null) vo.setRebateTotal(java.math.BigDecimal.ZERO);
        if (vo.getSubordinatesCount() == null) vo.setSubordinatesCount(0);
        if (vo.getTotalTradingUsers() == null) vo.setTotalTradingUsers(0);
        if (vo.getTodayTradingUsers() == null) vo.setTodayTradingUsers(0);
        return vo;
    }

    @Override
    public List<TopLevelUserStatVo> getTopLevelStats(String username, LocalDateTime startTime, LocalDateTime endTime) {
        // input validation
        List<TopLevelUserStatVo> stats = orderMemberUserMapper.selectTopLevelStats(username, startTime, endTime);
        if (stats == null) {
            return java.util.Collections.emptyList();
        }
        for (TopLevelUserStatVo s : stats) {
            if (s.getRechargeAmount() == null) s.setRechargeAmount(java.math.BigDecimal.ZERO);
            if (s.getWithdrawAmount() == null) s.setWithdrawAmount(java.math.BigDecimal.ZERO);
            s.setDepositWithdrawDiff(s.getRechargeAmount().subtract(s.getWithdrawAmount()));
            if (s.getTotalCommission() == null) s.setTotalCommission(java.math.BigDecimal.ZERO);
            if (s.getTotalBalance() == null) s.setTotalBalance(java.math.BigDecimal.ZERO);
            if (s.getTotalSubCount() == null) s.setTotalSubCount(0);
            if (s.getDirectInviteCount() == null) s.setDirectInviteCount(0);
        }
        return stats;
    }

    @Override
    public List<TopLevelUserStatVo> getSubStatsByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        List<TopLevelUserStatVo> stats = orderMemberUserMapper.selectSubStatsByUserId(userId, startTime, endTime);
        if (stats == null) {
            return java.util.Collections.emptyList();
        }
        for (TopLevelUserStatVo s : stats) {
            if (s.getRechargeAmount() == null) s.setRechargeAmount(java.math.BigDecimal.ZERO);
            if (s.getWithdrawAmount() == null) s.setWithdrawAmount(java.math.BigDecimal.ZERO);
            s.setDepositWithdrawDiff(s.getRechargeAmount().subtract(s.getWithdrawAmount()));
            if (s.getTotalCommission() == null) s.setTotalCommission(java.math.BigDecimal.ZERO);
            if (s.getTotalBalance() == null) s.setTotalBalance(java.math.BigDecimal.ZERO);
            if (s.getTotalSubCount() == null) s.setTotalSubCount(0);
            if (s.getDirectInviteCount() == null) s.setDirectInviteCount(0);
        }
        return stats;
    }

    @Override
    public java.util.List<MemberLevelAmountVo> getFirstThreeLevelsByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new ServiceException("用户名不能为空");
        }
        java.util.List<MemberLevelAmountVo> list = orderMemberUserMapper.selectFirstThreeLevelsByUsername(username);
        if (list == null) return java.util.Collections.emptyList();
        for (MemberLevelAmountVo v : list) {
            if (v.getRechargeAmount() == null) v.setRechargeAmount(java.math.BigDecimal.ZERO);
            if (v.getWithdrawAmount() == null) v.setWithdrawAmount(java.math.BigDecimal.ZERO);
            if (v.getLevel() == null) v.setLevel(0);
        }
        return list;
    }
}
