package com.order.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * Points, activity and website management mapper.
 */
public interface CommerceManagementMapper
{
    List<Map<String, Object>> selectGiftList(Map<String, Object> params);
    Map<String, Object> selectGiftById(Long id);
    int insertGift(Map<String, Object> data);
    int updateGift(Map<String, Object> data);
    int deleteGiftByIds(Long[] ids);
    int adjustGift(@Param("id") Long id, @Param("stockDelta") Long stockDelta,
            @Param("salesDelta") Long salesDelta, @Param("version") Integer version);

    List<Map<String, Object>> selectPointsAccountList(Map<String, Object> params);
    int ensurePointsAccount(Long userId);
    Map<String, Object> selectPointsAccountForUpdate(Long userId);
    Map<String, Object> selectUserSnapshot(Long userId);
    int updatePointsAccount(@Param("userId") Long userId, @Param("points") BigDecimal points,
            @Param("availablePoints") BigDecimal availablePoints);
    int insertPointsFlow(Map<String, Object> data);
    List<Map<String, Object>> selectPointsFlowList(Map<String, Object> params);

    List<Map<String, Object>> selectGiftOrderList(Map<String, Object> params);
    Map<String, Object> selectGiftOrderById(Long id);
    int updateGiftOrderRemark(Map<String, Object> data);
    int shipGiftOrder(Long id);
    int receiveGiftOrder(Long id);
    int cancelGiftOrder(Long id);

    List<Map<String, Object>> selectActivityList(Map<String, Object> params);
    Map<String, Object> selectActivityById(Long id);
    int insertActivity(Map<String, Object> data);
    int updateActivity(Map<String, Object> data);
    int deleteActivityByIds(Long[] ids);
    BigDecimal selectEnabledPrizeProbability(Long activityId);

    List<Map<String, Object>> selectActivityPrizeList(Map<String, Object> params);
    Map<String, Object> selectActivityPrizeById(Long id);
    int insertActivityPrize(Map<String, Object> data);
    int updateActivityPrize(Map<String, Object> data);
    int deleteActivityPrizeByIds(Long[] ids);

    List<Map<String, Object>> selectActivityAccountList(Map<String, Object> params);
    int ensureActivityAccount(Long userId);
    Map<String, Object> selectActivityAccountForUpdate(Long userId);
    int updateActivityAccount(@Param("userId") Long userId, @Param("availableTimes") Long availableTimes);

    List<Map<String, Object>> selectAccountPrizeList(@Param("userId") Long userId,
            @Param("params") Map<String, Object> params);
    Map<String, Object> selectAccountPrizeById(Long id);
    int insertAccountPrize(Map<String, Object> data);
    int updateAccountPrize(Map<String, Object> data);
    int deleteAccountPrizeByIds(Long[] ids);

    List<Map<String, Object>> selectActivityPartnerList(Map<String, Object> params);
    int updatePartnerHidden(@Param("ids") Long[] ids, @Param("isHidden") String isHidden);

    List<Map<String, Object>> selectWebsiteCustomerList(Map<String, Object> params);
    Map<String, Object> selectWebsiteCustomerById(Long id);
    int insertWebsiteCustomer(Map<String, Object> data);
    int deleteWebsiteCustomerByIds(Long[] ids);
}
