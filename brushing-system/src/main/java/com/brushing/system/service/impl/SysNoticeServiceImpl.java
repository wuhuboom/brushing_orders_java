package com.brushing.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.system.domain.SysNotice;
import com.brushing.system.mapper.SysNoticeMapper;
import com.brushing.system.service.ISysNoticeService;

/**
 * 公告 服务层实现
 * 
 * @author brushing
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    @Autowired
    private SysNoticeMapper noticeMapper;

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId)
    {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice)
    {
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId)
    {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }

    @Override
    public List<Long> selectReadNoticeIds(Long userId, List<Long> noticeIds)
    {
        if (noticeIds == null || noticeIds.isEmpty())
        {
            return java.util.Collections.emptyList();
        }
        return noticeMapper.selectReadNoticeIds(userId, noticeIds);
    }

    @Override
    public int selectUnreadNoticeCount(Long userId)
    {
        return noticeMapper.selectUnreadNoticeCount(userId);
    }

    @Override
    public int markNoticeRead(Long userId, Long noticeId)
    {
        return noticeMapper.insertNoticeRead(userId, noticeId);
    }
}
