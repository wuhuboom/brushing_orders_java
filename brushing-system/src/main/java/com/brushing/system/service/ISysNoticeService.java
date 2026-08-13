package com.brushing.system.service;

import java.util.List;
import com.brushing.system.domain.SysNotice;

/**
 * 公告 服务层
 * 
 * @author brushing
 */
public interface ISysNoticeService
{
    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public SysNotice selectNoticeById(Long noticeId);

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    public List<SysNotice> selectNoticeList(SysNotice notice);

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    public int insertNotice(SysNotice notice);

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    public int updateNotice(SysNotice notice);

    /**
     * 删除公告信息
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    public int deleteNoticeById(Long noticeId);
    
    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    public int deleteNoticeByIds(Long[] noticeIds);

    /**
     * 查询用户在指定公告中的已读公告ID。
     */
    public List<Long> selectReadNoticeIds(Long userId, List<Long> noticeIds);

    /**
     * 查询用户未读的有效公告数量。
     */
    public int selectUnreadNoticeCount(Long userId);

    /**
     * 将指定公告标记为已读，操作是幂等的。
     */
    public int markNoticeRead(Long userId, Long noticeId);
}
