package com.order.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.SupportedLocale;
import com.order.common.utils.html.RichTextImagePaths;
import org.springframework.transaction.annotation.Transactional;
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

    private static final ObjectMapper JSON = new ObjectMapper();

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
        synchronizeCopies(translations, null);
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
    @Transactional
    public int updateTranslations(Translations translations)
    {
        Translations existing = translations.getId() == null ? null
                : translationsMapper.selectTranslationsByIdForUpdate(translations.getId());
        synchronizeCopies(translations, existing);
        return translationsMapper.updateTranslations(translations);
    }

    /** Rebuild from canonical language columns, never from a client-provided derived map. */
    private void synchronizeCopies(Translations incoming, Translations existing) {
        Map<String, String> relative = new LinkedHashMap<>();
        for (SupportedLocale locale : SupportedLocale.values()) {
            String value = locale.read(incoming);
            if (value != null) {
                value = RichTextStorageSupport.absolute(value);
                setLanguage(incoming, locale, value);
            } else {
                value = locale.read(existing);
            }
            if (value != null) {
                relative.put(locale.getCode(), RichTextImagePaths.toRelative(value));
            }
        }
        try {
            incoming.setRelativeContent(JSON.writeValueAsString(relative));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize relative translations", ex);
        }
    }

    private static void setLanguage(Translations target, SupportedLocale locale, String value) {
        switch (locale) {
            case ZH_CN -> target.setZhCn(value);
            case ZH_TW -> target.setZhTw(value);
            case KO_KR -> target.setKoKr(value);
            case TH_TH -> target.setThTh(value);
            case JA_JP -> target.setJaJp(value);
            case PT_PT -> target.setPtPt(value);
            case EN_US -> target.setEnUs(value);
            case AR_SA -> target.setArSa(value);
            case ES_ES -> target.setEsEs(value);
            case SV_SE -> target.setSvSe(value);
            case IT_IT -> target.setItIt(value);
            case DE_DE -> target.setDeDe(value);
            case NO_NO -> target.setNoNo(value);
            case RU_RU -> target.setRuRu(value);
            case HU_HU -> target.setHuHu(value);
            case PL_PL -> target.setPlPl(value);
            case SK_SK -> target.setSkSk(value);
            case FR_FR -> target.setFrFr(value);
            case CS_CZ -> target.setCsCz(value);
            case PT_BR -> target.setPtBr(value);
            case HI_IN -> target.setHiIn(value);
        }
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
