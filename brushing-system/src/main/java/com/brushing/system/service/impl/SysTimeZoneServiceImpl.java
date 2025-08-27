package com.brushing.system.service.impl;


import com.brushing.common.utils.DateUtils;
import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.mapper.SysTimeZoneMapper;
import com.brushing.system.service.ISysTimeZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

/**
 * 时区管理Service业务层处理
 * 
 * @author betting
 * @date 2025-08-26
 */
@Service
public class SysTimeZoneServiceImpl implements ISysTimeZoneService
{
    @Autowired
    private SysTimeZoneMapper sysTimeZoneMapper;

    /**
     * 查询时区管理
     * 
     * @param id 时区管理主键
     * @return 时区管理
     */
    @Override
    public SysTimeZone selectSysTimeZoneById(Long id)
    {
        return sysTimeZoneMapper.selectSysTimeZoneById(id);
    }

    @Override
    public SysTimeZone selectByTzName(String tzName) {
        return sysTimeZoneMapper.selectByTzName(tzName);
    }

    /**
     * 查询时区管理列表
     * 
     * @param sysTimeZone 时区管理
     * @return 时区管理
     */
    @Override
    public List<SysTimeZone> selectSysTimeZoneList(SysTimeZone sysTimeZone)
    {
        return sysTimeZoneMapper.selectSysTimeZoneList(sysTimeZone);
    }

    @Override
    public SysTimeZone getActive() {
        return sysTimeZoneMapper.getActive();
    }

    /**
     * 新增时区管理
     * 
     * @param sysTimeZone 时区管理
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysTimeZone(SysTimeZone sysTimeZone)
    {
        try {
            ZoneId.of(sysTimeZone.getTzName());  // 如果时区无效，抛出异常
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的时区ID: " + sysTimeZone.getTzName());
        }
        // 2. 检查时区是否已存在
        SysTimeZone existing = sysTimeZoneMapper.selectByTzName(sysTimeZone.getTzName());
        if (existing != null) {
            throw new IllegalArgumentException("时区已经存在: " + sysTimeZone.getTzName());
        }
        if (sysTimeZoneMapper.countAll() == 0) {
            sysTimeZone.setStatus(0);
        }else {
            sysTimeZone.setStatus(1);  // 如果已有记录，状态默认为 1
        }
        sysTimeZone.setCreateTime(DateUtils.getNowDate());
        return sysTimeZoneMapper.insertSysTimeZone(sysTimeZone);
    }

    /**
     * 修改时区管理
     * 
     * @param sysTimeZone 时区管理
     * @return 结果
     */
    @Override
    public int updateSysTimeZone(SysTimeZone sysTimeZone)
    {
        return sysTimeZoneMapper.updateSysTimeZone(sysTimeZone);
    }

    @Override
    public int setActiveById(Long id) {
        return sysTimeZoneMapper.setActiveById(id);
    }

    /**
     * 批量删除时区管理
     * 
     * @param ids 需要删除的时区管理主键
     * @return 结果
     */
    @Override
    public int deleteSysTimeZoneByIds(String[] ids)
    {
        return sysTimeZoneMapper.deleteSysTimeZoneByIds(ids);
    }

    /**
     * 删除时区管理信息
     * 
     * @param id 时区管理主键
     * @return 结果
     */
    @Override
    public int deleteSysTimeZoneById(String id)
    {
        return sysTimeZoneMapper.deleteSysTimeZoneById(id);
    }



}
