package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.service.IGoodsMemberLevelService;

/**
 * 等级Service业务层处理
 * 
 * @author order
 * @date 2025-10-11
 */
@Service
@Transactional
public class GoodsMemberLevelServiceImpl implements IGoodsMemberLevelService
{
    @Autowired
    private GoodsMemberLevelMapper goodsMemberLevelMapper;

    @Autowired
    private ITranslationsService translationsService;

    /**
     * 查询等级
     * 
     * @param id 等级主键
     * @return 等级
     */
    @Override
    public GoodsMemberLevel selectGoodsMemberLevelById(Long id)
    {
        GoodsMemberLevel level = goodsMemberLevelMapper.selectGoodsMemberLevelById(id);
        if (level != null && level.getTranslationsId() != null) {
            level.setTranslations(translationsService.selectTranslationsById(level.getTranslationsId()));
        }
        return level;
    }

    /**
     * 查询等级列表
     * 
     * @param goodsMemberLevel 等级
     * @return 等级
     */
    @Override
    public List<GoodsMemberLevel> selectGoodsMemberLevelList(GoodsMemberLevel goodsMemberLevel)
    {
        return goodsMemberLevelMapper.selectGoodsMemberLevelList(goodsMemberLevel);
    }

    /**
     * 新增等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    @Override
    public int insertGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel)
    {
        // handle translations first if provided
        Translations translations = goodsMemberLevel.getTranslations();
        if (translations != null && translations.hasAnyValue()) {
            translationsService.insertTranslations(translations);
            goodsMemberLevel.setTranslationsId(translations.getId());
        }

        goodsMemberLevel.setCreateTime(DateUtils.getNowDate());
        return goodsMemberLevelMapper.insertGoodsMemberLevel(goodsMemberLevel);
    }

    /**
     * 修改等级
     * 
     * @param goodsMemberLevel 等级
     * @return 结果
     */
    @Override
    public int updateGoodsMemberLevel(GoodsMemberLevel goodsMemberLevel)
    {
        // handle translations: update existing or insert new
        Translations translations = goodsMemberLevel.getTranslations();
        if (translations != null && (translations.getId() != null || translations.hasAnyValue())) {
            if (translations.getId() != null) {
                translationsService.updateTranslations(translations);
                goodsMemberLevel.setTranslationsId(translations.getId());
            } else {
                translationsService.insertTranslations(translations);
                goodsMemberLevel.setTranslationsId(translations.getId());
            }
        }

        return goodsMemberLevelMapper.updateGoodsMemberLevel(goodsMemberLevel);
    }

    /**
     * 批量删除等级
     * 
     * @param ids 需要删除的等级主键
     * @return 结果
     */
    @Override
    public int deleteGoodsMemberLevelByIds(Long[] ids)
    {
        return goodsMemberLevelMapper.deleteGoodsMemberLevelByIds(ids);
    }

    /**
     * 删除等级信息
     * 
     * @param id 等级主键
     * @return 结果
     */
    @Override
    public int deleteGoodsMemberLevelById(Long id)
    {
        return goodsMemberLevelMapper.deleteGoodsMemberLevelById(id);
    }
}
