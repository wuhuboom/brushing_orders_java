package com.order.member.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.LegacyMarketingMapper;
import com.order.member.service.ILegacyMarketingService;

/**
 * Legacy customer-management compatibility service implementation.
 */
@Service
public class LegacyMarketingServiceImpl implements ILegacyMarketingService
{
    @Autowired
    private LegacyMarketingMapper legacyMarketingMapper;

    @Override
    public List<Map<String, Object>> selectAuthRecordList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectAuthRecordList(params);
    }

    @Override
    public Map<String, Object> selectAuthRecordById(Long id)
    {
        return legacyMarketingMapper.selectAuthRecordById(id);
    }

    @Override
    public int insertAuthRecord(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertAuthRecord(data);
    }

    @Override
    public int updateAuthRecord(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateAuthRecord(data);
    }

    @Override
    public int deleteAuthRecordByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteAuthRecordByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectMemberDateStatistics(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectMemberDateStatistics(params);
    }

    @Override
    public List<Map<String, Object>> selectMemberStatistics(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectMemberStatistics(params);
    }

    @Override
    public List<Map<String, Object>> selectPerformanceStatistics(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectPerformanceStatistics(params);
    }

    @Override
    public List<Map<String, Object>> selectDuplicateIpMembers(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectDuplicateIpMembers(params);
    }

    @Override
    public List<Map<String, Object>> selectYuebaoAccountList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectYuebaoAccountList(params);
    }

    @Override
    public Map<String, Object> selectYuebaoAccountById(Long id)
    {
        return legacyMarketingMapper.selectYuebaoAccountById(id);
    }

    @Override
    public int insertYuebaoAccount(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertYuebaoAccount(data);
    }

    @Override
    public int updateYuebaoAccount(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateYuebaoAccount(data);
    }

    @Override
    public int deleteYuebaoAccountByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteYuebaoAccountByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectYuebaoFlowList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectYuebaoFlowList(params);
    }

    @Override
    public Map<String, Object> selectYuebaoFlowById(Long id)
    {
        return legacyMarketingMapper.selectYuebaoFlowById(id);
    }

    @Override
    public int insertYuebaoFlow(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertYuebaoFlow(data);
    }

    @Override
    public int updateYuebaoFlow(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateYuebaoFlow(data);
    }

    @Override
    public int updateYuebaoFlowHiddenByIds(Long[] ids, String isHidden)
    {
        return legacyMarketingMapper.updateYuebaoFlowHiddenByIds(ids, isHidden);
    }

    @Override
    public int deleteYuebaoFlowByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteYuebaoFlowByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectBulletinList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectBulletinList(params);
    }

    @Override
    public Map<String, Object> selectBulletinById(Long id)
    {
        return legacyMarketingMapper.selectBulletinById(id);
    }

    @Override
    public int insertBulletin(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertBulletin(data);
    }

    @Override
    public int updateBulletin(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateBulletin(data);
    }

    @Override
    public int deleteBulletinByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteBulletinByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectWebsiteStatistics(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectWebsiteStatistics(params);
    }

    @Override
    public List<Map<String, Object>> selectWalletList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectWalletList(params);
    }

    @Override
    public Map<String, Object> selectWalletById(Long id)
    {
        return legacyMarketingMapper.selectWalletById(id);
    }

    @Override
    public int insertWallet(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertWallet(data);
    }

    @Override
    public int updateWallet(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateWallet(data);
    }

    @Override
    public int deleteWalletByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteWalletByIds(ids);
    }

    @Override
    public List<Map<String, Object>> selectRecruitmentList(Map<String, Object> params)
    {
        return legacyMarketingMapper.selectRecruitmentList(params);
    }

    @Override
    public Map<String, Object> selectRecruitmentById(Long id)
    {
        return legacyMarketingMapper.selectRecruitmentById(id);
    }

    @Override
    public int insertRecruitment(Map<String, Object> data)
    {
        return legacyMarketingMapper.insertRecruitment(data);
    }

    @Override
    public int updateRecruitment(Map<String, Object> data)
    {
        return legacyMarketingMapper.updateRecruitment(data);
    }

    @Override
    public int deleteRecruitmentByIds(Long[] ids)
    {
        return legacyMarketingMapper.deleteRecruitmentByIds(ids);
    }
}
