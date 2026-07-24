package com.order.system.service.impl;

import java.util.List;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.system.domain.SysNotice;
import com.order.system.mapper.SysNoticeMapper;
import com.order.system.service.ISysNoticeService;

/**
 * 公告 服务层实现
 * 
 * @author order
 */
@Service
@Transactional
public class SysNoticeServiceImpl implements ISysNoticeService
{
    @Autowired
    private SysNoticeMapper noticeMapper;

    @Autowired
    private ITranslationsService translationsService;

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId)
    {
        SysNotice notice = noticeMapper.selectNoticeById(noticeId);
        if (notice != null && notice.getTranslationsId() != null) {
            notice.setTranslations(translationsService.selectTranslationsById(notice.getTranslationsId()));
        }
        return notice;
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
        saveTranslations(notice);
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
        saveTranslations(notice);
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

    private void saveTranslations(SysNotice notice) {
        Translations translations = notice.getTranslations();
        if (translations == null || (translations.getId() == null && !translations.hasAnyValue())) {
            return;
        }
        if (translations.getId() == null) {
            translationsService.insertTranslations(translations);
        } else {
            translationsService.updateTranslations(translations);
        }
        notice.setTranslationsId(translations.getId());
    }
}
