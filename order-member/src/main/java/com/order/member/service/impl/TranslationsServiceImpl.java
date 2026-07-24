package com.order.member.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import com.order.member.mapper.TranslationsMapper;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;

/**
 * 多语言翻译Service业务层处理
 * 
 * @author order
 * @date 2025-12-16
 */
@Service
public class TranslationsServiceImpl implements ITranslationsService 
{
    @Autowired
    private TranslationsMapper translationsMapper;

    /**
     * 查询多语言翻译
     * 
     * @param id 多语言翻译主键
     * @return 多语言翻译
     */
    @Override
    public Translations selectTranslationsById(Long id)
    {
        return translationsMapper.selectTranslationsById(id);
    }

    @Override
    public Map<Long, Translations> selectTranslationsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Translations> result = new LinkedHashMap<>();
        for (Translations translations : translationsMapper.selectTranslationsByIds(distinctIds)) {
            result.put(translations.getId(), translations);
        }
        return result;
    }

    /**
     * 查询多语言翻译列表
     * 
     * @param translations 多语言翻译
     * @return 多语言翻译
     */
    @Override
    public List<Translations> selectTranslationsList(Translations translations)
    {
        return translationsMapper.selectTranslationsList(translations);
    }

    /**
     * 新增多语言翻译
     * 
     * @param translations 多语言翻译
     * @return 结果
     */
    @Override
    @CacheEvict(value = "apiMessageCatalog", allEntries = true)
    public int insertTranslations(Translations translations)
    {
        return translationsMapper.insertTranslations(translations);
    }

    /**
     * 修改多语言翻译
     * 
     * @param translations 多语言翻译
     * @return 结果
     */
    @Override
    @CacheEvict(value = "apiMessageCatalog", allEntries = true)
    public int updateTranslations(Translations translations)
    {
        return translationsMapper.updateTranslations(translations);
    }

    /**
     * 批量删除多语言翻译
     * 
     * @param ids 需要删除的多语言翻译主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = "apiMessageCatalog", allEntries = true)
    public int deleteTranslationsByIds(Long[] ids)
    {
        return translationsMapper.deleteTranslationsByIds(ids);
    }

    /**
     * 删除多语言翻译信息
     * 
     * @param id 多语言翻译主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = "apiMessageCatalog", allEntries = true)
    public int deleteTranslationsById(Long id)
    {
        return translationsMapper.deleteTranslationsById(id);
    }
}
