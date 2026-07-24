package com.order.member.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * Legacy customer-management compatibility mapper.
 */
public interface LegacyMarketingMapper
{
    List<Map<String, Object>> selectAuthRecordList(Map<String, Object> params);

    Map<String, Object> selectAuthRecordById(Long id);

    int insertAuthRecord(Map<String, Object> data);

    int updateAuthRecord(Map<String, Object> data);

    int deleteAuthRecordByIds(Long[] ids);

    List<Map<String, Object>> selectMemberDateStatistics(Map<String, Object> params);

    List<Map<String, Object>> selectMemberStatistics(Map<String, Object> params);

    List<Map<String, Object>> selectPerformanceStatistics(Map<String, Object> params);

    List<Map<String, Object>> selectDuplicateIpMembers(Map<String, Object> params);

    List<Map<String, Object>> selectYuebaoAccountList(Map<String, Object> params);

    Map<String, Object> selectYuebaoAccountById(Long id);

    int insertYuebaoAccount(Map<String, Object> data);

    int updateYuebaoAccount(Map<String, Object> data);

    int deleteYuebaoAccountByIds(Long[] ids);

    List<Map<String, Object>> selectYuebaoFlowList(Map<String, Object> params);

    Map<String, Object> selectYuebaoFlowById(Long id);

    int insertYuebaoFlow(Map<String, Object> data);

    int updateYuebaoFlow(Map<String, Object> data);

    int updateYuebaoFlowHiddenByIds(@Param("ids") Long[] ids, @Param("isHidden") String isHidden);

    int deleteYuebaoFlowByIds(Long[] ids);

    List<Map<String, Object>> selectBulletinList(Map<String, Object> params);

    Map<String, Object> selectBulletinById(Long id);

    int insertBulletin(Map<String, Object> data);

    int updateBulletin(Map<String, Object> data);

    int deleteBulletinByIds(Long[] ids);

    List<Map<String, Object>> selectWebsiteStatistics(Map<String, Object> params);

    List<Map<String, Object>> selectWalletList(Map<String, Object> params);

    Map<String, Object> selectWalletById(Long id);

    int insertWallet(Map<String, Object> data);

    int updateWallet(Map<String, Object> data);

    int deleteWalletByIds(Long[] ids);

    List<Map<String, Object>> selectRecruitmentList(Map<String, Object> params);

    Map<String, Object> selectRecruitmentById(Long id);

    int insertRecruitment(Map<String, Object> data);

    int updateRecruitment(Map<String, Object> data);

    int deleteRecruitmentByIds(Long[] ids);
}
