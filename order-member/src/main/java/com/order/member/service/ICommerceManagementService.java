package com.order.member.service;

import java.util.List;
import java.util.Map;

/**
 * Points, activity and website management service.
 */
public interface ICommerceManagementService
{
    List<Map<String, Object>> selectGiftList(Map<String, Object> params);
    Map<String, Object> selectGiftById(Long id);
    int insertGift(Map<String, Object> data);
    int updateGift(Map<String, Object> data);
    int deleteGiftByIds(Long[] ids);
    int adjustGift(Long id, Map<String, Object> data);

    List<Map<String, Object>> selectPointsAccountList(Map<String, Object> params);
    int adjustPoints(Long userId, Map<String, Object> data);
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

    List<Map<String, Object>> selectActivityPrizeList(Map<String, Object> params);
    Map<String, Object> selectActivityPrizeById(Long id);
    int insertActivityPrize(Map<String, Object> data);
    int updateActivityPrize(Map<String, Object> data);
    int deleteActivityPrizeByIds(Long[] ids);

    List<Map<String, Object>> selectActivityAccountList(Map<String, Object> params);
    int adjustActivityTimes(Long userId, Map<String, Object> data);
    List<Map<String, Object>> selectAccountPrizeList(Long userId, Map<String, Object> params);
    Map<String, Object> selectAccountPrizeById(Long id);
    int insertAccountPrize(Long userId, Map<String, Object> data);
    int updateAccountPrize(Long userId, Map<String, Object> data);
    int deleteAccountPrizeByIds(Long[] ids);

    List<Map<String, Object>> selectActivityPartnerList(Map<String, Object> params);
    int updatePartnerHidden(Long[] ids, String isHidden);

    List<Map<String, Object>> selectWebsiteCustomerList(Map<String, Object> params);
    Map<String, Object> selectWebsiteCustomerById(Long id);
    int insertWebsiteCustomer(Map<String, Object> data);
    int deleteWebsiteCustomerByIds(Long[] ids);
}
