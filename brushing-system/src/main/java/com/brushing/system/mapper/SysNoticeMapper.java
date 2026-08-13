package com.brushing.system.mapper;

import java.util.List;
import com.brushing.system.domain.SysNotice;
import org.apache.ibatis.annotations.Param;

/**
 * 通知公告表 数据层
 * 
 * @author brushing
 */
public interface SysNoticeMapper
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
     * 批量删除公告
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
     *
     * @param userId 会员用户ID
     * @param noticeIds 公告ID列表
     * @return 已读公告ID列表
     */
    public List<Long> selectReadNoticeIds(@Param("userId") Long userId,
                                          @Param("noticeIds") List<Long> noticeIds);

    /**
     * 查询用户未读的有效公告数量。
     *
     * @param userId 会员用户ID
     * @return 未读数量
     */
    public int selectUnreadNoticeCount(Long userId);

    /**
     * 记录用户已读公告，重复调用不会新增重复记录。
     *
     * @param userId 会员用户ID
     * @param noticeId 公告ID
     * @return 影响行数
     */
    public int insertNoticeRead(@Param("userId") Long userId,
                                @Param("noticeId") Long noticeId);
}
