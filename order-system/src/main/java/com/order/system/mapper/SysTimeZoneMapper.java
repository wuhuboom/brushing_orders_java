package com.order.system.mapper;



import com.order.system.domain.SysTimeZone;

import java.util.List;

/**
 * 时区管理Mapper接口
 * 
 * @author betting
 * @date 2025-08-26
 */
public interface SysTimeZoneMapper 
{
    /**
     * 查询时区管理
     * 
     * @param id 时区管理主键
     * @return 时区管理
     */
    public SysTimeZone selectSysTimeZoneById(Long id);


    public SysTimeZone selectByTzName(String tzName);

    //查询正在使用的时区
    public SysTimeZone getActive();

    /**
     * 查询时区管理列表
     * 
     * @param sysTimeZone 时区管理
     * @return 时区管理集合
     */
    public List<SysTimeZone> selectSysTimeZoneList(SysTimeZone sysTimeZone);


    public int countAll();

    /**
     * 新增时区管理
     * 
     * @param sysTimeZone 时区管理
     * @return 结果
     */
    public int insertSysTimeZone(SysTimeZone sysTimeZone);

    /**
     * 修改时区管理
     * 
     * @param sysTimeZone 时区管理
     * @return 结果
     */
    public int updateSysTimeZone(SysTimeZone sysTimeZone);
    public int setActiveById(Long id);

    /**
     * 删除时区管理
     * 
     * @param id 时区管理主键
     * @return 结果
     */
    public int deleteSysTimeZoneById(String id);

    /**
     * 批量删除时区管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysTimeZoneByIds(String[] ids);
}
