package com.order.member.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.common.exception.ServiceException;
import com.order.member.mapper.CommerceManagementMapper;
import com.order.member.service.ICommerceManagementService;

/**
 * Points, activity and website management service implementation.
 */
@Service
public class CommerceManagementServiceImpl implements ICommerceManagementService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private CommerceManagementMapper mapper;

    @Override
    public List<Map<String, Object>> selectGiftList(Map<String, Object> params)
    {
        return mapper.selectGiftList(params);
    }

    @Override
    public Map<String, Object> selectGiftById(Long id)
    {
        return mapper.selectGiftById(id);
    }

    @Override
    public int insertGift(Map<String, Object> data)
    {
        validateGift(data);
        requireNonNegative(data, "stock", "库存不能为负数");
        return mapper.insertGift(data);
    }

    @Override
    public int updateGift(Map<String, Object> data)
    {
        requireId(data);
        validateGift(data);
        return requireStateChange(mapper.updateGift(data), "礼品不存在或已被删除");
    }

    @Override
    public int deleteGiftByIds(Long[] ids)
    {
        return mapper.deleteGiftByIds(ids);
    }

    @Override
    @Transactional
    public int adjustGift(Long id, Map<String, Object> data)
    {
        Map<String, Object> gift = mapper.selectGiftById(id);
        if (gift == null)
        {
            throw new ServiceException("礼品不存在");
        }
        Long stockDelta = longValue(data.get("stockDelta"));
        Long salesDelta = longValue(data.get("salesDelta"));
        if (stockDelta == null)
        {
            stockDelta = 0L;
        }
        if (salesDelta == null)
        {
            salesDelta = 0L;
        }
        if (stockDelta == 0L && salesDelta == 0L)
        {
            throw new ServiceException("库存或销量变动不能为空");
        }
        Integer version = integerValue(data.get("version"));
        if (version == null)
        {
            version = integerValue(gift.get("version"));
        }
        int rows = mapper.adjustGift(id, stockDelta, salesDelta, version);
        if (rows == 0)
        {
            throw new ServiceException("库存或销量不足，或数据已被其他用户修改，请刷新后重试");
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> selectPointsAccountList(Map<String, Object> params)
    {
        return mapper.selectPointsAccountList(params);
    }

    @Override
    @Transactional
    public int adjustPoints(Long userId, Map<String, Object> data)
    {
        Map<String, Object> user = requireUser(userId);
        BigDecimal amount = decimalValue(data.get("amount"));
        if (amount == null || amount.compareTo(ZERO) <= 0)
        {
            throw new ServiceException("积分必须大于0");
        }
        String operationType = stringValue(data.get("operationType"));
        if ("subtract".equalsIgnoreCase(operationType) || "0".equals(operationType) || "减".equals(operationType))
        {
            amount = amount.negate();
        }
        mapper.ensurePointsAccount(userId);
        Map<String, Object> account = mapper.selectPointsAccountForUpdate(userId);
        if (account == null)
        {
            throw new ServiceException("积分账户初始化失败");
        }
        BigDecimal before = decimalOrZero(account.get("availablePoints"));
        BigDecimal totalBefore = decimalOrZero(account.get("points"));
        BigDecimal after = before.add(amount);
        BigDecimal totalAfter = totalBefore.add(amount);
        if (after.compareTo(ZERO) < 0 || totalAfter.compareTo(ZERO) < 0)
        {
            throw new ServiceException("可用积分不足");
        }
        int rows = requireStateChange(mapper.updatePointsAccount(userId, totalAfter, after), "积分账户更新失败");
        Map<String, Object> flow = new HashMap<>();
        flow.put("flowNo", nextBusinessNo("PF"));
        flow.put("userId", userId);
        flow.put("username", user == null ? null : user.get("username"));
        flow.put("phoneNumber", user == null ? null : user.get("phoneNumber"));
        flow.put("beforePoints", before);
        flow.put("changePoints", amount);
        flow.put("afterPoints", after);
        flow.put("transactionNo", flow.get("flowNo"));
        flow.put("createBy", data.get("createBy"));
        flow.put("remark", data.get("remark"));
        mapper.insertPointsFlow(flow);
        return rows;
    }

    @Override
    public List<Map<String, Object>> selectPointsFlowList(Map<String, Object> params)
    {
        return mapper.selectPointsFlowList(params);
    }

    @Override
    public List<Map<String, Object>> selectGiftOrderList(Map<String, Object> params)
    {
        return mapper.selectGiftOrderList(params);
    }

    @Override
    public Map<String, Object> selectGiftOrderById(Long id)
    {
        return mapper.selectGiftOrderById(id);
    }

    @Override
    public int updateGiftOrderRemark(Map<String, Object> data)
    {
        requireId(data);
        return requireStateChange(mapper.updateGiftOrderRemark(data), "积分订单不存在或已被其他用户修改");
    }

    @Override
    @Transactional
    public int shipGiftOrder(Long id)
    {
        return requireStateChange(mapper.shipGiftOrder(id), "只有待处理订单可以发货");
    }

    @Override
    @Transactional
    public int receiveGiftOrder(Long id)
    {
        return requireStateChange(mapper.receiveGiftOrder(id), "只有已发货订单可以确认收货");
    }

    @Override
    @Transactional
    public int cancelGiftOrder(Long id)
    {
        return requireStateChange(mapper.cancelGiftOrder(id), "订单已取消或不存在");
    }

    @Override
    public List<Map<String, Object>> selectActivityList(Map<String, Object> params)
    {
        return mapper.selectActivityList(params);
    }

    @Override
    public Map<String, Object> selectActivityById(Long id)
    {
        return mapper.selectActivityById(id);
    }

    @Override
    public int insertActivity(Map<String, Object> data)
    {
        validateActivity(data);
        if ("1".equals(stringValue(data.get("isEnabled"))))
        {
            throw new ServiceException("请先以禁用状态创建活动并配置奖品，奖品概率合计达到100%后再启用");
        }
        return mapper.insertActivity(data);
    }

    @Override
    public int updateActivity(Map<String, Object> data)
    {
        requireId(data);
        validateActivity(data);
        if ("1".equals(stringValue(data.get("isEnabled"))))
        {
            Long id = longValue(data.get("id"));
            BigDecimal probability = mapper.selectEnabledPrizeProbability(id);
            if (probability == null || probability.compareTo(new BigDecimal("100")) < 0)
            {
                throw new ServiceException("启用活动前，启用奖品的中奖概率合计必须大于等于100%");
            }
        }
        return requireStateChange(mapper.updateActivity(data), "活动不存在或已被删除");
    }

    @Override
    public int deleteActivityByIds(Long[] ids)
    {
        return mapper.deleteActivityByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectActivityPrizeList(Map<String, Object> params)
    {
        return mapper.selectActivityPrizeList(params);
    }

    @Override
    public Map<String, Object> selectActivityPrizeById(Long id)
    {
        return mapper.selectActivityPrizeById(id);
    }

    @Override
    public int insertActivityPrize(Map<String, Object> data)
    {
        validatePrize(data);
        return mapper.insertActivityPrize(data);
    }

    @Override
    public int updateActivityPrize(Map<String, Object> data)
    {
        requireId(data);
        validatePrize(data);
        return requireStateChange(mapper.updateActivityPrize(data), "活动奖品不存在或已被删除");
    }

    @Override
    public int deleteActivityPrizeByIds(Long[] ids)
    {
        return mapper.deleteActivityPrizeByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectActivityAccountList(Map<String, Object> params)
    {
        return mapper.selectActivityAccountList(params);
    }

    @Override
    @Transactional
    public int adjustActivityTimes(Long userId, Map<String, Object> data)
    {
        requireUser(userId);
        Long amount = longValue(data.get("amount"));
        if (amount == null || amount <= 0)
        {
            throw new ServiceException("抽奖次数必须大于0");
        }
        String operationType = stringValue(data.get("operationType"));
        if ("subtract".equalsIgnoreCase(operationType) || "0".equals(operationType) || "减".equals(operationType))
        {
            amount = -amount;
        }
        mapper.ensureActivityAccount(userId);
        Map<String, Object> account = mapper.selectActivityAccountForUpdate(userId);
        Long before = account == null ? 0L : longValue(account.get("availableTimes"));
        long after = (before == null ? 0L : before) + amount;
        if (after < 0)
        {
            throw new ServiceException("可用抽奖次数不足");
        }
        return requireStateChange(mapper.updateActivityAccount(userId, after), "活动账户更新失败");
    }

    @Override
    public List<Map<String, Object>> selectAccountPrizeList(Long userId, Map<String, Object> params)
    {
        return mapper.selectAccountPrizeList(userId, params);
    }

    @Override
    public Map<String, Object> selectAccountPrizeById(Long id)
    {
        return mapper.selectAccountPrizeById(id);
    }

    @Override
    public int insertAccountPrize(Long userId, Map<String, Object> data)
    {
        validateAccountPrize(userId, data);
        data.put("userId", userId);
        return mapper.insertAccountPrize(data);
    }

    @Override
    public int updateAccountPrize(Long userId, Map<String, Object> data)
    {
        requireId(data);
        validateAccountPrize(userId, data);
        data.put("userId", userId);
        return requireStateChange(mapper.updateAccountPrize(data), "用户奖品设置不存在或不属于当前用户");
    }

    @Override
    public int deleteAccountPrizeByIds(Long[] ids)
    {
        return mapper.deleteAccountPrizeByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectActivityPartnerList(Map<String, Object> params)
    {
        return mapper.selectActivityPartnerList(params);
    }

    @Override
    public int updatePartnerHidden(Long[] ids, String isHidden)
    {
        if (!"0".equals(isHidden) && !"1".equals(isHidden))
        {
            throw new ServiceException("隐藏状态不正确");
        }
        return requireStateChange(mapper.updatePartnerHidden(ids, isHidden), "未找到可更新的活动参与记录");
    }

    @Override
    public List<Map<String, Object>> selectWebsiteCustomerList(Map<String, Object> params)
    {
        return mapper.selectWebsiteCustomerList(params);
    }

    @Override
    public Map<String, Object> selectWebsiteCustomerById(Long id)
    {
        return mapper.selectWebsiteCustomerById(id);
    }

    @Override
    public int insertWebsiteCustomer(Map<String, Object> data)
    {
        requireText(data, "firstName", "名不能为空");
        requireText(data, "lastName", "姓不能为空");
        requireText(data, "email", "邮箱不能为空");
        requireText(data, "country", "国家不能为空");
        return mapper.insertWebsiteCustomer(data);
    }

    @Override
    public int deleteWebsiteCustomerByIds(Long[] ids)
    {
        return mapper.deleteWebsiteCustomerByIds(ids);
    }

    private void validateGift(Map<String, Object> data)
    {
        requireText(data, "title", "礼品标题不能为空");
        requireOneOf(data, "kind", "礼品类型不正确", "1", "2");
        requireOneOf(data, "isEnabled", "礼品启用状态不正确", "0", "1");
        requireNonNegativeValue(data, "points", "积分不能为空且不能为负数");
        requireNonNegativeValue(data, "price", "价格不能为空且不能为负数");
        requireText(data, "image", "礼品图片不能为空");
    }

    private void validateActivity(Map<String, Object> data)
    {
        requireText(data, "title", "活动标题不能为空");
        requireOneOf(data, "isEnabled", "活动启用状态不正确", "0", "1");
        requireNonNegativeValue(data, "sortOrder", "活动序号不能为空且不能为负数");
        requireText(data, "image", "活动图片不能为空");
        requireText(data, "noWinningTips", "未中奖提示不能为空");
        requireText(data, "ruleContent", "活动规则不能为空");
        requireText(data, "description", "活动描述不能为空");
    }

    private void validatePrize(Map<String, Object> data)
    {
        Long activityId = longValue(data.get("activityId"));
        if (activityId == null)
        {
            throw new ServiceException("活动不能为空");
        }
        if (mapper.selectActivityById(activityId) == null)
        {
            throw new ServiceException("活动不存在");
        }
        requireText(data, "title", "奖品标题不能为空");
        BigDecimal probability = decimalValue(data.get("probability"));
        if (probability == null || probability.compareTo(ZERO) < 0 || probability.compareTo(new BigDecimal("100")) > 0)
        {
            throw new ServiceException("中奖概率必须在0到100之间");
        }
        requireOneOf(data, "kind", "奖品类型不正确", "1", "2", "3");
        requireOneOf(data, "isEnabled", "奖品启用状态不正确", "0", "1");
        requireNonNegativeValue(data, "price", "奖品价格不能为空且不能为负数");
        requireNonNegativeValue(data, "sortOrder", "奖品序号不能为空且不能为负数");
        requireText(data, "image", "奖品图片不能为空");
    }

    private void validateAccountPrize(Long userId, Map<String, Object> data)
    {
        requireUser(userId);
        Long activityId = longValue(data.get("activityId"));
        if (activityId == null || mapper.selectActivityById(activityId) == null)
        {
            throw new ServiceException("活动不存在");
        }
        Long prizeId = longValue(data.get("prizeId"));
        Map<String, Object> prize = prizeId == null ? null : mapper.selectActivityPrizeById(prizeId);
        if (prize == null)
        {
            throw new ServiceException("奖品不存在");
        }
        if (!activityId.equals(longValue(prize.get("activityId"))))
        {
            throw new ServiceException("奖品不属于所选活动");
        }
        requireOneOf(data, "isWinning", "中奖状态不正确", "0", "1");
        requireNonNegativeValue(data, "sortOrder", "序号不能为空且不能为负数");
    }

    private Map<String, Object> requireUser(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户不能为空");
        }
        Map<String, Object> user = mapper.selectUserSnapshot(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    private int requireStateChange(int rows, String message)
    {
        if (rows == 0)
        {
            throw new ServiceException(message);
        }
        return rows;
    }

    private void requireId(Map<String, Object> data)
    {
        if (longValue(data.get("id")) == null)
        {
            throw new ServiceException("ID不能为空");
        }
    }

    private void requireText(Map<String, Object> data, String key, String message)
    {
        if (stringValue(data.get(key)) == null || stringValue(data.get(key)).isBlank())
        {
            throw new ServiceException(message);
        }
    }

    private void requireNonNegative(Map<String, Object> data, String key, String message)
    {
        BigDecimal value = decimalValue(data.get(key));
        if (value != null && value.compareTo(ZERO) < 0)
        {
            throw new ServiceException(message);
        }
    }

    private void requireNonNegativeValue(Map<String, Object> data, String key, String message)
    {
        BigDecimal value = decimalValue(data.get(key));
        if (value == null || value.compareTo(ZERO) < 0)
        {
            throw new ServiceException(message);
        }
    }

    private void requireOneOf(Map<String, Object> data, String key, String message, String... values)
    {
        String actual = stringValue(data.get(key));
        for (String value : values)
        {
            if (value.equals(actual))
            {
                return;
            }
        }
        throw new ServiceException(message);
    }

    private String nextBusinessNo(String prefix)
    {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private BigDecimal decimalOrZero(Object value)
    {
        BigDecimal number = decimalValue(value);
        return number == null ? ZERO : number;
    }

    private BigDecimal decimalValue(Object value)
    {
        if (value == null || String.valueOf(value).isBlank())
        {
            return null;
        }
        try
        {
            return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value));
        }
        catch (NumberFormatException exception)
        {
            throw new ServiceException("数字格式不正确");
        }
    }

    private Long longValue(Object value)
    {
        if (value == null || String.valueOf(value).isBlank())
        {
            return null;
        }
        try
        {
            return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException exception)
        {
            throw new ServiceException("整数格式不正确");
        }
    }

    private Integer integerValue(Object value)
    {
        Long number = longValue(value);
        return number == null ? null : number.intValue();
    }

    private String stringValue(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}
